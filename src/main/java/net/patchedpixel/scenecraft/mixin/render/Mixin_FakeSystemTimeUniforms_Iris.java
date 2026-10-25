package net.patchedpixel.scenecraft.mixin.render;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.SystemTimeUniforms$Timer", remap = false)
public class Mixin_FakeSystemTimeUniforms_Iris {
    @ModifyVariable(method = "beginFrame", at = @At("HEAD"), argsOnly = true, require = 0)
    private long useReplayTimeDuringRender(long frameStartTimeNs) {
        GameRenderer gameRenderer = MCVer.getMinecraft().gameRenderer;
        EntityRendererHandler entityRendererHandler =
            ((EntityRendererHandler.IEntityRenderer) gameRenderer).sceneCraftRender_getHandler();
        if (entityRendererHandler != null) {
            frameStartTimeNs = entityRendererHandler.getFakeFinishTimeNano();
        }
        return frameStartTimeNs;
    }
}
