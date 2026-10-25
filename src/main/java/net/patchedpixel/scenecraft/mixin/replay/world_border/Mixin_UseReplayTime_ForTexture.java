package net.patchedpixel.scenecraft.mixin.replay.world_border;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class Mixin_UseReplayTime_ForTexture {
    @Redirect(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    private long getWorldBorderTime() {
        ReplayHandler replayHandler = SceneCraftReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return MCVer.milliTime();
    }
}
