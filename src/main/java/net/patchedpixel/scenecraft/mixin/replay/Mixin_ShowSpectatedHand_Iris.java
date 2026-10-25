package net.patchedpixel.scenecraft.mixin.replay;

import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.patchedpixel.scenecraft.core.versions.MCVer.getMinecraft;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer", remap = false)
public abstract class Mixin_ShowSpectatedHand_Iris {
    @Redirect(
            method = "canRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;m_105295_()Lnet/minecraft/world/level/GameType;",
                    remap = true
            ),
            require = 0
    )
    private GameType getGameMode(MultiPlayerGameMode interactionManager) {
        LocalPlayer camera = getMinecraft().player;
        if (camera instanceof CameraEntity) {
            // alternative doesn't really matter, the caller only checks for equality to SPECTATOR
            return camera.isSpectator() ? GameType.SPECTATOR : GameType.SURVIVAL;
        }

        return interactionManager.getPlayerMode();
    }
}
