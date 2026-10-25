package net.patchedpixel.scenecraft.mixin.render;

import net.patchedpixel.scenecraft.render.capturer.IrisODSFrameCapturer;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public class Mixin_AddIrisOdsShaderUniforms {
    // Using ModifyVariable, so we only depend on a single argument. Hoping that reduces the chance of breaking changes.
    @ModifyVariable(method = "generalCommonUniforms", at = @At("HEAD"), argsOnly = true, require = 0)
    private static UniformHolder addSceneCraftOdsUniforms(UniformHolder uniforms) {
        IrisODSFrameCapturer ods = IrisODSFrameCapturer.INSTANCE;
        if (ods != null) {
            uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "leftEye", ods::isLeftEye);
            uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "direction", ods::getDirection);
        }
        return uniforms;
    }
}
