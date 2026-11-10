package net.patchedpixel.scenecraft.platform;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;

public final class ConnectionAccess {
    private ConnectionAccess() {}

    // Connection.channel() method is additional getter added by NeoForge not by vanilla
    public static Channel getChannel(Connection connection) {
        return connection.channel();
    }
}
