package net.patchedpixel.scenecraft.common.simplepathing.gui;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.pathing.gui.GuiKeyframeRepository;
import net.patchedpixel.scenecraft.common.pathing.player.RealtimeTimelinePlayer;
import net.patchedpixel.scenecraft.common.pathing.properties.CameraProperties;
import net.patchedpixel.scenecraft.common.pathing.properties.SpectatorProperty;
import net.patchedpixel.scenecraft.common.pathing.properties.TimestampProperty;
import net.patchedpixel.scenecraft.common.render.gui.GuiRenderQueue;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.camera.CameraEntity;
import net.patchedpixel.scenecraft.common.replay.gui.overlay.GuiReplayOverlay;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Keyframe;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Path;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Timeline;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.serialize.TimelineSerialization;
import net.patchedpixel.scenecraft.common.scenestudio.util.EntityPositionTracker;
import net.patchedpixel.scenecraft.common.simplepathing.SPTimeline;
import net.patchedpixel.scenecraft.common.simplepathing.SPTimeline.SPPath;
import net.patchedpixel.scenecraft.common.simplepathing.SceneCraftSimplePathing;
import net.patchedpixel.scenecraft.common.simplepathing.Setting;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.screen.popup.ProgressPopup;
import net.patchedpixel.scenecraft.common.flamex.widget.ScrollbarWidget;
import net.patchedpixel.scenecraft.common.flamex.widget.SpriteButton;
import net.patchedpixel.scenecraft.common.flamex.widget.TimelineTimeWidget;
import net.patchedpixel.scenecraft.common.utils.Result;
import net.patchedpixel.scenecraft.common.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.getMinecraft;
import static net.patchedpixel.scenecraft.common.simplepathing.SceneCraftSimplePathing.LOGGER;
import static net.patchedpixel.scenecraft.common.utils.Utils.error;

public class GuiPathing {
    private static final Logger logger = LogManager.getLogger();
    private static final int TEXTURE_SIZE = SceneCraft.TEXTURE_SIZE;

    public final SpriteButton playPauseButton;
    public final SpriteButton renderButton;
    public final SpriteButton positionKeyframeButton;
    public final SpriteButton timeKeyframeButton;
    public final GuiKeyframeTimeline timeline;
    public final ScrollbarWidget scrollbar = new ScrollbarWidget(100, 9);
    public final TimelineTimeWidget timelineTime = new TimelineTimeWidget(100, 8);
    public final SpriteButton zoomInButton;
    public final SpriteButton zoomOutButton;

    private final SceneCraft core;
    private final SceneCraftSimplePathing mod;
    private final ReplayHandler replayHandler;
    public final GuiReplayOverlay overlay;
    private final RealtimeTimelinePlayer player;

    // Whether any error which occured during entity tracker loading has already been shown to the user
    private boolean errorShown;
    private EntityPositionTracker entityTracker;
    private Consumer<Double> entityTrackerLoadingProgress;
    private SettableFuture<Void> entityTrackerFuture;

    public GuiPathing(final SceneCraft core, final SceneCraftSimplePathing mod, final ReplayHandler replayHandler) {
        this.core = core;
        this.mod = mod;
        this.replayHandler = replayHandler;
        this.overlay = replayHandler.getOverlay();
        this.player = new RealtimeTimelinePlayer(replayHandler);

        timeline = new GuiKeyframeTimeline(this) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                if(player.isActive()) {
                    setCursorPosition((int) player.getTimePassed()).ensureCursorVisibleWithPadding();
                }
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
            }
        };
        timeline.setMarkers();
        timeline.setLength(core.getSettingsRegistry().get(Setting.TIMELINE_LENGTH) * 1000);
        timelineTime.setTimeline(timeline);

        scrollbar.onValueChanged(() -> {
            timeline.setOffset((int) (scrollbar.getPosition() * timeline.getLength()));
            timeline.setZoom(scrollbar.getZoom());
        }).setZoom(0.1);

        playPauseButton = new SpriteButton(20, 20, SceneCraft.TEXTURE, TEXTURE_SIZE, 20, this::playPausePressed)
                .uv(() -> 0, () -> player.isActive() ? 20 : 0)
                .dynamicTooltip(() -> {
                    if(player.isActive()) {
                        return Component.translatable("scenecraft.gui.ingame.menu.pausepath");
                    } else if(InputConstants.isKeyDown(getMinecraft().getWindow().getWindow(), InputConstants.KEY_LCONTROL)) {
                        return Component.translatable("scenecraft.gui.ingame.menu.playpathfromstart");
                    } else {
                        return Component.translatable("scenecraft.gui.ingame.menu.playpath");
                    }
                });

        renderButton = new SpriteButton(20, 20, SceneCraft.TEXTURE, TEXTURE_SIZE, 20, () -> {
            abortPathPlayback();
            new GuiRenderQueue(null, replayHandler, () -> preparePathsForPlayback(false)).open();
        }).uv(40, 0);
        renderButton.setTooltip(Tooltip.create(Component.translatable("scenecraft.gui.ingame.menu.renderpath")));

        positionKeyframeButton = new SpriteButton(20, 20, SceneCraft.TEXTURE, TEXTURE_SIZE, 20,
                () -> toggleKeyframe(SPPath.POSITION, false))
                .uv(this::positionUvX, this::positionUvY)
                .dynamicTooltip(() -> {
                    String label;
                    if(positionUvY() == 40) { // Add keyframe
                        if(positionUvX() == 0) { // Position
                            label = "scenecraft.gui.ingame.menu.addposkeyframe";
                        } else { // Spectator
                            label = "scenecraft.gui.ingame.menu.addspeckeyframe";
                        }
                    } else { // Remove keyframe
                        if(positionUvX() == 0) { // Position
                            label = "scenecraft.gui.ingame.menu.removeposkeyframe";
                        } else { // Spectator
                            label = "scenecraft.gui.ingame.menu.removespeckeyframe";
                        }
                    }
                    return MCVer.literalText(I18n.get(label) + " (" + mod.keyPositionKeyframe.getBoundKey() + ")");
                });

        timeKeyframeButton = new SpriteButton(20, 20, SceneCraft.TEXTURE, TEXTURE_SIZE, 20,
                () -> toggleKeyframe(SPPath.TIME, false))
                .uv(() -> 0, this::timeUvY)
                .dynamicTooltip(() -> {
                    String label = timeUvY() == 80
                            ? "scenecraft.gui.ingame.menu.addtimekeyframe" // Add time keyframe
                            : "scenecraft.gui.ingame.menu.removetimekeyframe"; // Remove time keyframe
                    return MCVer.literalText(I18n.get(label) + " (" + mod.keyTimeKeyframe.getBoundKey() + ")");
                });

        zoomInButton = new SpriteButton(9, 9, SceneCraft.TEXTURE, TEXTURE_SIZE, 9, () -> zoomTimeline(2d / 3d)).uv(40, 20);
        zoomInButton.setTooltip(Tooltip.create(Component.translatable("scenecraft.gui.ingame.menu.zoomin")));
        zoomOutButton = new SpriteButton(9, 9, SceneCraft.TEXTURE, TEXTURE_SIZE, 9, () -> zoomTimeline(3d / 2d)).uv(40, 30);
        zoomOutButton.setTooltip(Tooltip.create(Component.translatable("scenecraft.gui.ingame.menu.zoomout")));

        for(net.minecraft.client.gui.components.AbstractWidget w : new net.minecraft.client.gui.components.AbstractWidget[]{
                playPauseButton, renderButton, positionKeyframeButton, timeKeyframeButton,
                timelineTime, timeline, scrollbar, zoomInButton, zoomOutButton}) {
            overlay.addWidget(w);
        }
        overlay.addLayoutHook(this::layout);

        startLoadingEntityTracker();
    }

    private void layout(int width, int height) {
        checkForAutoSync();
        int x0 = 10;
        int y0 = overlay.topPanel.getHeight() + 10 + 3;
        SpriteButton[] buttons = {playPauseButton, renderButton, positionKeyframeButton, timeKeyframeButton};
        for(int i = 0; i < buttons.length; i++) {
            buttons[i].setPosition(x0 + i * 25, y0 + 10);
        }
        int timelineX = x0 + buttons.length * 25;
        int zoomX = width - 10 - zoomInButton.getWidth();
        int w = Math.max(20, zoomX - 2 - timelineX);
        timelineTime.setWidth(w);
        timelineTime.setPosition(timelineX, y0 + 2);
        timeline.setWidth(w);
        timeline.setPosition(timelineX, y0 + 10);
        scrollbar.setWidth(w);
        scrollbar.setPosition(timelineX, y0 + 31);
        zoomInButton.setPosition(zoomX, y0 + 10);
        zoomOutButton.setPosition(zoomX, y0 + 21);
    }

    private int positionUvX() {
        SPPath keyframePath = mod.getSelectedPath();
        long keyframeTime = mod.getSelectedTime();
        if(keyframePath != SPPath.POSITION) {
            // No keyframe or wrong path
            keyframeTime = timeline.getCursorPosition();
            keyframePath = mod.getCurrentTimeline().isPositionKeyframe(keyframeTime) ? SPPath.POSITION : null;
        }
        if(keyframePath != SPPath.POSITION) {
            return replayHandler.isCameraView() ? 0 : 40;
        } else {
            return mod.getCurrentTimeline().isSpectatorKeyframe(keyframeTime) ? 40 : 0;
        }
    }

    private int positionUvY() {
        SPPath keyframePath = mod.getSelectedPath();
        if(keyframePath != SPPath.POSITION) {
            // No keyframe selected but there might be one at exactly the position of the cursor
            keyframePath = mod.getCurrentTimeline().isPositionKeyframe(timeline.getCursorPosition()) ? SPPath.POSITION : null;
        }
        return keyframePath == SPPath.POSITION ? 60 : 40;
    }

    private int timeUvY() {
        SPPath keyframePath = mod.getSelectedPath();
        if(keyframePath != SPPath.TIME) {
            // No keyframe selected but there might be one at exactly the position of the cursor
            keyframePath = mod.getCurrentTimeline().isTimeKeyframe(timeline.getCursorPosition()) ? SPPath.TIME : null;
        }
        return keyframePath == SPPath.TIME ? 100 : 80;
    }

    private void showInfo(String... lines) {
        Minecraft mc = MCVer.getMinecraft();
        net.minecraft.client.gui.screens.Screen previous = mc.screen;
        mc.setScreen(FlameX.info(() -> mc.setScreen(previous), lines));
    }

    private void playPausePressed() {
        if(player.isActive()) {
            player.getFuture().cancel(false);
        } else {
            boolean ignoreTimeKeyframes = InputConstants.isKeyDown(getMinecraft().getWindow().getWindow(), InputConstants.KEY_LSHIFT);

            Timeline timeline = preparePathsForPlayback(ignoreTimeKeyframes).okOrElse(err -> {
                if(err != null) showInfo(err);
                return null;
            });
            if(timeline == null) return;

            Path timePath = new SPTimeline(timeline).getTimePath();
            timePath.setActive(!ignoreTimeKeyframes);

            // Start from cursor time unless the control key is pressed (then start from beginning)
            int startTime = InputConstants.isKeyDown(getMinecraft().getWindow().getWindow(), InputConstants.KEY_LCONTROL)? 0 : GuiPathing.this.timeline.getCursorPosition();
            ListenableFuture<Void> future = player.start(timeline, startTime);
            overlay.setCloseable(false);
            overlay.setMouseVisible(true);
            core.printInfoToChat("scenecraft.chat.pathstarted");
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    if(future.isCancelled()) {
                        core.printInfoToChat("scenecraft.chat.pathinterrupted");
                    } else {
                        core.printInfoToChat("scenecraft.chat.pathfinished");
                    }
                    overlay.setCloseable(true);
                }

                @Override
                public void onFailure(Throwable t) {
                    if(!(t instanceof CancellationException)) {
                        t.printStackTrace();
                    }
                    overlay.setCloseable(true);
                }
            }, Runnable::run);
        }
    }

    private void abortPathPlayback() {
        if(!player.isActive()) {
            return;
        }

        ListenableFuture<Void> future = player.getFuture();
        if(!future.isDone() && !future.isCancelled()) {
            future.cancel(false);
        }
        // Tear down of the player might only happen the next tick after it was cancelled
        player.onTick();
    }

    public void keyframeRepoButtonPressed() {
        abortPathPlayback();
        try {
            TimelineSerialization serialization = new TimelineSerialization(mod.getCurrentTimeline(), null);
            String serialized = serialization.serialize(Collections.singletonMap("", mod.getCurrentTimeline().getTimeline()));
            Timeline currentTimeline = serialization.deserialize(serialized).get("");
            GuiKeyframeRepository gui = new GuiKeyframeRepository(
                    mod.getCurrentTimeline(), replayHandler.getReplayFile(), currentTimeline);
            Futures.addCallback(gui.getFuture(), new FutureCallback<Timeline>() {
                @Override
                public void onSuccess(Timeline result) {
                    if(result != null) {
                        mod.setCurrentTimeline(new SPTimeline(result));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                    core.printWarningToChat("Error loading timeline: " + t.getMessage());
                }
            }, Runnable::run);
            gui.display();
        } catch(IOException e) {
            e.printStackTrace();
            core.printWarningToChat("Error loading timeline: " + e.getMessage());
        }
    }

    public void clearKeyframesButtonPressed() {
        Minecraft mc = MCVer.getMinecraft();
        mc.setScreen(FlameX.confirm(mc.screen, "scenecraft.gui.clearcallback.title", "gui.yes", "gui.no", () -> {
            mod.clearCurrentTimeline();
            if(entityTracker != null) {
                mod.getCurrentTimeline().setEntityTracker(entityTracker);
            }
        }));
    }

    private int prevSpeed = -1;
    private int prevTime = -1;
    private void checkForAutoSync() {
        if(!mod.keySyncTime.isAutoActivating()) {
            prevSpeed = -1;
            prevTime = -1;
            return;
        }

        int speed = overlay.speedSlider.getValue();
        if(prevSpeed != speed && prevSpeed != -1) {
            syncTimeButtonPressed();
        }
        prevSpeed = speed;

        int time = replayHandler.getReplaySender().currentTimeStamp();
        if(prevTime != time && prevTime != -1 && !player.isActive()) {
            syncTimeButtonPressed();
        }
        prevTime = time;
    }

    private Integer computeSyncTime(int cursor) {
        // Current replay time
        int time = replayHandler.getReplaySender().currentTimeStamp();
        // Get the last time keyframe before the cursor
        Keyframe keyframe = mod.getCurrentTimeline().getTimePath().getKeyframes().stream()
                .filter(it -> it.getTime() <= cursor).reduce((__, last) -> last)
                .orElse(null);
        if(keyframe != null) {
            // Cursor position at the keyframe
            int keyframeCursor = (int) keyframe.getTime();
            // Replay time at the keyframe
            // This is a keyframe from the time path, so it _should_ always have a time property
            int keyframeTime = keyframe.getValue(TimestampProperty.PROPERTY).get();
            // Replay time passed
            int timePassed = time - keyframeTime;
            // Speed (set to 1 when shift is held)
            double speed = InputConstants.isKeyDown(getMinecraft().getWindow().getWindow(), InputConstants.KEY_LSHIFT) ? 1 : replayHandler.getOverlay().getSpeedSliderValue();
            // Cursor time passed
            int cursorPassed = (int) (timePassed / speed);
            // Return new position
            return keyframeCursor + cursorPassed;
        } else {
            // No keyframes before cursor
            return null;
        }
    }

    public void syncTimeButtonPressed() {
        // Position of the cursor
        int cursor = timeline.getCursorPosition();

        // Update cursor once
        Integer updatedCursor = computeSyncTime(cursor);
        if(updatedCursor == null) {
            return; // no keyframes before cursor, nothing we can do
        }
        cursor = updatedCursor;

        // Repeatedly update until we find a fix point
        while(true) {
            updatedCursor = computeSyncTime(cursor);
            if(updatedCursor == null) {
                // Cursor has gotten stuck before in front of all keyframes.
                // Let's just use the last value we got, this shouldn't happen with ordinary timelines anyway.
                break;
            }
            if(updatedCursor == cursor) {
                // Found the fix point, we can stop now
                break;
            }
            if(updatedCursor < cursor) {
                // We've gone backwards, we'll likely get stuck in a loop, so abort the whole thing
                return;
            }
            // Found a new position, take it, repeat
            cursor = updatedCursor;
        }

        // Move cursor to new position
        timeline.setCursorPosition(cursor).ensureCursorVisibleWithPadding();
        // Deselect keyframe to allow the user to add a new one right away
        mod.setSelected(null, 0);
    }

    public boolean deleteButtonPressed() {
        if(mod.getSelectedPath() != null) {
            toggleKeyframe(mod.getSelectedPath(), false);
            return true;
        }
        return false;
    }

    private void startLoadingEntityTracker() {
        Preconditions.checkState(entityTrackerFuture == null);
        // Start loading entity tracker
        entityTrackerFuture = SettableFuture.create();
        new Thread(() -> {
            EntityPositionTracker tracker = new EntityPositionTracker(replayHandler.getReplayFile());
            try {
                long start = System.currentTimeMillis();
                tracker.load(p -> {
                    if(entityTrackerLoadingProgress != null) {
                        entityTrackerLoadingProgress.accept(p);
                    }
                });
                logger.info("Loaded entity tracker in " + (System.currentTimeMillis() - start) + "ms");
            } catch(Throwable e) {
                logger.error("Loading entity tracker:", e);
                mod.getCore().runLater(() -> {
                    mod.getCore().printWarningToChat("Error loading entity tracker: %s", e.getLocalizedMessage());
                    entityTrackerFuture.setException(e);
                });
                return;
            }
            entityTracker = tracker;
            mod.getCore().runLater(() -> {
                entityTrackerFuture.set(null);
            });
        }).start();
    }

    private Result<Timeline, String[]> preparePathsForPlayback(boolean ignoreTimeKeyframes) {
        SPTimeline spTimeline = mod.getCurrentTimeline();

        String[] errors = validatePathsForPlayback(spTimeline, ignoreTimeKeyframes);
        if(errors != null) {
            return Result.err(errors);
        }

        try {
            TimelineSerialization serialization = new TimelineSerialization(spTimeline, null);
            String serialized = serialization.serialize(Collections.singletonMap("", spTimeline.getTimeline()));
            Timeline timeline = serialization.deserialize(serialized).get("");
            timeline.getPaths().forEach(Path::updateAll);
            return Result.ok(timeline);
        } catch(Throwable t) {
            error(LOGGER, CrashReport.forThrowable(t, "Cloning timeline"), () -> {});
            return Result.err(null);
        }
    }

    private String[] validatePathsForPlayback(SPTimeline timeline, boolean ignoreTimeKeyframes) {
        timeline.getTimeline().getPaths().forEach(Path::updateAll);

        // Make sure there are at least two position keyframes
        if(timeline.getPositionPath().getSegments().isEmpty()) {
            return new String[]{ "scenecraft.chat.morekeyframes" };
        }

        if(ignoreTimeKeyframes) {
            return null;
        }

        // Make sure time keyframes's values are monotonically increasing
        int lastTime = 0;
        for(Keyframe keyframe : timeline.getTimePath().getKeyframes()) {
            int time = keyframe.getValue(TimestampProperty.PROPERTY).orElseThrow(IllegalStateException::new);
            if(time < lastTime) {
                // We are going backwards in time
                return new String[]{
                        "scenecraft.error.negativetime1",
                        "scenecraft.error.negativetime2",
                        "scenecraft.error.negativetime3"
                };
            }
            lastTime = time;
        }

        // Make sure there are at least two time keyframes
        if(timeline.getTimePath().getSegments().isEmpty()) {
            return new String[]{ "scenecraft.chat.morekeyframes" };
        }

        return null;
    }

    public void zoomTimeline(double factor) {
        scrollbar.setZoom(scrollbar.getZoom() * factor);
    }

    public boolean loadEntityTracker(Runnable thenRun) {
        if(entityTracker == null && !errorShown) {
            LOGGER.debug("Entity tracker not yet loaded, delaying...");
            ProgressPopup popup = new ProgressPopup("scenecraft.gui.loadentitytracker", null);
            popup.open();
            entityTrackerLoadingProgress = p -> popup.setProgress(p.floatValue());
            Futures.addCallback(entityTrackerFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    popup.close();
                    if(mod.getCurrentTimeline().getEntityTracker() == null) {
                        mod.getCurrentTimeline().setEntityTracker(entityTracker);
                    }
                    thenRun.run();
                }

                @Override
                public void onFailure(@Nonnull Throwable t) {
                    if(!errorShown) {
                        String message = "Failed to load entity tracker, spectator keyframes will be broken.";
                        Utils.error(LOGGER, CrashReport.forThrowable(t, message), () -> {
                            popup.close();
                            thenRun.run();
                        });
                        errorShown = true;
                    } else {
                        thenRun.run();
                    }
                }
            }, Runnable::run);
            return false;
        }
        if(mod.getCurrentTimeline().getEntityTracker() == null) {
            mod.getCurrentTimeline().setEntityTracker(entityTracker);
        }
        return true;
    }

    /**
     * Called when either one of the property buttons is pressed.
     * @param path {@code TIME} for the time property button, {@code POSITION} for the place property button
     * @param neverSpectator when true, will insert a position keyframe even when currently spectating an entity
     */
    public void toggleKeyframe(SPPath path, boolean neverSpectator) {
        LOGGER.debug("Updating keyframe on path {}" + path);
        if(!loadEntityTracker(() -> toggleKeyframe(path, neverSpectator))) return;

        int time = timeline.getCursorPosition();
        SPTimeline timeline = mod.getCurrentTimeline();

        if(timeline.getPositionPath().getKeyframes().isEmpty() &&
                timeline.getTimePath().getKeyframes().isEmpty() &&
                time > 1000) {
            String text = I18n.get("scenecraft.gui.ingame.first_keyframe_not_at_start_warning");
            showInfo(text.split("\\\\n"));
        }

        switch(path) {
            case TIME:
                if(mod.getSelectedPath() == path) {
                    LOGGER.debug("Selected keyframe is time keyframe -> removing keyframe");
                    timeline.removeTimeKeyframe(mod.getSelectedTime());
                    mod.setSelected(null, 0);
                } else if(timeline.isTimeKeyframe(time)) {
                    LOGGER.debug("Keyframe at cursor position is time keyframe -> removing keyframe");
                    timeline.removeTimeKeyframe(time);
                    mod.setSelected(null, 0);
                } else {
                    LOGGER.debug("No time keyframe found -> adding new keyframe");
                    timeline.addTimeKeyframe(time, replayHandler.getReplaySender().currentTimeStamp());
                    mod.setSelected(path, time);
                }
                break;
            case POSITION:
                if(mod.getSelectedPath() == path) {
                    LOGGER.debug("Selected keyframe is position keyframe -> removing keyframe");
                    timeline.removePositionKeyframe(mod.getSelectedTime());
                    mod.setSelected(null, 0);
                } else if(timeline.isPositionKeyframe(time)) {
                    LOGGER.debug("Keyframe at cursor position is position keyframe -> removing keyframe");
                    timeline.removePositionKeyframe(time);
                    mod.setSelected(null, 0);
                } else {
                    LOGGER.debug("No position keyframe found -> adding new keyframe");
                    CameraEntity camera = replayHandler.getCameraEntity();
                    int spectatedId = -1;
                    if(!replayHandler.isCameraView() && !neverSpectator) {
                        spectatedId = replayHandler.getOverlay().getMinecraft().getCameraEntity().getId();
                    }
                    timeline.addPositionKeyframe(time, camera.getX(), camera.getY(), camera.getZ(),
                            camera.getYRot(), camera.getXRot(), camera.roll, spectatedId);
                    mod.setSelected(path, time);
                }
                break;
        }
    }

    public SceneCraftSimplePathing getMod() {
        return mod;
    }

    public EntityPositionTracker getEntityTracker() {
        return entityTracker;
    }

    public void openEditKeyframePopup(SPPath path, long time) {
        if(!loadEntityTracker(() -> openEditKeyframePopup(path, time))) return;
        Keyframe keyframe = mod.getCurrentTimeline().getKeyframe(path, time);
        if(keyframe.getProperties().contains(SpectatorProperty.PROPERTY)) {
            new GuiEditKeyframe.Spectator(this, path, keyframe.getTime()).open();
        } else if(keyframe.getProperties().contains(CameraProperties.POSITION)) {
            new GuiEditKeyframe.Position(this, path, keyframe.getTime()).open();
        } else {
            new GuiEditKeyframe.Time(this, path, keyframe.getTime()).open();
        }
    }
}
