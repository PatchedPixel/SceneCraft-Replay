package net.patchedpixel.scenecraft.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ClientPayloadRegistry {
    private final PayloadRegistrar registrar;

    public ClientPayloadRegistry(RegisterPayloadHandlersEvent event) {
        // optional: servers and clients are allowed to connect even if the other side doesn't have the payloads
        this.registrar = event.registrar("1").optional();
    }

    public <T extends CustomPacketPayload> void configurationToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
        registrar.configurationToClient(type, codec, (_, _) -> {});
    }

    public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
        registrar.playToClient(type, codec, (_, _) -> {});
    }
}
