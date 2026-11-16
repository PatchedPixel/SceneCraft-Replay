package net.patchedpixel.scenecraft.common.mixin.replay;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.spectator.PlayerMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.world.entity.player.Player;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.replay.camera.CameraEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vanilla "teleport to player" spectator menu sends a packet to the server, which does not exist in a replay
 * (all outbound packets are dropped). In a replay, selecting a player from that menu teleports the free camera to that player instead.
 */
@Mixin(PlayerMenuItem.class)
public abstract class MixinPlayerMenuItem {
    @Shadow @Final private GameProfile profile;

    @Inject(method = "selectItem", at = @At("HEAD"), cancellable = true)
    private void sceneCraft$spectateInReplay(SpectatorMenu menu, CallbackInfo ci) {
        ReplayHandler replayHandler = SceneCraftReplay.instance.getReplayHandler();
        if(replayHandler == null) {
            return;
        }
        ci.cancel();
        Minecraft mc = MCVer.getMinecraft();
        if(mc.level == null) {
            return;
        }
        Player player = mc.level.getPlayerByUUID(this.profile.getId());
        if(player != null) {
            replayHandler.spectateCamera();
            CameraEntity camera = replayHandler.getCameraEntity();
            if(camera != null) {
                camera.setCameraPosRot(player);
            }
            menu.exit();
        }
    }
}
