package net.patchedpixel.scenecraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.patchedpixel.scenecraft.core.events.KeyBindingEventCallback;
import net.patchedpixel.scenecraft.core.events.KeyEventCallback;
import net.patchedpixel.scenecraft.gui.function.KeyInput;
import net.patchedpixel.scenecraft.gui.versions.ScreenExt;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
    private static final String ON_KEY_PRESSED = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V";

    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = ON_KEY_PRESSED), cancellable = true)
    private void beforeKeyBindingTick(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        KeyInput keyInput = new KeyInput(key, scancode, modifiers);
        if (KeyEventCallback.EVENT.invoker().onKeyEvent(keyInput, action)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = ON_KEY_PRESSED, shift = At.Shift.AFTER))
    private void afterKeyBindingTick(CallbackInfo ci) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
    }

    @ModifyExpressionValue(
            method = "keyPress",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", ordinal = 0),
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=keyPressed event handler"))
    )
    private Screen doesScreenPassEvents(Screen screen) {
        if (screen instanceof ScreenExt ext && ext.doesPassEvents()) {
            screen = null;
        }
        return screen;
    }
}
