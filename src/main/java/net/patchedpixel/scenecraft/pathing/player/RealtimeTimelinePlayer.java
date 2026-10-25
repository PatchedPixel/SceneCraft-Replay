package net.patchedpixel.scenecraft.pathing.player;

import com.google.common.util.concurrent.ListenableFuture;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Timeline;

/**
 * Timeline player using the system time.
 */
public class RealtimeTimelinePlayer extends AbstractTimelinePlayer {
    /**
     * Wether the next frame is the first frame.
     * We only start measuring time from the second frame
     * as the first might have to jump in time which might take time.
     */
    private boolean firstFrame;
    private boolean secondFrame;

    /**
     * System time in milliseconds at the start.
     */
    private long startTime;

    private boolean loadingResources;
    private long timeBeforeResourceLoading;

    public RealtimeTimelinePlayer(ReplayHandler replayHandler) {
        super(replayHandler);
    }

    @Override
    public ListenableFuture<Void> start(Timeline timeline) {
        firstFrame = true;
        loadingResources = false;
        return super.start(timeline);
    }

    @Override
    public void onTick() {
        if (secondFrame) {
            secondFrame = false;
            startTime = System.currentTimeMillis() - startOffset;
        }

        if (MCVer.getMinecraft().getOverlay() != null) {
            if (!loadingResources) {
                timeBeforeResourceLoading = getTimePassed();
                loadingResources = true;
            }
            super.onTick();
            return;
        } else if (loadingResources && !firstFrame) {
            startTime = System.currentTimeMillis() - timeBeforeResourceLoading;
            loadingResources = false;
        }

        super.onTick();

        if (firstFrame) {
            firstFrame = false;
            secondFrame = true;
        }
    }

    @Override
    public long getTimePassed() {
        if (firstFrame) return startOffset;
        if (loadingResources) return timeBeforeResourceLoading;
        return System.currentTimeMillis() - startTime;
    }
}
