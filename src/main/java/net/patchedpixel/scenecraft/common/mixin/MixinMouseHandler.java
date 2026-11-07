package net.patchedpixel.scenecraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.patchedpixel.scenecraft.common.core.events.KeyBindingEventCallback;
import net.patchedpixel.scenecraft.common.gui.function.Click;
import net.patchedpixel.scenecraft.common.gui.versions.callbacks.MouseCallback;
import net.patchedpixel.scenecraft.common.replay.InputReplayTimer;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {
    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V", shift = At.Shift.AFTER))
    private void afterKeyBindingTick(CallbackInfo ci) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
    }

    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    private boolean mouseDown(Screen screen, MouseButtonEvent click, boolean doubled, Operation<Boolean> original) {
        if(MouseCallback.EVENT.invoker().mouseDown(new Click(click))) {
            return true;
        }
        return original.call(screen, click, doubled);
    }

    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean mouseUp(Screen screen, MouseButtonEvent click, Operation<Boolean> original) {
        if(MouseCallback.EVENT.invoker().mouseUp(new Click(click))) {
            return true;
        }
        return original.call(screen, click);
    }

    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"))
    private boolean mouseDrag(Screen screen, MouseButtonEvent click, double dx, double dy, Operation<Boolean> original) {
        if(MouseCallback.EVENT.invoker().mouseDrag(new Click(click), dx, dy)) {
            return true;
        }
        return original.call(screen, click, dx, dy);
    }

    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean mouseScroll(Screen screen, double x, double y, double horizontal, double vertical, Operation<Boolean> original) {
        if(MouseCallback.EVENT.invoker().mouseScroll(x, y, horizontal, vertical)) {
            return true;
        }
        return original.call(screen, x, y, horizontal, vertical);
    }

    @Inject(method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    private void handleSceneCraftScroll(
            long _p0, double _p1, double _p2,
            CallbackInfo ci,
            boolean discreteScroll,
            double sensitivity,
            double xOffsetAccumulated,
            double yOffsetAccumulated
    ) {
        if(SceneCraftReplay.instance.getReplayHandler() != null) {
            InputReplayTimer.handleScroll((int) (yOffsetAccumulated * 120));
            ci.cancel();
        }
    }
}
