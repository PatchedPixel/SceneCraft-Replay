package net.patchedpixel.scenecraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.patchedpixel.scenecraft.common.render.RenderSettings;
import net.patchedpixel.scenecraft.common.render.capturer.CubicGpuFrameCapturer;
import net.patchedpixel.scenecraft.common.render.capturer.StereoscopicGpuFrameCapturer;
import net.patchedpixel.scenecraft.common.render.hooks.EntityRendererHandler;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.getMinecraft;
import static org.joml.Math.PI_OVER_2_f;
import static org.joml.Math.PI_f;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow private @Nullable Entity entity;
    @Shadow @Final private static Vector3fc FORWARDS;
    @Shadow @Final private static Vector3fc UP;
    @Shadow @Final private static Vector3fc LEFT;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private int matrixPropertiesDirty;

    @Shadow
    protected abstract void move(float forwards, float up, float right);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = At.Shift.AFTER))
    private void sceneCraftRender_setupCubicFrameRotation(CallbackInfo ci) {
        if(getHandler() == null || !(getHandler().data instanceof CubicGpuFrameCapturer.Data)) return;
        CubicGpuFrameCapturer.Data data = (CubicGpuFrameCapturer.Data) getHandler().data;

        switch(data) {
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

        FORWARDS.rotate(this.rotation, this.forwards);
        UP.rotate(this.rotation, this.up);
        LEFT.rotate(this.rotation, this.left);
        this.matrixPropertiesDirty |= 3;
    }

    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void sceneCraftRender_setupStereoscopicProjection(CallbackInfo ci) {
        if(getHandler() == null || !(getHandler().data instanceof StereoscopicGpuFrameCapturer.Data)) return;
        StereoscopicGpuFrameCapturer.Data data = (StereoscopicGpuFrameCapturer.Data) getHandler().data;

        move(0f, 0f, 0.1f * (data == StereoscopicGpuFrameCapturer.Data.LEFT_EYE ? -1 : 1));
    }

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void sceneCraftRender_stabilizeCamera(Camera instance, float yRot, float xRot, Operation<Void> original) {
        if(getHandler() != null) {
            RenderSettings settings = getHandler().getSettings();

            if(settings.isStabilizeYaw()) {
                yRot = 0f;
            }

            if(settings.isStabilizePitch()) {
                xRot = 0f;
            }
        }

        original.call(instance, yRot, xRot);
    }

    @Unique
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).sceneCraftRender_getHandler();
    }
}
