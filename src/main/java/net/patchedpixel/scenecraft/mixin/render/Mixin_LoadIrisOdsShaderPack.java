package net.patchedpixel.scenecraft.mixin.render;

import net.patchedpixel.scenecraft.render.capturer.IrisODSFrameCapturer;
import net.irisshaders.iris.Iris;
import java.net.URISyntaxException;
import java.net.URL;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
public class Mixin_LoadIrisOdsShaderPack {
    @Redirect(method = "loadExternalShaderpack", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/Iris;getShaderpacksDirectory()Ljava/nio/file/Path;"), require = 0)
    private static Path loadSceneCraftOdsPack(String name) {
        if (IrisODSFrameCapturer.INSTANCE != null && IrisODSFrameCapturer.SHADER_PACK_NAME.equals(name)) {
            // Root of our own mod file (contains the shaders/ directory of the ODS shader pack)
            return modRoot();
        } else {
            return Iris.getShaderpacksDirectory();
        }
    }

    /** Root of our own mod file (contains the iris/ods shader pack under assets/scenecraft/). */
    private static Path modRoot() {
        try {
            URL marker = Mixin_LoadIrisOdsShaderPack.class.getResource("/META-INF/mods.toml");
            if (marker == null) throw new IllegalStateException("mods.toml not found");
            return Path.of(marker.toURI()).getParent().getParent();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to locate SceneCraft mod root", e);
        }
    }
}
