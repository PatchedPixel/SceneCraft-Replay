package net.patchedpixel.scenecraft.common.mixin.record;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.record.SceneCraftRecorder;
import net.patchedpixel.scenecraft.common.record.packet.PacketListener;
import net.patchedpixel.scenecraft.common.scenestudio.protocol.State;
import net.patchedpixel.scenecraft.common.scenestudio.protocol.Packet;
import net.patchedpixel.scenecraft.common.scenestudio.protocol.PacketType;
import net.patchedpixel.scenecraft.common.scenestudio.protocol.PacketTypeRegistry;
import net.patchedpixel.scenecraft.common.scenestudio.protocol.packets.PacketEnabledPacksData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientConfigurationPacketListenerImpl.class)
public abstract class MixinNetHandlerConfigClient {
    @Inject(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setupInboundProtocol(Lnet/minecraft/network/ProtocolInfo;Lnet/minecraft/network/PacketListener;)V"))
    public void recordEnabledPackData(CallbackInfo ci, @Local RegistryAccess.Frozen registryManager) {
        PacketListener packetListener = SceneCraftRecorder.instance.getConnectionEventHandler().getPacketListener();
        if(packetListener == null) return;

        ByteBuf byteBuf = Unpooled.buffer();
        FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
        buf.writeUtf(PacketEnabledPacksData.ID);
        RegistryOps<Tag> ops = registryManager.createSerializationContext(NbtOps.INSTANCE);
        buf.writeVarInt(1);
        write(buf, registryManager.lookupOrThrow(Registries.DIMENSION_TYPE), DimensionType.DIRECT_CODEC, ops);

        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        byteBuf.release();

        int packetIdCustomPayload = getPacketId(new ClientboundCustomPayloadPacket(new BrandPayload("")));
        int packetIdFinish = getPacketId(ClientboundFinishConfigurationPacket.INSTANCE);

        PacketTypeRegistry registry = MCVer.getPacketTypeRegistry(State.CONFIGURATION);
        packetListener.save(new Packet(registry, packetIdCustomPayload, PacketType.ConfigCustomPayload, Unpooled.wrappedBuffer(bytes)));
        packetListener.save(new Packet(registry, packetIdFinish, PacketType.ConfigFinish, Unpooled.buffer()));
    }

    @Unique
    private <T> void write(FriendlyByteBuf buf, Registry<T> registry, Codec<T> codec, RegistryOps<Tag> ops) {
        buf.writeUtf(registry.key().identifier().toString());
        buf.writeVarInt(registry.size());
        for(Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
            buf.writeUtf(entry.getKey().identifier().toString());
            buf.writeNbt(codec.encodeStart(ops, entry.getValue()).getOrThrow());
        }
    }

    @Unique
    private int getPacketId(net.minecraft.network.protocol.Packet<? super ClientConfigurationPacketListener> packet) {
        ByteBuf byteBuf = Unpooled.buffer();
        try {
            ConfigurationProtocols.CLIENTBOUND.codec().encode(byteBuf, packet);
            return new FriendlyByteBuf(byteBuf).readVarInt();
        } finally {
            byteBuf.release();
        }
    }
}
