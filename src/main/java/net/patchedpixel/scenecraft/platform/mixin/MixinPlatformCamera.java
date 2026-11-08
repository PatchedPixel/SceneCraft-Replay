package net.patchedpixel.scenecraft.platform.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.patchedpixel.scenecraft.common.render.hooks.EntityRendererHandler;
import net.patchedpixel.scenecraft.common.replay.camera.CameraEntity;
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

@Mixin(Camera.class)
public abstract class MixinPlatformCamera {
    @Shadow private @Nullable Entity entity;
    @Shadow @Final private static Vector3fc FORWARDS;
    @Shadow @Final private static Vector3fc UP;
    @Shadow @Final private static Vector3fc LEFT;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private int matrixPropertiesDirty;

    // Camera#setRotation(float, float, float) with the roll parameter only exists because NeoForge patches it in.
    @Inject(method = "setRotation(FFF)V", at = @At("TAIL"))
    private void sceneCraftReplay_applyRoll(float yRot, float xRot, float roll, CallbackInfo ci) {
        Entity entity = this.entity;
        if(!(entity instanceof CameraEntity)) {
            return;
        }

        EntityRendererHandler handler = getHandler();
        if(handler != null && handler.getSettings().isStabilizeRoll()) {
            return;
        }

        this.rotation.rotateZ(-((CameraEntity) entity).roll * (float) Math.PI / 180f);

        FORWARDS.rotate(this.rotation, this.forwards);
        UP.rotate(this.rotation, this.up);
        LEFT.rotate(this.rotation, this.left);
        this.matrixPropertiesDirty |= 3;
    }

    @Unique
    private EntityRendererHandler getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).sceneCraftRender_getHandler();
    }
}
