package net.patchedpixel.scenecraft.mixin.replay;

import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.world.entity.projectile.Arrow;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TippableArrowRenderer.class)
public abstract class MixinRenderArrow extends EntityRenderer<Arrow> {
    protected MixinRenderArrow(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(Arrow entity, Frustum camera, double camX, double camY, double camZ) {
        return SceneCraftReplay.instance.getReplayHandler() != null || super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
