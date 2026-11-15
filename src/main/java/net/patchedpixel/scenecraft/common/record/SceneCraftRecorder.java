package net.patchedpixel.scenecraft.common.record;

import net.patchedpixel.scenecraft.platform.ConnectionAccess;
import net.patchedpixel.scenecraft.platform.KeyBindingRegistry;
import net.patchedpixel.scenecraft.common.core.Module;
import net.patchedpixel.scenecraft.common.SceneCraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.common.record.handler.ConnectionEventHandler;
import net.patchedpixel.scenecraft.common.record.handler.GuiHandler;
import net.patchedpixel.scenecraft.common.record.packet.PacketListener;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SceneCraftRecorder implements Module {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AttributeKey<Void> ATTR_CHECKED = AttributeKey.newInstance("SceneCraftRecording_checked");

    { instance = this; }
    public static SceneCraftRecorder instance;

    private final SceneCraft core;

    private ConnectionEventHandler connectionEventHandler;

    public SceneCraftRecorder(SceneCraft mod) {
        core = mod;
        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("scenecraft.input.marker", InputConstants.KEY_M, () -> {
            PacketListener packetListener = connectionEventHandler.getPacketListener();
            if(packetListener != null) {
                packetListener.addMarker(null);
                core.printInfoToChat("scenecraft.chat.addedmarker");
            }
        }, false);
    }

    @Override
    public void initClient() {
        connectionEventHandler = new ConnectionEventHandler(LOGGER, core);
        new GuiHandler(core).register();
    }

    public ConnectionEventHandler getConnectionEventHandler() {
        return connectionEventHandler;
    }

    public void initiateRecording(Connection networkManager) {
        Channel channel = ConnectionAccess.getChannel(networkManager);
        if(channel.pipeline().get(ReplayHandler.PACKET_HANDLER_NAME) != null) return;
        if(channel.hasAttr(ATTR_CHECKED)) return;
        channel.attr(ATTR_CHECKED).set(null);
        connectionEventHandler.onConnectedToServerEvent(networkManager);
    }
}
