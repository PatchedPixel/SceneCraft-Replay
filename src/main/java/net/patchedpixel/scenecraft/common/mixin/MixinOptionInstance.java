package net.patchedpixel.scenecraft.common.mixin;

import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.extras.FullBrightness;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionInstance.class)
public class MixinOptionInstance {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void modifyGet(CallbackInfoReturnable<Object> cir) {
        if(FullBrightness.getGamma()) {
            Minecraft mc = MCVer.getMinecraft();
            OptionInstance<?> current = (OptionInstance<?>) (Object) this;
            if(current == mc.options.gamma()) {
                cir.setReturnValue(1000.0);
            }
        }
    }
}
