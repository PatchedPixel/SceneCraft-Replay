package net.patchedpixel.scenecraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.patchedpixel.scenecraft.render.RenderSettings;
import net.patchedpixel.scenecraft.render.capturer.CubicGpuFrameCapturer;
import net.patchedpixel.scenecraft.render.capturer.StereoscopicGpuFrameCapturer;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.patchedpixel.scenecraft.core.versions.MCVer.getMinecraft;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Unique
    private static final float PI_f = (float) Math.PI;

    @Unique
    private static final float PI_OVER_2_f = (float) (Math.PI / 2);

    @Shadow private Entity entity;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;

    @Shadow
    protected abstract void move(double forwards, double up, double right);

    @Inject(method = "setup", at = @At("RETURN"))
    private void sceneCraftRender_setupCubicFrameRotation(CallbackInfo ci) {
        if (getHandler() == null || !(getHandler().data instanceof CubicGpuFrameCapturer.Data)) return;
        CubicGpuFrameCapturer.Data data = (CubicGpuFrameCapturer.Data) getHandler().data;
        switch (data) {
            case FRONT:
                break;
            case RIGHT:
                this.rotation.rotateY(-PI_OVER_2_f);
                break;
            case BACK:
                this.rotation.rotateY(PI_f);
                break;
            case LEFT:
                this.rotation.rotateY(PI_OVER_2_f);
                break;
            case TOP:
                this.rotation.rotateX(PI_OVER_2_f);
                break;
            case BOTTOM:
                this.rotation.rotateX(-PI_OVER_2_f);
                break;
        }
        this.forwards.set(0.0F, 0.0F, 1.0F).rotate(this.rotation);
        this.up.set(0.0F, 1.0F, 0.0F).rotate(this.rotation);
        this.left.set(1.0F, 0.0F, 0.0F).rotate(this.rotation);
    }

    @Inject(method = "setup", at = @At("RETURN"))
    private void sceneCraftRender_setupStereoscopicProjection(CallbackInfo ci) {
        if (getHandler() == null || !(getHandler().data instanceof StereoscopicGpuFrameCapturer.Data)) return;
        StereoscopicGpuFrameCapturer.Data data = (StereoscopicGpuFrameCapturer.Data) getHandler().data;
        move(0f, 0f, 0.1f * (data == StereoscopicGpuFrameCapturer.Data.LEFT_EYE ? -1 : 1));
    }

    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0))
    private void sceneCraftRender_stabilizeCamera(Camera instance, float yRot, float xRot, Operation<Void> original) {
        if (getHandler() != null) {
            RenderSettings settings = getHandler().getSettings();
            if (settings.isStabilizeYaw()) {
                yRot = 0f;
            }
            if (settings.isStabilizePitch()) {
                xRot = 0f;
            }
        }
        original.call(instance, yRot, xRot);
    }

    @Inject(method = "setRotation(FF)V", at = @At("TAIL"))
    private void sceneCraftReplay_applyRoll(float yRot, float xRot, CallbackInfo ci) {
        Entity entity = this.entity;
        if (!(entity instanceof CameraEntity)) return;
        EntityRendererHandler handler = getHandler();
        if (handler != null && handler.getSettings().isStabilizeRoll()) return;
        this.rotation.rotateZ(-((CameraEntity) entity).roll * (float) Math.PI / 180f);
        this.forwards.set(0.0F, 0.0F, 1.0F).rotate(this.rotation);
        this.up.set(0.0F, 1.0F, 0.0F).rotate(this.rotation);
        this.left.set(1.0F, 0.0F, 0.0F).rotate(this.rotation);
    }

    @Unique
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).sceneCraftRender_getHandler();
    }
}
