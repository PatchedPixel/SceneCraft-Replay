package net.patchedpixel.scenecraft.render.rendering;

import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.pathing.player.AbstractTimelinePlayer;
import net.patchedpixel.scenecraft.pathing.player.ReplayTimer;
import net.patchedpixel.scenecraft.pathing.properties.TimestampProperty;
import net.patchedpixel.scenecraft.render.CameraPathExporter;
import net.patchedpixel.scenecraft.render.EXRWriter;
import net.patchedpixel.scenecraft.render.PNGWriter;
import net.patchedpixel.scenecraft.render.RenderSettings;
import net.patchedpixel.scenecraft.render.SceneCraftRender;
import net.patchedpixel.scenecraft.render.FFmpegWriter;
import net.patchedpixel.scenecraft.render.capturer.RenderInfo;
import net.patchedpixel.scenecraft.render.events.ReplayRenderCallback;
import net.patchedpixel.scenecraft.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.render.gui.GuiRenderingDone;
import net.patchedpixel.scenecraft.render.gui.GuiVideoRenderer;
import net.patchedpixel.scenecraft.render.gui.progress.VirtualWindow;
import net.patchedpixel.scenecraft.render.hooks.ForceChunkLoadingHook;
import net.patchedpixel.scenecraft.render.metadata.MetadataInjector;
import net.patchedpixel.scenecraft.render.utils.FlawlessFrames;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Keyframe;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Path;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Timeline;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.ReportedException;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.thread.BlockableEventLoop;
import net.neoforged.fml.ModList;

import java.util.concurrent.CompletableFuture;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;
import static net.patchedpixel.scenecraft.platform.Utils.DEFAULT_MS_PER_TICK;
import static net.patchedpixel.scenecraft.core.versions.MCVer.*;
import static net.patchedpixel.scenecraft.render.SceneCraftRender.LOGGER;

public class VideoRenderer implements RenderInfo {
    private static final Identifier SOUND_RENDER_SUCCESS = identifier(SceneCraft.MOD_ID, "render_success");
    private final Minecraft mc = MCVer.getMinecraft();
    private final RenderSettings settings;
    private final ReplayHandler replayHandler;
    private final Timeline timeline;
    private final Pipeline renderingPipeline;
    private final FFmpegWriter ffmpegWriter;
    private final CameraPathExporter cameraPathExporter;

    private int fps;
    private boolean mouseWasGrabbed;
    private boolean debugInfoWasShown;
    private Map<SoundSource, Float> originalSoundLevels;

    private TimelinePlayer timelinePlayer;
    private Future<Void> timelinePlayerFuture;
    private ForceChunkLoadingHook forceChunkLoadingHook;

    private int framesDone;
    private int totalFrames;

    private final VirtualWindow guiWindow = new VirtualWindow(mc);
    private final GuiVideoRenderer gui;
    private boolean paused;
    private boolean cancelled;
    private volatile Throwable failureCause;

    public VideoRenderer(RenderSettings settings, ReplayHandler replayHandler, Timeline timeline) throws IOException {
        this.settings = settings;
        this.replayHandler = replayHandler;
        this.timeline = timeline;
        this.gui = new GuiVideoRenderer(this);
        FrameConsumer<BitmapFrame> frameConsumer;
        if (settings.getEncodingPreset() == RenderSettings.EncodingPreset.EXR) {
            frameConsumer = EXRWriter.create(settings.getOutputFile().toPath(), settings.isIncludeAlphaChannel());
        } else if (settings.getEncodingPreset() == RenderSettings.EncodingPreset.PNG) {
            frameConsumer = new PNGWriter(settings.getOutputFile().toPath(), settings.isIncludeAlphaChannel());
        } else {
            frameConsumer = new FFmpegWriter(this);
        }
        ffmpegWriter = frameConsumer instanceof FFmpegWriter ? (FFmpegWriter) frameConsumer : null;
        FrameConsumer<BitmapFrame> previewingFrameConsumer = new FrameConsumer<BitmapFrame>() {
            private int lastFrameId = -1;

            @Override
            public void consume(Map<Channel, BitmapFrame> channels) {
                BitmapFrame bgra = channels.get(Channel.BRGA);
                if (bgra != null) {
                    synchronized (this) {
                        int frameId = bgra.getFrameId();
                        if (lastFrameId < frameId) {
                            lastFrameId = frameId;
                            gui.updatePreview(bgra.getByteBuffer(), bgra.getSize());
                        }
                    }
                }
                frameConsumer.consume(channels);
            }

            @Override
            public void close() throws IOException {
                frameConsumer.close();
            }

            @Override
            public boolean isParallelCapable() {
                return frameConsumer.isParallelCapable();
            }
        };
        this.renderingPipeline = Pipelines.newPipeline(settings.getRenderMethod(), this, previewingFrameConsumer);

        if (settings.isCameraPathExport()) {
            this.cameraPathExporter = new CameraPathExporter(settings);
        } else {
            this.cameraPathExporter = null;
        }
    }

    /**
     * Render this video.
     * @return {@code true} if rendering was successful, {@code false} if the user aborted rendering (or the window was closed)
     */
    public boolean renderVideo() throws Throwable {
        ReplayRenderCallback.Pre.EVENT.invoker().beforeRendering(this);

        setup();

        // Because this might take some time to prepare we'll render the GUI at least once to not confuse the user
        drawGui();

        ReplayTimer timer = (ReplayTimer) mc.deltaTracker;

        // Play up to one second before starting to render
        // This is necessary in order to ensure that all entities have at least two position packets
        // and their first position in the record is correct.
        // Note that it is impossible to also get the interpolation between their latest position
        // and the one in the record correct as there's no reliable way to tell when the server ticks
        // or when we should be done with the interpolation of the entity
        Optional<Integer> optionalVideoStartTime = timeline.getValue(TimestampProperty.PROPERTY, 0);
        if (optionalVideoStartTime.isPresent()) {
            int videoStart = optionalVideoStartTime.get();

            if (videoStart > 1000) {
                int replayTime = videoStart - 1000;
                timer.tickDelta = 0;
                timer.msPerTick = DEFAULT_MS_PER_TICK;
                while (replayTime < videoStart) {
                    replayTime += 50;
                    replayHandler.getReplaySender().sendPacketsTill(replayTime);
                    tick();
                }
            }
        }

        renderingPipeline.run();

        if (BlockableEventLoop.delayedCrash != null) {
            throw new ReportedException(BlockableEventLoop.delayedCrash.get());
        }

        if (settings.isInjectSphericalMetadata()) {
            MetadataInjector.injectMetadata(settings.getRenderMethod(), settings.getOutputFile(),
                    settings.getTargetVideoWidth(), settings.getTargetVideoHeight(),
                    settings.getSphericalFovX(), settings.getSphericalFovY());
        }

        finish();

        ReplayRenderCallback.Post.EVENT.invoker().afterRendering(this);

        if (failureCause != null) {
            throw failureCause;
        }

        return !cancelled;
    }

    @Override
    public float updateForNextFrame() {
        // because the jGui lib uses Minecraft's displayWidth and displayHeight values, update these temporarily
        guiWindow.bind();

        if (!settings.isHighPerformance() || framesDone % fps == 0) {
            while (drawGui() && paused) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Updating the timer will cause the timeline player to update the game state
        ReplayTimer timer = (ReplayTimer) mc.deltaTracker;
        int elapsedTicks =
        timer.advanceGameTime(
                MCVer.milliTime()
        );

        executeTaskQueue();

        while (elapsedTicks-- > 0) {
            tick();
        }

        if (mc.level != null) {
            mc.level.update();
        }

        // change Minecraft's display size back
        guiWindow.unbind();

        if (cameraPathExporter != null) {
            cameraPathExporter.recordFrame();
        }

        framesDone++;
        return timer.tickDelta;
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }

    private void setup() {
        timelinePlayer = new TimelinePlayer(replayHandler);
        timelinePlayerFuture = timelinePlayer.start(timeline);

        // FBOs are always used in 1.14+
        if (mc.debugEntries.isOverlayVisible()) {
            mc.debugEntries.setOverlayVisible(false);
            debugInfoWasShown = true;
        }
        if (mc.mouseHandler.isMouseGrabbed()) {
            mouseWasGrabbed = true;
        }
        mc.mouseHandler.releaseMouse();

        // Mute all sounds except GUI sounds (buttons, etc.)
        originalSoundLevels = new EnumMap<>(SoundSource.class);
        for (SoundSource category : SoundSource.values()) {
            if (category != SoundSource.MASTER) {
                originalSoundLevels.put(category, mc.options.getSoundSourceVolume(category));
                mc.options.getSoundSourceOptionInstance(category).set((double) 0);
            }
        }

        fps = settings.getFramesPerSecond();

        long duration = 0;
        for (Path path : timeline.getPaths()) {
            if (!path.isActive()) continue;

            // Prepare path interpolations
            path.updateAll();
            // Find end time
            Collection<Keyframe> keyframes = path.getKeyframes();
            if (!keyframes.isEmpty()) {
                duration = Math.max(duration, getLast(keyframes).getTime());
            }
        }

        totalFrames = (int) (duration*fps/1000);

        if (cameraPathExporter != null) {
            cameraPathExporter.setup(totalFrames);
        }

        gui.toMinecraft().init(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());

        forceChunkLoadingHook = new ForceChunkLoadingHook(mc.levelRenderer);
    }

    private void finish() {
        if (!timelinePlayerFuture.isDone()) {
            timelinePlayerFuture.cancel(false);
        }
        // Tear down of the timeline player might only happen the next tick after it was cancelled
        timelinePlayer.onTick();

        guiWindow.close();

        // FBOs are always used in 1.14+
        if (debugInfoWasShown) {
            mc.debugEntries.setOverlayVisible(true);
        }
        if (mouseWasGrabbed) {
            mc.mouseHandler.grabMouse();
        }
        for (Map.Entry<SoundSource, Float> entry : originalSoundLevels.entrySet()) {
            mc.options.getSoundSourceOptionInstance(entry.getKey()).set((double) entry.getValue());
        }
        mc.gui.setScreen(null);
        forceChunkLoadingHook.uninstall();

        if (!hasFailed() && cameraPathExporter != null) {
            try {
                cameraPathExporter.finish();
            } catch (IOException e) {
                setFailure(e);
            }
        }

        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(SOUND_RENDER_SUCCESS), 1));

        try {
            if (!hasFailed() && ffmpegWriter != null) {
                new GuiRenderingDone(SceneCraftRender.instance, ffmpegWriter.getVideoFile(), totalFrames, settings).display();
            }
        } catch (FFmpegWriter.FFmpegStartupException e) {
            setFailure(e);
        }

        // Finally, resize the Minecraft framebuffer to the actual width/height of the window
        resizeMainWindow(mc, guiWindow.getFramebufferWidth(), guiWindow.getFramebufferHeight());
    }

    private void executeTaskQueue() {
        while (true) {
            while (mc.gui.overlay() != null) {
                drawGui();
                ((MinecraftMethodAccessor) mc).sceneCraftExecuteTaskQueue();

                // The SplashOverlay now only closes on `tick`, but there are no ticks while we're waiting,
                // so we need to manually tick it to not get stuck.
                Overlay overlay = mc.gui.overlay();
                if (overlay != null) {
                    overlay.tick();
                }
            }

            CompletableFuture<Void> resourceReloadFuture = mc.pendingReload;
            if (resourceReloadFuture != null) {
                mc.pendingReload = null;
                mc.reloadResourcePacks().thenRun(() -> resourceReloadFuture.complete(null));
                continue;
            }
            break;
        }
        ((MinecraftMethodAccessor) mc).sceneCraftExecuteTaskQueue();

        mc.gui.screen = gui.toMinecraft();
    }

    private void tick() {
        mc.getTextureManager().tick();

        mc.tick();
    }

    public boolean drawGui() {
        Window window = mc.getWindow();
        do {
            if (window.shouldClose() || BlockableEventLoop.delayedCrash != null) {
                return false;
            }
            MCVer.pollEvents();

            pushMatrix();
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorAndDepthTextures(mc.gameRenderer.mainRenderTarget().getColorTexture(), new org.joml.Vector4f(), mc.gameRenderer.mainRenderTarget().getDepthTexture(), 0);
            guiWindow.beginWrite();

            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorAndDepthTextures(mc.gameRenderer.mainRenderTarget().getColorTexture(), new org.joml.Vector4f(), mc.gameRenderer.mainRenderTarget().getDepthTexture(), 0);

            gui.toMinecraft().init(window.getGuiScaledWidth(), window.getGuiScaledHeight());

            // Events are polled on 1.13+ in mainWindow.update which is called later

            int mouseX = (int) mc.mouseHandler.xpos() * window.getGuiScaledWidth() / Math.max(window.getScreenWidth(), 1);
            int mouseY = (int) mc.mouseHandler.ypos() * window.getGuiScaledHeight() / Math.max(window.getScreenHeight(), 1);

            GameRenderer gameRenderer = mc.gameRenderer;
            GuiRenderState guiRenderState = gameRenderer.gameRenderState().guiRenderState;
            guiRenderState.reset();
            GuiGraphicsExtractor drawContext = new GuiGraphicsExtractor(mc, guiRenderState, mouseX, mouseY);

            WindowRenderState windowRenderState = gameRenderer.gameRenderState().windowRenderState;
            windowRenderState.width = window.getWidth();
            windowRenderState.height = window.getHeight();
            windowRenderState.guiScale = window.getGuiScale();
            windowRenderState.appropriateLineWidth = window.getAppropriateLineWidth();
            windowRenderState.isMinimized = window.isIconified();

            if (mc.gui.overlay() != null) {
                Screen orgScreen = mc.gui.screen();
                try {
                    mc.gui.screen = gui.toMinecraft();
                    mc.gui.overlay().extractRenderState(
                            drawContext,
                            mouseX, mouseY, 0);
                } finally {
                    mc.gui.screen = orgScreen;
                }
            } else {
                gui.toMinecraft().tick();
                gui.toMinecraft().extractRenderStateWithTooltipAndSubtitles(
                        drawContext,
                        mouseX, mouseY, 0);
            }
            var orgFog = RenderSystem.getShaderFog();
            var orgProjBuf = RenderSystem.getProjectionMatrixBuffer();
            var orgProjType = RenderSystem.getProjectionType();
            gameRenderer.guiRenderer.render();
            RenderSystem.setShaderFog(orgFog);
            RenderSystem.setProjectionMatrix(orgProjBuf, orgProjType);

            guiWindow.endWrite();
            popMatrix();
            pushMatrix();
            guiWindow.flip();
            popMatrix();

            if (mc.mouseHandler.isMouseGrabbed()) {
                mc.mouseHandler.releaseMouse();
            }

            return !hasFailed() && !cancelled;
        } while (true);
    }

    public int getFramesDone() {
        return framesDone;
    }

    @Override
    public ReadableDimension getFrameSize() {
        return new Dimension(settings.getVideoWidth(), settings.getVideoHeight());
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public int getVideoTime() { return framesDone * 1000 / fps; }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void cancel() {
        if (ffmpegWriter != null) {
            ffmpegWriter.abort();
        }
        this.cancelled = true;
        renderingPipeline.cancel();
    }

    public boolean hasFailed() {
        return failureCause != null;
    }

    public synchronized void setFailure(Throwable cause) {
        if (this.failureCause != null) {
            LOGGER.error("Further failure during failed rendering: ", cause);
        } else {
            LOGGER.error("Failure during rendering: ", cause);
            this.failureCause = cause;
            cancel();
        }
    }

    private class TimelinePlayer extends AbstractTimelinePlayer {
        public TimelinePlayer(ReplayHandler replayHandler) {
            super(replayHandler);
        }

        @Override
        public long getTimePassed() {
            return getVideoTime();
        }
    }

    public static String[] checkCompat(Stream<RenderSettings> settings) {
        return settings.map(VideoRenderer::checkCompat).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public static String[] checkCompat(RenderSettings settings) {
        if (ModList.get().isLoaded("sodium") && !FlawlessFrames.hasSodium()) {
            return new String[] {
                    "Rendering is not supported with your Sodium version.",
                    "It is missing support for the FREX Flawless Frames API.",
                    "Either use the Sodium build from replaymod.com or uninstall Sodium before rendering!",
            };
        }
        if (settings.getRenderMethod() == RenderSettings.RenderMethod.ODS
                && !ModList.get().isLoaded("iris")) {
            return new String[] {
                    "ODS export requires Iris to be installed for Minecraft 1.17 and above.",
                    "Note that it is nevertheless incompatible with other shaders and will simply replace them.",
                    "Get it from: https://modrinth.com/mod/iris",
            };
        }
        return null;
    }
}
