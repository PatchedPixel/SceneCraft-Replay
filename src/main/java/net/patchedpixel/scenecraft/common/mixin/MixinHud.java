package net.patchedpixel.scenecraft.common.mixin;

import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.render.hooks.EntityRendererHandler;
import net.patchedpixel.scenecraft.common.replay.events.RenderHotbarCallback;
import net.patchedpixel.scenecraft.common.replay.events.RenderSpectatorCrosshairCallback;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Hud.class)
public abstract class MixinHud {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void sceneCraftRender_skipHudDuringRender(CallbackInfo ci) {
        if(((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).sceneCraftRender_getHandler() != null) {
            ci.cancel();
        }
    }

    @Inject(method = "canRenderCrosshairForSpectator", at = @At("HEAD"), cancellable = true)
    private void shouldRenderSpectatorCrosshair(CallbackInfoReturnable<Boolean> ci) {
        Boolean state = RenderSpectatorCrosshairCallback.EVENT.invoker().shouldRenderSpectatorCrosshair();
        if(state != null) {
            ci.setReturnValue(state);
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void shouldRenderHotbar(CallbackInfo ci) {
        Boolean state = RenderHotbarCallback.EVENT.invoker().shouldRenderHotbar();
        if(state == Boolean.FALSE) {
            ci.cancel();
        }
    }
}
