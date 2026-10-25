package net.patchedpixel.scenecraft.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class MixinParticleManager {
    @Unique
    private Quaternionf sceneCraftRender_orgRotation;

    @Inject(method = "render", at = @At("HEAD"))
    private void faceCameraAtParticle(VertexConsumer consumer, Camera camera, float partialTicks, CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).sceneCraftRender_getHandler();
        if (handler == null || !handler.omnidirectional) {
            return;
        }

        Quaternionf rotation = camera.rotation();
        this.sceneCraftRender_orgRotation = new Quaternionf(rotation);

        Vec3 from = new Vec3(0, 0, -1);
        Vec3 to = MCVer.getPosition((SingleQuadParticle) (Object) this, partialTicks)
                .subtract(camera.getPosition())
                .normalize();
        Vec3 axis = from.cross(to);
        rotation.set((float) axis.x, (float) axis.y, (float) axis.z, (float) (1 + from.dot(to)));
        rotation.normalize();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void faceCameraAtParticleCleanup(VertexConsumer consumer, Camera camera, float partialTicks, CallbackInfo ci) {
        Quaternionf orgRotation = this.sceneCraftRender_orgRotation;
        if (orgRotation == null) {
            return;
        }
        this.sceneCraftRender_orgRotation = null;
        camera.rotation().set(orgRotation.x, orgRotation.y, orgRotation.z, orgRotation.w);
    }
}
