package net.patchedpixel.scenecraft.mixin.render;

import net.patchedpixel.scenecraft.render.hooks.ForceChunkLoadingHook;
import net.patchedpixel.scenecraft.render.utils.FlawlessFrames;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Mixin(LevelRenderer.class)
public abstract class Mixin_ForceChunkLoading {
    @Shadow
    private ChunkRenderDispatcher chunkRenderDispatcher;

    @Shadow
    private Future<?> lastFullRenderChunkUpdate;

    @Shadow
    protected abstract void setupRender(Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean spectator);

    @Shadow
    protected abstract void compileChunks(Camera camera);

    @Unique
    private boolean sceneCraftRender_passThroughSetupRender;

    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    private void sceneCraftRender_forceAllChunks(Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean spectator, CallbackInfo ci) {
        if (!ForceChunkLoadingHook.enabled || FlawlessFrames.hasSodium()) {
            return;
        }
        if (sceneCraftRender_passThroughSetupRender) {
            return;
        }

        sceneCraftRender_passThroughSetupRender = true;
        try {
            boolean uploadedSomething;
            do {
                setupRender(camera, frustum, hasCapturedFrustum, spectator);

                compileChunks(camera);

                Future<?> update = this.lastFullRenderChunkUpdate;
                if (update != null) {
                    try {
                        update.get(5, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                }

                uploadedSomething = ((ForceChunkLoadingHook.IBlockOnChunkRebuilds) this.chunkRenderDispatcher).uploadEverythingBlocking();
            } while (uploadedSomething);
        } finally {
            sceneCraftRender_passThroughSetupRender = false;
        }

        ci.cancel();
    }
}
