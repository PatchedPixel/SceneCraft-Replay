package net.patchedpixel.scenecraft.mixin;

import net.patchedpixel.scenecraft.core.events.KeyBindingEventCallback;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {
    @Inject(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V", shift = At.Shift.AFTER))
    private void afterKeyBindingTick(CallbackInfo ci) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
    }
}
