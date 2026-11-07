package net.patchedpixel.scenecraft.common.mixin.replay;

import net.patchedpixel.scenecraft.common.replay.camera.CameraEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.*;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinRenderLivingBase {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void sceneCraftReplay_canRenderInvisibleName(
            LivingEntity entity,
            double distSquared,
            CallbackInfoReturnable<Boolean> ci
    ) {
        Player thePlayer = getMinecraft().player;
        if(thePlayer instanceof CameraEntity && entity.isInvisible()) {
            ci.setReturnValue(false);
        }
    }

    @Redirect(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z"
            )
    )
    private boolean sceneCraftReplay_shouldInvisibleNotBeRendered(LivingEntity entity, Player thePlayer) {
        return thePlayer instanceof CameraEntity || entity.isInvisibleTo(thePlayer);
    }
}
