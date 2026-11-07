package net.patchedpixel.scenecraft.common.mixin.render;

import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.render.hooks.EntityRendererHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FogRenderer.class)
public abstract class Mixin_ChromaKeyDisableFog {
    @ModifyVariable(method = "getBuffer", at = @At("HEAD"), argsOnly = true)
    private FogRenderer.FogMode sceneCraftRender_suppressFogIfChromaKeyIsActive(FogRenderer.FogMode fogType) {
        Minecraft mc = MCVer.getMinecraft();
        if(mc == null) return fogType;
        GameRenderer gameRenderer = mc.gameRenderer;
        if(gameRenderer == null) return fogType;
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) gameRenderer).sceneCraftRender_getHandler();
        if(handler != null && handler.getSettings().getChromaKeyingColor() != null) {
            fogType = FogRenderer.FogMode.NONE;
        }
        return fogType;
    }
}
