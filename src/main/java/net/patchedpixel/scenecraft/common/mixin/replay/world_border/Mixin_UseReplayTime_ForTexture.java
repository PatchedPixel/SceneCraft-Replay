package net.patchedpixel.scenecraft.common.mixin.replay.world_border;

import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.renderer.WorldBorderRenderer;

/**
 * Normally Minecraft's world border texture animation is based off real time;
 * this redirect ensures that it is synced with the time in the Replay instead.
 */
@Mixin(WorldBorderRenderer.class)
public class Mixin_UseReplayTime_ForTexture {
    @Redirect(method = "prepareDynamicTransforms", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMillis()J"))
    private long getWorldBorderTime() {
        ReplayHandler replayHandler = SceneCraftReplay.instance.getReplayHandler();
        if(replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }

        return MCVer.milliTime();
    }
}
