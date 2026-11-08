package net.patchedpixel.scenecraft.platform.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.patchedpixel.scenecraft.common.gui.versions.callbacks.PostRenderScreenCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinPlatformGui {
    private static final String EXTRACT_GUI = "extractRenderState";

    // NeoForge wraps Screen#extractRenderStateWithTooltipAndSubtitles in this hook (screen layers and
    // ScreenEvent.Render.Pre/Post), so the vanilla call is no longer present in Gui#extractRenderState.
    // The descriptor is left out on purpose so this keeps working if the layer parameter type changes.
    private static final String RENDER = "Lnet/neoforged/neoforge/client/ClientHooks;extractScreen(Lnet/minecraft/client/gui/screens/Screen;Ljava/util/Stack;Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V";

    @Unique
    private GuiGraphicsExtractor context;

    // The graphics extractor is the third argument: (screen, screenLayers, graphics, mouseX, mouseY, partialTick)
    @ModifyArg(method = EXTRACT_GUI, at = @At(value = "INVOKE", target = RENDER), index = 2)
    private GuiGraphicsExtractor captureContext(GuiGraphicsExtractor context) {
        this.context = context;
        return context;
    }

    @Inject(method = EXTRACT_GUI, at = @At(value = "INVOKE", target = RENDER, shift = At.Shift.AFTER))
    private void postRenderScreen(DeltaTracker tickCounter, boolean renderWorld, boolean resourcesLoaded, CallbackInfo ci) {
        float partialTicks = tickCounter.getGameTimeDeltaPartialTick(true);
        PostRenderScreenCallback.EVENT.invoker().postRenderScreen(context, partialTicks);
    }
}
