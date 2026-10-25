package net.patchedpixel.scenecraft.pathing.player;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Keyframe;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Path;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Timeline;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;

import javax.annotation.Nullable;
import java.util.Iterator;

import static net.patchedpixel.scenecraft.platform.Utils.DEFAULT_MS_PER_TICK;
import static net.patchedpixel.scenecraft.core.versions.MCVer.*;

/**
 * Plays a timeline.
 */
public abstract class AbstractTimelinePlayer extends EventRegistrations {
    private final Minecraft mc = getMinecraft();
    private final ReplayHandler replayHandler;
    private Timeline timeline;
    protected long startOffset;
    private boolean wasAsyncMode;
    private DeltaTracker.Timer orgTimer;
    private long lastTime;
    private long lastTimestamp;
    private ListenableFuture<Void> future;
    private SettableFuture<Void> settableFuture;

    public AbstractTimelinePlayer(ReplayHandler replayHandler) {
        this.replayHandler = replayHandler;
    }

    public ListenableFuture<Void> start(Timeline timeline, long from) {
        startOffset = from;
        return start(timeline);
    }

    public ListenableFuture<Void> start(Timeline timeline) {
        this.timeline = timeline;

        Iterator<Keyframe> iter = Iterables.concat(Iterables.transform(timeline.getPaths(),
                new Function<Path, Iterable<Keyframe>>() {
            @Nullable
            @Override
            public Iterable<Keyframe> apply(@Nullable Path input) {
                assert input != null;
                return input.getKeyframes();
            }
        })).iterator();
        if (!iter.hasNext()) {
            lastTimestamp = 0;
        } else {
            lastTimestamp = new Ordering<Keyframe>() {
                @Override
                public int compare(@Nullable Keyframe left, @Nullable Keyframe right) {
                    assert left != null;
                    assert right != null;
                    return Longs.compare(left.getTime(), right.getTime());
                }
            }.max(iter).getTime();
        }

        wasAsyncMode = replayHandler.getReplaySender().isAsyncMode();
        replayHandler.getReplaySender().setSyncModeAndWait();
        register();
        lastTime = 0;

        orgTimer = mc.deltaTracker;
        ReplayTimer timer = new ReplayTimer();
        mc.deltaTracker = timer;

        //noinspection ConstantConditions
        timer.msPerTick = DEFAULT_MS_PER_TICK;
        timer.tickDelta = timer.ticksThisFrame = 0;
        return future = settableFuture = SettableFuture.create();
    }

    public ListenableFuture<Void> getFuture() {
        return future;
    }

    public boolean isActive() {
        return future != null && !future.isDone();
    }

    { on(ReplayTimer.UpdatedCallback.EVENT, this::onTick); }
    public void onTick() {
        if (future.isDone()) {
            mc.deltaTracker = orgTimer;
            replayHandler.getReplaySender().setReplaySpeed(0);
            if (wasAsyncMode) {
                replayHandler.getReplaySender().setAsyncMode(true);
            }
            unregister();
            return;
        }
        long time = getTimePassed();
        if (time > lastTimestamp) {
            time = lastTimestamp;
        }

        // Apply to timeline
        timeline.applyToGame(time, replayHandler);
        // Apply a second time in case same of the packets have moved the camera from where it was
        timeline.applyToGame(time, replayHandler);

        // Update minecraft timer
        long replayTime = replayHandler.getReplaySender().currentTimeStamp();
        if (lastTime == 0) {
            // First frame, no change yet
            lastTime = replayTime;
        }
        float timeInTicks = replayTime / 50f;
        float previousTimeInTicks = lastTime / 50f;
        float passedTicks = timeInTicks - previousTimeInTicks;
        DeltaTracker renderTickCounter = mc.deltaTracker;
        if (renderTickCounter instanceof ReplayTimer) {
            ReplayTimer timer = (ReplayTimer) renderTickCounter;
            timer.tickDelta += passedTicks;
            timer.ticksThisFrame = (int) timer.tickDelta;
            timer.tickDelta -= timer.ticksThisFrame;
        }

        lastTime = replayTime;

        if (time >= lastTimestamp) {
            settableFuture.set(null);
        }
    }

    public abstract long getTimePassed();
}
