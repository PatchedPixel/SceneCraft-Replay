package net.patchedpixel.scenecraft.common.mixin.replay;

import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

@Mixin(RenderStateShard.class)
public class MixinRenderItem {
    @Redirect(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    private static long getEnchantmentTime() {
        ReplayHandler replayHandler = SceneCraftReplay.instance.getReplayHandler();
        if(replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return MCVer.milliTime();
    }
}
