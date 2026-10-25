package net.patchedpixel.scenecraft.mixin.replay.world_border;

import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.level.border.WorldBorder$MovingBorderExtent")
public class Mixin_UseReplayTime_ForMovement {
    private static long scenecraft$replayNowOrReal() {
        ReplayHandler handler = SceneCraftReplay.instance.getReplayHandler();
        return handler != null ? handler.getReplaySender().currentTimeStamp() : Util.getMillis();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    private long scenecraft$beginTime() {
        long packetTime = SceneCraftReplay.pending;
        return packetTime >= 0L ? packetTime : scenecraft$replayNowOrReal();
    }

    @Redirect(method = { "getSize", "getLerpRemainingTime" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    private long scenecraft$currentTime() {
        return scenecraft$replayNowOrReal();
    }
}
