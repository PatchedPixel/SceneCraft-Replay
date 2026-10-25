package net.patchedpixel.scenecraft.mixin.render;

import net.patchedpixel.scenecraft.render.hooks.ForceChunkLoadingHook;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.thread.ProcessorMailbox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ChunkRenderDispatcher.class)
public abstract class Mixin_BlockOnChunkRebuilds implements ForceChunkLoadingHook.IBlockOnChunkRebuilds {
    @Shadow
    @Final
    private Queue<ChunkBufferBuilderPack> freeBuffers;

    @Shadow
    @Final
    private Queue<Runnable> toUpload;

    @Shadow
    @Final
    private ProcessorMailbox<Runnable> mailbox;

    @Shadow
    private volatile int toBatchCount;

    @Shadow
    public abstract void uploadAllPendingUploads();

    @Unique
    private int sceneCraftRender_totalBuffers;

    @Unique
    private long sceneCraftRender_uploadEvents;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sceneCraftRender_rememberBufferCount(CallbackInfo ci) {
        this.sceneCraftRender_totalBuffers = this.freeBuffers.size();
    }

    @Inject(method = "uploadAllPendingUploads", at = @At("HEAD"))
    private void sceneCraftRender_countUploads(CallbackInfo ci) {
        if (!this.toUpload.isEmpty()) {
            this.sceneCraftRender_uploadEvents++;
        }
    }

    @Unique
    private boolean sceneCraftRender_isIdle() {
        return this.toBatchCount == 0
                && this.mailbox.size() == 0
                && this.freeBuffers.size() >= this.sceneCraftRender_totalBuffers;
    }

    @Override
    public boolean uploadEverythingBlocking() {
        long before = this.sceneCraftRender_uploadEvents;
        int stableChecks = 0;
        while (stableChecks < 3) {
            this.uploadAllPendingUploads();
            if (sceneCraftRender_isIdle() && this.toUpload.isEmpty()) {
                stableChecks++;
            } else {
                stableChecks = 0;
            }
            if (stableChecks < 3) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return this.sceneCraftRender_uploadEvents != before;
    }
}
