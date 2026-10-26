package net.patchedpixel.scenecraft.replay;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.patchedpixel.scenecraft.core.utils.Restrictions;
import net.patchedpixel.scenecraft.platform.Utils;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import net.patchedpixel.scenecraft.replay.camera.SpectatorCameraController;
import net.patchedpixel.scenecraft.replay.events.ReplayClosedCallback;
import net.patchedpixel.scenecraft.replay.events.ReplayClosingCallback;
import net.patchedpixel.scenecraft.replay.events.ReplayOpenedCallback;
import net.patchedpixel.scenecraft.replay.gui.overlay.GuiReplayOverlay;
import net.patchedpixel.scenecraft.scenestudio.data.Marker;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.scenestudio.util.Location;
import net.patchedpixel.scenecraft.gui.container.AbstractGuiScreen;
import net.patchedpixel.scenecraft.gui.container.GuiContainer;
import net.patchedpixel.scenecraft.gui.container.GuiScreen;
import net.patchedpixel.scenecraft.gui.element.GuiLabel;
import net.patchedpixel.scenecraft.gui.element.advanced.GuiProgressBar;
import net.patchedpixel.scenecraft.gui.layout.HorizontalLayout;
import net.patchedpixel.scenecraft.gui.popup.AbstractGuiPopup;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraftforge.network.NetworkConstants;
import net.patchedpixel.scenecraft.mixin.replay.NetworkConstantsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketBundlePacker;
import net.minecraft.network.PacketDecoder;

import java.io.IOException;
import java.util.*;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.protocol.PacketFlow;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static net.patchedpixel.scenecraft.platform.Utils.DEFAULT_MS_PER_TICK;
import static net.patchedpixel.scenecraft.core.versions.MCVer.*;
import static net.patchedpixel.scenecraft.replay.SceneCraftReplay.LOGGER;

public class ReplayHandler {
    public static final String PACKET_HANDLER_NAME = "SceneCraftReplay_packetHandler";

    private static Minecraft mc = getMinecraft();

    /**
     * The file currently being played.
     */
    private final ReplayFile replayFile;

    /**
     * Decodes and sends packets into channel.
     */
    private final FullReplaySender fullReplaySender;
    private final QuickReplaySender quickReplaySender;
    private boolean quickMode = false;

    /**
     * Currently active replay restrictions.
     */
    private Restrictions restrictions = new Restrictions();

    private Set<Marker> markers;

    private final GuiReplayOverlay overlay;

    private EmbeddedChannel channel;

    private int replayDuration;

    /**
     * The position at which the camera should be located after the next jump.
     */
    private Location targetCameraPosition;

    private UUID spectating;

    public ReplayHandler(ReplayFile replayFile, boolean asyncMode) throws IOException {
        Preconditions.checkState(mc.isSameThread(), "Must be called from Minecraft thread.");
        this.replayFile = replayFile;

        replayDuration = replayFile.getMetaData().getDuration();

        markers = replayFile.getMarkers().orElse(Collections.emptySet());

        fullReplaySender = new FullReplaySender(this, replayFile);
        quickReplaySender = new QuickReplaySender(SceneCraftReplay.instance, replayFile);

        setup();

        overlay = new GuiReplayOverlay(this);
        overlay.setVisible(true);

        ReplayOpenedCallback.EVENT.invoker().replayOpened(this);

        fullReplaySender.setAsyncMode(asyncMode);
    }

    void restartedReplay() {
        Preconditions.checkState(mc.isSameThread(), "Must be called from Minecraft thread.");

        channel.close();

        mc.mouseHandler.releaseMouse();

        // Force re-creation of camera entity by unloading the previous world
        mc.clearLevel();

        restrictions = new Restrictions();

        setup();
    }

    public void endReplay() throws IOException {
        Preconditions.checkState(mc.isSameThread(), "Must be called from Minecraft thread.");

        ReplayClosingCallback.EVENT.invoker().replayClosing(this);

        fullReplaySender.terminateReplay();
        if (quickMode) {
            quickReplaySender.unregister();
        }

        replayFile.save();
        replayFile.close();

        channel.close().awaitUninterruptibly();

        if (mc.player instanceof CameraEntity) {
        }

        if (mc.level != null) {
            mc.clearLevel();
        }

        mc.timer.msPerTick = DEFAULT_MS_PER_TICK;
        overlay.setVisible(false);

        SceneCraftReplay.instance.forcefullyStopReplay();

        mc.setScreen(null);

        ReplayClosedCallback.EVENT.invoker().replayClosed(this);
    }

    private void setup() {
        Preconditions.checkState(mc.isSameThread(), "Must be called from Minecraft thread.");

        mc.gui.getChat().clearMessages(false);

        Connection networkManager = new Connection(PacketFlow.CLIENTBOUND) {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
                t.printStackTrace();
            }
        };

        channel = new EmbeddedChannel();
        channel.pipeline().addFirst("SceneCraftReplay_head", new DropOutboundMessagesHandler());

        quickReplaySender.setChannel(channel);
        fullReplaySender.setChannel(channel);

        channel.pipeline().addLast("decoder", new PacketDecoder(PacketFlow.CLIENTBOUND));
        channel.pipeline().addLast("bundler", new PacketBundlePacker(PacketFlow.CLIENTBOUND));
        channel.pipeline().addLast(PACKET_HANDLER_NAME, quickMode ? quickReplaySender : fullReplaySender);
        channel.pipeline().addLast("packet_handler", networkManager);
        channel.pipeline().fireChannelActive();
        channel.attr(NetworkConstantsAccessor.sceneCraft$getFmlNetVersion()).set(NetworkConstants.NOVERSION);

        networkManager.setProtocol(ConnectionProtocol.LOGIN);
        networkManager.setListener(new ClientHandshakePacketListenerImpl(
                networkManager, mc, null, null, false, null, it -> {}
        ));

        mc.pendingConnection = networkManager;
    }

    public ReplayFile getReplayFile() {
        return replayFile;
    }

    public Restrictions getRestrictions() {
        return restrictions;
    }

    public ReplaySender getReplaySender() {
        return quickMode ? quickReplaySender : fullReplaySender;
    }

    public GuiReplayOverlay getOverlay() {
        return overlay;
    }

    public void ensureQuickModeInitialized(Runnable andThen) {
        ListenableFuture<Void> future = quickReplaySender.getInitializationPromise();
        if (future == null) {
            InitializingQuickModePopup popup = new InitializingQuickModePopup(overlay);
            future = quickReplaySender.initialize(progress -> popup.progressBar.setProgress(progress.floatValue()));
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    popup.close();
                }

                @Override
                public void onFailure(@Nonnull Throwable t) {
                    String message = "Failed to initialize quick mode. It will not be available.";
                    Utils.error(LOGGER, overlay, CrashReport.forThrowable(t, message), popup::close);
                }
            }, Runnable::run);
        }
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                andThen.run();
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                // Exception already printed in callback added above
            }
        }, Runnable::run);
    }

    private class InitializingQuickModePopup extends AbstractGuiPopup<InitializingQuickModePopup> {
        private final GuiProgressBar progressBar = new GuiProgressBar(popup).setSize(300, 20)
                .setI18nLabel("scenecraft.gui.loadquickmode");

        public InitializingQuickModePopup(GuiContainer container) {
            super(container);
            open();
        }

        @Override
        public void close() {
            super.close();
        }

        @Override
        protected InitializingQuickModePopup getThis() {
            return this;
        }
    }

    public void setQuickMode(boolean quickMode) {
        if (quickMode == this.quickMode) return;
        if (quickMode && fullReplaySender.isAsyncMode()) {
            // If this method is called via runLater, then it cannot switch to sync mode by itself as there might be
            // some rogue packets in the task queue after it. Instead the caller must switch to sync mode first and
            // use runLater until all packets have been processed (when using setAsyncModeAndWait, one runLater should
            // be sufficient).
            throw new IllegalStateException("Cannot switch to quick mode while in async mode.");
        }
        this.quickMode = quickMode;

        CameraEntity cam = getCameraEntity();
        if (cam != null) {
            targetCameraPosition = new Location(cam.getX(), cam.getY(), cam.getZ(), cam.getYRot(), cam.getXRot());
        } else {
            targetCameraPosition = null;
        }

        channel.pipeline().replace(PACKET_HANDLER_NAME, PACKET_HANDLER_NAME, quickMode ? quickReplaySender : fullReplaySender);

        if (quickMode) {
            quickReplaySender.register();
            quickReplaySender.restart();
            quickReplaySender.sendPacketsTill(fullReplaySender.currentTimeStamp());
        } else {
            quickReplaySender.unregister();
            fullReplaySender.sendPacketsTill(0);
            fullReplaySender.sendPacketsTill(quickReplaySender.currentTimeStamp());
        }

        moveCameraToTargetPosition();
    }

    public boolean isQuickMode() {
        return quickMode;
    }

    public int getReplayDuration() {
        return replayDuration;
    }

    /**
     * Spectate the specified entity.
     * When the entity is {@code null} or the camera entity, the camera becomes the view entity.
     * @param e The entity to spectate
     */
    public void spectateEntity(Entity e) {
        CameraEntity cameraEntity = getCameraEntity();
        if (cameraEntity == null) {
            return; // Cannot spectate if we have no camera
        }
        if (e == null || e == cameraEntity) {
            spectating = null;
            e = cameraEntity;
        } else if (e instanceof Player) {
            spectating = e.getUUID();
        }

        if (e == cameraEntity) {
            cameraEntity.setCameraController(SceneCraftReplay.instance.createCameraController(cameraEntity));
        } else {
            cameraEntity.setCameraController(new SpectatorCameraController(cameraEntity));
        }

        if (mc.getCameraEntity() != e) {
            mc.setCameraEntity(e);
            cameraEntity.setCameraPosRot(e);
        }
    }

    /**
     * Set the camera as the view entity.
     * This is equivalent to {@code spectateEntity(null)}.
     */
    public void spectateCamera() {
        spectateEntity(null);
    }

    /**
     * Returns whether the current view entity is the camera entity.
     * @return {@code true} if the camera is the view entity, {@code false} otherwise
     */
    public boolean isCameraView() {
        return mc.player instanceof CameraEntity && mc.player == mc.getCameraEntity();
    }

    /**
     * Returns the camera entity.
     * @return The camera entity or {@code null} if it does not yet exist
     */
    public CameraEntity getCameraEntity() {
        return mc.player instanceof CameraEntity ? (CameraEntity) mc.player : null;
    }

    public UUID getSpectatedUUID() {
        return spectating;
    }

    public void moveCameraToTargetPosition() {
        CameraEntity cam = getCameraEntity();
        if (cam != null && targetCameraPosition != null) {
            cam.setCameraPosRot(targetCameraPosition);
        }
    }

    public void doJump(int targetTime, boolean retainCameraPosition) {
        if (!getReplaySender().isAsyncMode()) {
            return; // path playback, rendering, etc. -> no jumping allowed
        }

        if (getReplaySender() == quickReplaySender) {
            // Always round to full tick
            targetTime = targetTime + targetTime % 50;

            if (targetTime >= 50) {
                // Jump to time of previous tick first
                quickReplaySender.sendPacketsTill(targetTime - 50);
            }

            // Update all entity positions (especially prev/lastTick values)
            for (Entity entity : mc.level.entitiesForRendering()) {
                skipTeleportInterpolation(entity);
                entity.xOld = entity.xo = entity.getX();
                entity.yOld = entity.yo = entity.getY();
                entity.zOld = entity.zo = entity.getZ();
                entity.yRotO = entity.getYRot();
                entity.xRotO = entity.getXRot();
            }

            // Run previous tick
            mc.tick();

            // Jump to target tick
            quickReplaySender.sendPacketsTill(targetTime);

            // Immediately apply player teleport interpolation
            for (Entity entity : mc.level.entitiesForRendering()) {
                skipTeleportInterpolation(entity);
            }
            return;
        }
        FullReplaySender replaySender = fullReplaySender;

        if (replaySender.isHurrying()) {
            return; // When hurrying, no Timeline jumping etc. is possible
        }

        if (targetTime < replaySender.currentTimeStamp()) {
            mc.setScreen(null);
        }

        if (retainCameraPosition) {
            CameraEntity cam = getCameraEntity();
            if (cam != null) {
                targetCameraPosition = new Location(cam.getX(), cam.getY(), cam.getZ(),
                        cam.getYRot(), cam.getXRot());
            } else {
                targetCameraPosition = null;
            }
        }

        long diff = targetTime - (replaySender.isHurrying() ? replaySender.getDesiredTimestamp() : replaySender.currentTimeStamp());
        if (diff != 0) {
            if (diff > 0 && diff < 5000) { // Small difference and no time travel
                if (replaySender.paused()) {
                    replaySender.setSyncModeAndWait();
                    do {
                        replaySender.sendPacketsTill(targetTime);
                        targetTime += 500;
                    } while (mc.player == null || mc.screen instanceof LevelLoadingScreen);
                    replaySender.setAsyncMode(true);

                    for (int i = 0; i < Math.min(diff / 50, 3); i++) {
                        mc.tick();
                    }
                } else {
                    replaySender.jumpToTime(targetTime);
                }
            } else { // We either have to restart the replay or send a significant amount of packets
                // Render our please-wait-screen
                GuiScreen guiScreen = new GuiScreen();
                guiScreen.setBackground(AbstractGuiScreen.Background.DIRT);
                guiScreen.setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER));
                guiScreen.addElements(new HorizontalLayout.Data(0.5),
                        new GuiLabel().setI18nText("scenecraft.gui.pleasewait"));

                // Make sure that the replaysender changes into sync mode
                replaySender.setSyncModeAndWait();

                net.minecraft.client.gui.screens.Screen orgScreen = mc.screen;
                mc.setScreen(guiScreen.toMinecraft());
                mc.gameRenderer.render(mc.getFrameTime(), Util.getNanos(), true);
                mc.getWindow().updateDisplay();
                mc.setScreen(orgScreen);

                // Send the packets
                do {
                    replaySender.sendPacketsTill(targetTime);
                    targetTime += 500;
                } while (mc.player == null || mc.screen instanceof LevelLoadingScreen);
                replaySender.setAsyncMode(true);
                replaySender.setReplaySpeed(0);

                mc.getConnection().getConnection()
                        .tick();

                // If the packets we just sent somehow caused the client to disconnect, then the above connection tick
                // call will have unloaded the world, and we'll have to abort what we were doing.
                if (mc.level == null) {
                    return;
                }

                for (Entity entity : mc.level.entitiesForRendering()) {
                    skipTeleportInterpolation(entity);
                    entity.xOld = entity.xo = entity.getX();
                    entity.yOld = entity.yo = entity.getY();
                    entity.zOld = entity.zo = entity.getZ();
                    entity.yRotO = entity.getYRot();
                    entity.xRotO = entity.getXRot();
                }
                mc.tick();

                //finally, updating the camera's position (which is not done by the sync jumping)
                moveCameraToTargetPosition();

                // No need to remove our please-wait-screen. It'll vanish with the next
                // render pass as it's never been a real GuiScreen in the first place.
            }
        }
    }

    private void skipTeleportInterpolation(Entity entity) {
        if (entity instanceof LivingEntity && !(entity instanceof CameraEntity)) {
            LivingEntity living = (LivingEntity) entity;
            if (living.lerpSteps > 0) {
                living.setPos(living.lerpX, living.lerpY, living.lerpZ);
                living.setYRot((float) living.lerpYRot);
                living.setXRot((float) living.lerpXRot);
                living.lerpSteps = 0;
            }
        }
    }

    private static class DropOutboundMessagesHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            // The embedded channel's event loop will consider every thread to be in it and as such provides no
            // guarantees that only one thread is using the pipeline at any one time.
            // For reading the replay sender (either sync or async) is the only thread ever writing.
            // For writing it may very well happen that multiple threads want to use the pipline at the same time.
            // It's unclear whether the EmbeddedChannel is supposed to be thread-safe (the behavior of the event loop
            // does suggest that). However it seems like it either isn't (likely) or there is a race condition.
            // See: https://www.replaymod.com/forum/thread/1752#post8045 (https://paste.replaymod.com/lotacatuwo)
            // To work around this issue, we just outright drop all write/flush requests (they aren't needed anyway).
            // This still leaves channel handlers upstream with the threading issue but they all seem to cope well with it.
            promise.setSuccess();
        }

        @Override
        public void flush(ChannelHandlerContext ctx) {
            // See write method above
        }
    }
}
