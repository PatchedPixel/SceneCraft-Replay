package net.patchedpixel.scenecraft.platform;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;

public final class ConnectionAccess {
    private ConnectionAccess() {}

    public static Channel getChannel(Connection connection) {
        return connection.channel();
    }
}
