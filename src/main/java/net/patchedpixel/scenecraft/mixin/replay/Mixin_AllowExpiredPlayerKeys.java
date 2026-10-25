package net.patchedpixel.scenecraft.mixin.replay;

import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProfilePublicKey.Data.class)
public abstract class Mixin_AllowExpiredPlayerKeys {
    @Inject(method = { "hasExpired()Z", "hasExpired(Ljava/time/Duration;)Z" }, at = @At("HEAD"), cancellable = true)
    private void neverExpireWhenInReplay(CallbackInfoReturnable<Boolean> ci) {
        if (SceneCraftReplay.instance.getReplayHandler() != null) {
            ci.setReturnValue(false);
        }
    }
}
