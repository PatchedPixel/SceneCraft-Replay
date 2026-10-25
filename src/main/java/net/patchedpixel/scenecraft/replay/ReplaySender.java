package net.patchedpixel.scenecraft.replay;

import net.minecraft.client.Minecraft;

import static net.patchedpixel.scenecraft.core.versions.MCVer.getMinecraft;

public interface ReplaySender {
    int currentTimeStamp();

    /**
     * Whether the replay is currently paused.
     * @return {@code true} if it is paused, {@code false} otherwise
     */
    public default boolean paused() {
        Minecraft mc = getMinecraft();
        return mc.timer.msPerTick == Float.POSITIVE_INFINITY;
    }

    void setReplaySpeed(double factor);
    double getReplaySpeed();

    boolean isAsyncMode();
    void setAsyncMode(boolean async);
    void setSyncModeAndWait();

    void jumpToTime(int value); // async
    void sendPacketsTill(int replayTime); // sync
}
