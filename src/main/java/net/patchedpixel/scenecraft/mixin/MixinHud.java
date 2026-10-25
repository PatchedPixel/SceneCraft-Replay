package net.patchedpixel.scenecraft.mixin;

import net.patchedpixel.scenecraft.replay.events.RenderSpectatorCrosshairCallback;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Gui.class)
public abstract class MixinHud {
    @Inject(method = "canRenderCrosshairForSpectator", at = @At("HEAD"), cancellable = true)
    private void shouldRenderSpectatorCrosshair(HitResult hitResult, CallbackInfoReturnable<Boolean> ci) {
        Boolean state = RenderSpectatorCrosshairCallback.EVENT.invoker().shouldRenderSpectatorCrosshair();
        if (state != null) {
            ci.setReturnValue(state);
        }
    }
}
