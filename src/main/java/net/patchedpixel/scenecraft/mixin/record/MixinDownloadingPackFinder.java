package net.patchedpixel.scenecraft.mixin.record;

import net.patchedpixel.scenecraft.record.SceneCraftRecorder;
import net.patchedpixel.scenecraft.record.packet.PacketListener;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@Mixin(DownloadedPackSource.class)
public abstract class MixinDownloadingPackFinder {
    @Unique
    private volatile String sceneCraftRequestedUrl;

    @Inject(method = "downloadAndSelectResourcePack", at = @At("HEAD"))
    private void rememberRequestedUrl(URL url, String hash, boolean required, CallbackInfoReturnable<CompletableFuture<?>> cir) {
        this.sceneCraftRequestedUrl = url.toString();
    }

    @Inject(method = "setServerPack", at = @At("HEAD"))
    private void recordDownloadedPack(File file, PackSource source, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        PacketListener packetListener = SceneCraftRecorder.instance.getConnectionEventHandler().getPacketListener();
        if (packetListener == null) {
            return;
        }
        packetListener.getResourcePackRecorder().recordResourcePack(file.toPath(), this.sceneCraftRequestedUrl);
    }
}
