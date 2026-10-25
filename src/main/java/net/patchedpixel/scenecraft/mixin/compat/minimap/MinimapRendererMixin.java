package net.patchedpixel.scenecraft.mixin.compat.minimap;

import net.patchedpixel.scenecraft.compat.SceneCraftCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.module.MinimapRenderer", remap = false)
public abstract class MinimapRendererMixin {
    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void scenecraft$hideMinimap(CallbackInfo ci) {
        if (SceneCraftCompat.shouldHide()) {
            ci.cancel();
        }
    }
}
