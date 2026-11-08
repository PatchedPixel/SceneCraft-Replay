package net.patchedpixel.scenecraft.common.mixin.render;

import net.patchedpixel.scenecraft.common.render.capturer.IrisODSFrameCapturer;
import net.irisshaders.iris.Iris;
import net.patchedpixel.scenecraft.platform.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
public class Mixin_LoadIrisOdsShaderPack {
    @Redirect(method = "loadExternalShaderpack", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/Iris;getShaderpacksDirectory()Ljava/nio/file/Path;"))
    private static Path loadSceneCraftOdsPack(String name) {
        if(IrisODSFrameCapturer.INSTANCE != null && IrisODSFrameCapturer.SHADER_PACK_NAME.equals(name)) {
            // Root of our own mod file (contains the shaders/ directory of the ODS shader pack)
            return ModLoader.getOwnModRoot();
        } else {
            return Iris.getShaderpacksDirectory();
        }
    }
}
