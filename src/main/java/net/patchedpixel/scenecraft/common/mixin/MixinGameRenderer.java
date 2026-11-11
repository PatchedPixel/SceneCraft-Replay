package net.patchedpixel.scenecraft.common.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import net.patchedpixel.scenecraft.common.core.events.PreRenderHandCallback;
import net.patchedpixel.scenecraft.common.render.hooks.EntityRendererHandler;
import net.patchedpixel.scenecraft.common.render.hooks.FramebufferDelegateHolder;
import net.patchedpixel.scenecraft.common.replay.camera.CameraEntity;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.getMinecraft;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements EntityRendererHandler.IEntityRenderer, FramebufferDelegateHolder {
    private EntityRendererHandler sceneCraftRender_handler;

    @Unique
    private RenderTarget framebufferDelegate;

    @Override
    public void sceneCraftRender_setHandler(EntityRendererHandler handler) {
        this.sceneCraftRender_handler = handler;
    }

    @Override
    public EntityRendererHandler sceneCraftRender_getHandler() {
        return sceneCraftRender_handler;
    }

    @Override
    public void setFramebufferDelegate(RenderTarget framebuffer) {
        this.framebufferDelegate = framebuffer;
    }

    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void useGuiFramebuffer(CallbackInfoReturnable<RenderTarget> ci) {
        RenderTarget delegate = this.framebufferDelegate;
        if(delegate != null) {
            ci.setReturnValue(delegate);
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void preRenderHand(CallbackInfo ci) {
        if(PreRenderHandCallback.EVENT.invoker().preRenderHand()) {
            ci.cancel();
        }
    }

    @WrapWithCondition(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V")
    )
    private boolean sceneCraftRender_skipClearWhenRecordingDepthGui(CommandEncoder instance, GpuTexture texture, double v) {
        EntityRendererHandler handler = this.sceneCraftRender_getHandler();
        return handler == null || !handler.getSettings().isDepthMap();
    }

    @WrapWithCondition(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V")
    )
    private boolean sceneCraftRender_skipClearWhenRecordingDepthHand(CommandEncoder instance, GpuTexture texture, double v) {
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
        if(camera instanceof CameraEntity) {
            return camera.isSpectator() ? GameType.SPECTATOR : GameType.SURVIVAL;
        }
        return interactionManager.getPlayerMode();
    }
}
