package net.patchedpixel.scenecraft.scenestudio.protocol;

import net.minecraft.SharedConstants;

/**
 * The protocol version of the running game. Replays can only be read when they were recorded with the
 * exact same protocol version because no packet conversion is done.
 */
public final class CurrentProtocol {
    private CurrentProtocol() {
    }

    /**
     * @return The protocol version number of the running Minecraft version
     */
    public static int get() {
        return SharedConstants.getCurrentVersion().getProtocolVersion();
    }
}
