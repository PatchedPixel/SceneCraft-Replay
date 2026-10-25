package net.patchedpixel.scenecraft.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.patchedpixel.scenecraft.core.events.PreRenderHandCallback;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.GameType;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.patchedpixel.scenecraft.core.versions.MCVer.getMinecraft;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements EntityRendererHandler.IEntityRenderer {
    private EntityRendererHandler sceneCraftRender_handler;

    @Override
    public void sceneCraftRender_setHandler(EntityRendererHandler handler) {
        this.sceneCraftRender_handler = handler;
    }

    @Override
    public EntityRendererHandler sceneCraftRender_getHandler() {
        return sceneCraftRender_handler;
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void preRenderHand(CallbackInfo ci) {
        if (PreRenderHandCallback.EVENT.invoker().preRenderHand()) {
            ci.cancel();
        }
    }

    @WrapWithCondition(
            method = {"render", "renderLevel"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V")
    )
    private boolean sceneCraftRender_skipDepthClearWhenRecordingDepth(int mask, boolean checkError) {
        if (mask != 256) {
            return true;
        }
        EntityRendererHandler handler = this.sceneCraftRender_getHandler();
        return handler == null || !handler.getSettings().isDepthMap();
    }

    @Redirect(
            method = "renderItemInHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;"
            )
    )
    private GameType getGameMode(MultiPlayerGameMode interactionManager) {
        LocalPlayer camera = getMinecraft().player;
        if (camera instanceof CameraEntity) {
            return camera.isSpectator() ? GameType.SPECTATOR : GameType.SURVIVAL;
        }
        return interactionManager.getPlayerMode();
    }
}
