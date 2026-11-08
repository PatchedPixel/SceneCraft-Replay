package net.patchedpixel.scenecraft.common.mixin.gui;

import net.patchedpixel.scenecraft.common.gui.versions.callbacks.OpenGuiScreenCallback;
import net.patchedpixel.scenecraft.common.gui.versions.callbacks.PreTickCallback;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {
    @Inject(method = "tick", at = @At("HEAD"))
    private void preTick(CallbackInfo ci) {
        PreTickCallback.EVENT.invoker().preTick();
    }

    @Inject(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private void openGuiScreen(Screen newGuiScreen, CallbackInfo ci) {
        OpenGuiScreenCallback.EVENT.invoker().openGuiScreen(newGuiScreen);
    }
}
