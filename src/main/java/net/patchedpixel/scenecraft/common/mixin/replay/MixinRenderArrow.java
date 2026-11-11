package net.patchedpixel.scenecraft.common.mixin.replay;

import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

import net.minecraft.client.renderer.culling.Frustum;

@Mixin(TippableArrowRenderer.class)
public abstract class MixinRenderArrow extends EntityRenderer {
    protected MixinRenderArrow(Context context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean shouldRender(Entity entity, Frustum camera, double camX, double camY, double camZ) {
        // Force arrows to always render, otherwise they stop rendering when you get close to them
        return SceneCraftReplay.instance.getReplayHandler() != null || super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
