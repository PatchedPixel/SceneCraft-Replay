package net.patchedpixel.scenecraft.mixin.record;

import net.patchedpixel.scenecraft.record.SceneCraftRecorder;
import net.patchedpixel.scenecraft.record.packet.PacketListener;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientHandshakePacketListenerImpl.class)
public abstract class MixinNetHandlerLoginClient {
    @Final @Shadow
    private Connection connection;

    @Inject(method = "handleCustomQuery", at=@At("HEAD"))
    private void earlyInitiateRecording(ClientboundCustomQueryPacket packet, CallbackInfo ci) {
        initiateRecording(packet);
    }

    @Inject(method = "handleGameProfile", at=@At("HEAD"))
    private void lateInitiateRecording(ClientboundGameProfilePacket packet, CallbackInfo ci) {
        initiateRecording(packet);
    }

    private void initiateRecording(Packet<?> packet) {
        if (SceneCraftRecorder.instance.getConnectionEventHandler().getPacketListener() != null) {
            return; // already record
        }
        SceneCraftRecorder.instance.initiateRecording(this.connection);
        PacketListener packetListener = SceneCraftRecorder.instance.getConnectionEventHandler().getPacketListener();
        if (packetListener != null) {
            packetListener.save(packet);
        }
    }
}
