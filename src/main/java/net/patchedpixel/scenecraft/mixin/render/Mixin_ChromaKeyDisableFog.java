package net.patchedpixel.scenecraft.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class Mixin_ChromaKeyDisableFog {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void sceneCraftRender_suppressFogIfChromaKeyIsActive(Camera camera, FogRenderer.FogMode fogMode, float renderDistance, boolean thickFog, float partialTick, CallbackInfo ci) {
        Minecraft mc = MCVer.getMinecraft();
        if (mc == null) return;
        GameRenderer gameRenderer = mc.gameRenderer;
        if (gameRenderer == null) return;
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) gameRenderer).sceneCraftRender_getHandler();
        if (handler != null && handler.getSettings().getChromaKeyingColor() != null) {
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        }
    }
}
