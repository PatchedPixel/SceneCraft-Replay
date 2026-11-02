package net.patchedpixel.scenecraft.mixin.record;

import net.patchedpixel.scenecraft.record.SceneCraftRecorder;
import net.patchedpixel.scenecraft.record.packet.PacketListener;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.server.packs.DownloadQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPackManager.class)
public abstract class MixinDownloadingPackFinder {
    @Inject(method = "onDownload", at = @At("HEAD"))
    private void recordDownloadedPack(@Coerce Object packs, DownloadQueue.BatchResult result, CallbackInfo ci) {
        PacketListener packetListener = SceneCraftRecorder.instance.getConnectionEventHandler().getPacketListener();
        if(packetListener == null) {
            return;
        }
        for(Map.Entry<UUID, Path> entry : result.downloaded().entrySet()) {
            packetListener.getResourcePackRecorder().recordResourcePack(entry.getValue(), entry.getKey());
        }
    }
}
