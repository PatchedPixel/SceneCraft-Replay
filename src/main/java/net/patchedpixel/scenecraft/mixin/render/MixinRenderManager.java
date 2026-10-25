package net.patchedpixel.scenecraft.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinRenderManager {
    @Shadow
    public Quaternionf cameraOrientation;

    @Unique
    private Quaternionf sceneCraftRender_orgOrientation;

    @Inject(method = "render", at = @At("HEAD"))
    private void sceneCraftRender_reorientForCubicRendering(Entity entity, double dx, double dy, double dz, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).sceneCraftRender_getHandler();
        if (handler == null || !handler.omnidirectional) {
            return;
        }
        this.sceneCraftRender_orgOrientation = this.cameraOrientation;
        this.cameraOrientation = new Quaternionf().lookAlong((float) dx, (float) dy, (float) dz, 0f, 1f, 0f);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void sceneCraftRender_restoreOrientation(Entity entity, double dx, double dy, double dz, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (this.sceneCraftRender_orgOrientation != null) {
            this.cameraOrientation = this.sceneCraftRender_orgOrientation;
            this.sceneCraftRender_orgOrientation = null;
        }
    }
}
