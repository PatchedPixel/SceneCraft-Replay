package net.patchedpixel.scenecraft.platform;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraftforge.network.NetworkConstants;
import net.patchedpixel.scenecraft.platform.mixin.NetworkConstantsAccessor;

public final class ConnectionAccess {
    private ConnectionAccess() {}

    // Connection#channel() only exists because Forge patches it in.
    public static Channel getChannel(Connection connection) {
        return connection.channel();
    }

    /**
     * Makes Forge treat the connection as one to a modded server, so that modded packets are not rejected as vanilla.
     */
    public static void markAsModdedConnection(Channel channel) {
        channel.attr(NetworkConstantsAccessor.sceneCraft$getFmlNetVersion()).set(NetworkConstants.NETVERSION);
    }
}
