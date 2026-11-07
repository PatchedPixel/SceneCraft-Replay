package net.patchedpixel.scenecraft.common.replay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.getMinecraft;

public interface ReplaySender {
    int currentTimeStamp();

    /**
     * Whether the replay is currently paused.
     * @return {@code true} if it is paused, {@code false} otherwise
     */
    default boolean paused() {
        Minecraft mc = getMinecraft();
        DeltaTracker.Timer timer = (DeltaTracker.Timer) mc.getDeltaTracker();
        return timer.msPerTick == Float.POSITIVE_INFINITY;
    }

    void setReplaySpeed(double factor);
    double getReplaySpeed();

    boolean isAsyncMode();
    void setAsyncMode(boolean async);
    void setSyncModeAndWait();

    void jumpToTime(int value); // async
    void sendPacketsTill(int replayTime); // sync
}
