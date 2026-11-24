package net.patchedpixel.scenecraft.common.mixin.replay.world_border;

import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

@Mixin(targets = "net.minecraft.world.level.border.WorldBorder$MovingBorderExtent")
public class Mixin_UseReplayTime_ForMovement {
    @Unique private static long scenecraft$replayNowOrReal() {
        ReplayHandler handler = SceneCraftReplay.instance.getReplayHandler();
        return handler != null ? handler.getReplaySender().currentTimeStamp() : MCVer.milliTime();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    private long scenecraft$beginTime() {
        long packetTime = SceneCraftReplay.pendingBorderReplayTime;
        return packetTime >= 0L ? packetTime : scenecraft$replayNowOrReal();
    }

    @Redirect(method = { "getSize", "getLerpRemainingTime" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    private long scenecraft$currentTime() {
        return scenecraft$replayNowOrReal();
    }
}
