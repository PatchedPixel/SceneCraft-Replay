package net.patchedpixel.scenecraft.common.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.SDLEventHandler;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.patchedpixel.scenecraft.common.core.events.PostRenderCallback;
import net.patchedpixel.scenecraft.common.core.events.PreRenderCallback;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.gui.versions.ScreenExt;
import net.patchedpixel.scenecraft.common.render.gui.progress.VirtualWindow;
import net.patchedpixel.scenecraft.common.render.hooks.WindowDelegateHolder;
import net.patchedpixel.scenecraft.common.replay.InputReplayTimer;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.PacketProcessor;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft extends ReentrantBlockableEventLoop<Runnable> implements MCVer.MinecraftMethodAccessor, WindowDelegateHolder {
    private static final String GAME_RENDERER_RENDER = "Lnet/minecraft/client/Minecraft;renderFrame(Z)V";

    @Shadow protected abstract void handleKeybinds();
    @Shadow @Final private SDLEventHandler sdlEventHandler;
    @Shadow @Final private PacketProcessor packetProcessor;

    @Unique
    private VirtualWindow windowDelegate;

    MixinMinecraft() { super(null, false); }

    @Override
    public void sceneCraftProcessKeyBinds() {
        handleKeybinds();
    }

    @Override
    public void sceneCraftPollEvents() {
        RenderSystem.pollEvents(this.sdlEventHandler);
    }

    @Override
    public void sceneCraftPumpEvents() {
        RenderSystem.pumpEvents(this.sdlEventHandler);
    }

    @Override
    public void sceneCraftExecuteTaskQueue() {
        this.packetProcessor.processQueuedPackets();
        runAllTasks();
    }

    @Override
    public void setWindowDelegate(VirtualWindow window) {
        this.windowDelegate = window;
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
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;screen()Lnet/minecraft/client/gui/screens/Screen;"),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=gui"),
                    to = @At(value = "CONSTANT", args = "stringValue=gameRenderer")
            )
    )
    private Screen doesScreenPassEvents(Screen screen) {
        if(screen instanceof ScreenExt ext && ext.doesPassEvents()) {
            return null;
        }

        return screen;
    }

    @Inject(method = "resizeGui", at = @At("HEAD"), cancellable = true)
    private void suppressResizeDuringRender(CallbackInfo ci) {
        VirtualWindow delegate = this.windowDelegate;
        if(delegate != null && delegate.isBound()) {
            Window window = ((Minecraft) (Object) this).getWindow();
            delegate.onResolutionChanged(window.getWidth(), window.getHeight());
            ci.cancel();
        }
    }

    @Inject(method = "handleGlobalKeyPress", at = @At("HEAD"), cancellable = true)
    private void scenecraft$noFriendsHotkeyInReplay(InputConstants.Key key, boolean controlDown, CallbackInfoReturnable<Boolean> cir) {
        SceneCraftReplay replay = SceneCraftReplay.instance;
        if(replay == null || replay.getReplayHandler() == null) {
            return;
        }
        Minecraft mc = (Minecraft) (Object) this;
        if(mc.options.keyFriends.matches(key)) {
            cir.setReturnValue(false);
        }
    }
}
