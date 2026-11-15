package net.patchedpixel.scenecraft.common.mixin.replay;

import net.patchedpixel.scenecraft.common.replay.camera.CameraEntity;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.*;

@Mixin(SpectatorGui.class)
public abstract class MixinGuiSpectator {
    @Inject(method = "onMouseScrolled", at = @At("HEAD"), cancellable = true)
    public void isInReplay(
            int i,
            CallbackInfo ci
    ) {
        // Prevent spectator gui from opening while in a replay
        if(getMinecraft().player instanceof CameraEntity) {
            ci.cancel();
        }
    }
}
