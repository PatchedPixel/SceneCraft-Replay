package net.patchedpixel.scenecraft.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.patchedpixel.scenecraft.gui.utils.color.ReadableColor;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void chromaKeyingSky(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).sceneCraftRender_getHandler();
        if (handler != null) {
            ReadableColor color = handler.getSettings().getChromaKeyingColor();
            if (color != null) {
                RenderSystem.clearColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f);
                RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void sceneCraftRender_drawSelectionBox(CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).sceneCraftRender_getHandler();
        if (handler != null) {
            ci.cancel();
        }
    }
}
