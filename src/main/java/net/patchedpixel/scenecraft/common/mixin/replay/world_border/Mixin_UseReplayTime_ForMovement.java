package net.patchedpixel.scenecraft.common.mixin.replay.world_border;

import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Normally Minecraft's world border movement is based off real time;
 * this redirect ensures that it is synced with the time in the Replay instead.
 */
@Mixin(targets = "net.minecraft.world.level.border.WorldBorder$MovingBorderExtent")
public class Mixin_UseReplayTime_ForMovement {
    @Shadow private long lerpProgress;
    @Shadow @Final private double lerpDuration;
    @Unique private long scenecraft$beginReplayMs = -1L;

    @Unique private static long scenecraft$replayNow() {
        ReplayHandler handler = SceneCraftReplay.instance.getReplayHandler();
        return handler != null ? handler.getReplaySender().currentTimeStamp() : -1L;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void scenecraft$captureStart(CallbackInfo ci) {
        long packetTime = SceneCraftReplay.pendingBorderReplayTime;
        scenecraft$beginReplayMs = packetTime >= 0L ? packetTime : scenecraft$replayNow();
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void scenecraft$syncToReplayTime(CallbackInfoReturnable<?> cir) {
        long now = scenecraft$replayNow();

        if(scenecraft$beginReplayMs < 0L || now < 0L) {
            return; // not in a replay
        }

        long elapsedTicks = Math.max(0L, (now - scenecraft$beginReplayMs) / 50L);
        this.lerpProgress = Math.max(0L, (long) this.lerpDuration - elapsedTicks) + 1L;
    }
}
