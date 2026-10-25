package net.patchedpixel.scenecraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.patchedpixel.scenecraft.core.events.PostRenderCallback;
import net.patchedpixel.scenecraft.core.events.PreRenderCallback;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.gui.versions.ScreenExt;
import net.patchedpixel.scenecraft.render.gui.progress.VirtualWindow;
import net.patchedpixel.scenecraft.render.hooks.FramebufferDelegateHolder;
import net.patchedpixel.scenecraft.render.hooks.WindowDelegateHolder;
import net.patchedpixel.scenecraft.replay.InputReplayTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft extends ReentrantBlockableEventLoop<Runnable> implements MCVer.MinecraftMethodAccessor, WindowDelegateHolder, FramebufferDelegateHolder {
    private static final String GAME_RENDERER_RENDER = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V";

    @Shadow protected abstract void handleKeybinds();

    @Unique
    private VirtualWindow windowDelegate;

    @Unique
    private RenderTarget framebufferDelegate;

    MixinMinecraft() { super(null); }

    @Override
    public void sceneCraftProcessKeyBinds() {
        handleKeybinds();
    }

    @Override
    public void sceneCraftExecuteTaskQueue() {
        runAllTasks();
    }

    @Override
    public void setWindowDelegate(VirtualWindow window) {
        this.windowDelegate = window;
    }

    @Override
    public void setFramebufferDelegate(RenderTarget framebuffer) {
        this.framebufferDelegate = framebuffer;
    }

    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void useGuiFramebuffer(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget delegate = this.framebufferDelegate;
        if (delegate != null) {
            cir.setReturnValue(delegate);
        }
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = GAME_RENDERER_RENDER))
    private void preRender(boolean unused, CallbackInfo ci) {
        PreRenderCallback.EVENT.invoker().preRender();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = GAME_RENDERER_RENDER, shift = At.Shift.AFTER))
    private void postRender(boolean unused, CallbackInfo ci) {
        PostRenderCallback.EVENT.invoker().postRender();
    }

    @Inject(method = "runTick", at = @At(value = "CONSTANT", args = "stringValue=scheduledExecutables"))
    private void updateInReplay(boolean unused, CallbackInfo ci) {
        InputReplayTimer.updateInReplay();
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", ordinal = 0),
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;overlay:Lnet/minecraft/client/gui/screens/Overlay;"))
    )
    private Screen doesScreenPassEvents(Screen screen) {
        if (screen instanceof ScreenExt ext && ext.doesPassEvents()) {
            return null;
        }

        return screen;
    }

    @Inject(method = "resizeDisplay", at = @At("HEAD"), cancellable = true)
    private void suppressResizeDuringRender(CallbackInfo ci) {
        VirtualWindow delegate = this.windowDelegate;
        if (delegate != null && delegate.isBound()) {
            Window window = ((Minecraft) (Object) this).getWindow();
            delegate.onResolutionChanged(window.getWidth(), window.getHeight());
            ci.cancel();
        }
    }
}
