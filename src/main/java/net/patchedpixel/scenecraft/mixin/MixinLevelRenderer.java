package net.patchedpixel.scenecraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.patchedpixel.scenecraft.core.events.PostRenderWorldCallback;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.gui.utils.color.ReadableColor;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private GameRenderer gameRenderer;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"))
    private void postRenderWorld(CallbackInfo ci) {
        PoseStack matrixStack = new PoseStack();
        PostRenderWorldCallback.EVENT.invoker().postRenderWorld(matrixStack, this.submitNodeStorage);
    }

    @WrapOperation(
            method = "addSkyPass",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V")
    )
    private void chromaKeyingSky(FramePass pass, Runnable task, Operation<Void> original) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).sceneCraftRender_getHandler();
        if(handler != null) {
            ReadableColor color = handler.getSettings().getChromaKeyingColor();
            if(color != null) {
                original.call(pass, (Runnable) () -> RenderSystem.getDevice().createCommandEncoder().clearColorTexture(
                        MCVer.getMinecraft().gameRenderer.mainRenderTarget().getColorTexture(),
                        new org.joml.Vector4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f)
                ));
                return;
            }
        }
        original.call(pass, task);
    }

    @Inject(method = "submitHitOutline", at = @At("HEAD"), cancellable = true)
    private void sceneCraftRender_drawSelectionBox(CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) this.gameRenderer).sceneCraftRender_getHandler();
        if(handler != null) {
            ci.cancel();
        }
    }
}
