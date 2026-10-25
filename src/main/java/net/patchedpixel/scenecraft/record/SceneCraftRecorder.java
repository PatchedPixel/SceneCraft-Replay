package net.patchedpixel.scenecraft.record;

import net.patchedpixel.scenecraft.platform.KeyBindingRegistry;
import net.patchedpixel.scenecraft.core.Module;
import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.utils.Restrictions;
import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.record.handler.ConnectionEventHandler;
import net.patchedpixel.scenecraft.record.handler.GuiHandler;
import net.patchedpixel.scenecraft.record.packet.PacketListener;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.scenestudio.protocol.packets.PacketEnabledPacksData;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

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
            if (packetListener != null) {
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

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional()
                .configurationToClient(Restrictions.ID, Restrictions.CODEC, (_, _) -> {})
                .playToClient(Restrictions.ID, Restrictions.CODEC, (_, _) -> {})
                .configurationToClient(ENABLED_PACKS_DATA, ENABLED_PACKS_CODEC, (_, _) -> {});
    }

    private static final CustomPacketPayload.Type<EnabledPacksPayload> ENABLED_PACKS_DATA = new CustomPacketPayload.Type<>(Identifier.parse(PacketEnabledPacksData.ID));
    private static final StreamCodec<? super FriendlyByteBuf, EnabledPacksPayload> ENABLED_PACKS_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeBytes(payload.bytes()),
                    buf -> {
                        byte[] bytes = new byte[buf.readableBytes()];
                        buf.readBytes(bytes);
                        return new EnabledPacksPayload(bytes);
                    });

    private record EnabledPacksPayload(byte[] bytes) implements CustomPacketPayload {
        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return ENABLED_PACKS_DATA;
        }
    }

    public void initiateRecording(Connection networkManager) {
        Channel channel = networkManager.channel();
        if (channel.pipeline().get(ReplayHandler.PACKET_HANDLER_NAME) != null) return;
        if (channel.hasAttr(ATTR_CHECKED)) return;
        channel.attr(ATTR_CHECKED).set(null);
        connectionEventHandler.onConnectedToServerEvent(networkManager);
    }
}
