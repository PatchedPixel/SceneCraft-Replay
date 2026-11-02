package net.patchedpixel.scenecraft.mixin;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.extras.SceneCraftExtras;
import net.patchedpixel.scenecraft.extras.playeroverview.PlayerOverview;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderer.class, priority = 1200)
public abstract class MixinEntityRenderer {
    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V", at = @At("HEAD"), cancellable = true)
    private void sceneCraftRender_areAllNamesHidden(CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).sceneCraftRender_getHandler();
        if(handler != null && !handler.getSettings().isRenderNameTags()) {
            ci.cancel();
        }
    }

    /**
     * Prevents players that are hidden in the PlayerOverview from being rendered.
     * Cancelling the render event alone is insufficient because it affects neither the shadows nor the fire texture.
     */
    @Inject(method = "shouldRender", at=@At("HEAD"), cancellable = true)
    public void sceneCraftExtras_isPlayerHidden(Entity entity, @Coerce Object camera, double camX, double camY, double camZ, float partialTicks, CallbackInfoReturnable<Boolean> ci) {
        SceneCraftExtras.instance.get(PlayerOverview.class).ifPresent(playerOverview -> {
            if(entity instanceof Player) {
                Player player = (Player) entity;
                if(playerOverview.isHidden(player.getUUID())) {
                    ci.setReturnValue(false);
                }
            }
        });
    }
}
