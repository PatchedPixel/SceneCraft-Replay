package net.patchedpixel.scenecraft.mixin.replay;

import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderStateShard.class)
public class MixinRenderItem {
    @Redirect(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    private static long getEnchantmentTime() {
        ReplayHandler replayHandler = SceneCraftReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return Util.getMillis();
    }
}
