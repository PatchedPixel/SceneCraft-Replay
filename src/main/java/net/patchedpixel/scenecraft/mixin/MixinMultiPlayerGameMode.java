package net.patchedpixel.scenecraft.mixin;

import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.stats.StatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @Shadow
    private Minecraft minecraft;

    @Shadow
    private ClientPacketListener connection;

    @Inject(method = "getPlayerMode", at=@At("HEAD"), cancellable = true)
    private void sceneCraftReplay_getPlayerMode(CallbackInfoReturnable<GameType> ci) {
        if (this.minecraft.player instanceof CameraEntity) {
            ci.setReturnValue(this.minecraft.player.isSpectator() ? GameType.SPECTATOR : GameType.SURVIVAL);
        }
    }

    @Inject(method = "isAlwaysFlying", at = @At("HEAD"), cancellable = true)
    private void sceneCraftReplay_isAlwaysFlying(CallbackInfoReturnable<Boolean> ci) {
        if(this.minecraft.player instanceof CameraEntity) {
            ci.setReturnValue(this.minecraft.player.isSpectator());
        }
    }

    @Inject(method = "createPlayer(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/stats/StatsCounter;Lnet/minecraft/client/ClientRecipeBook;ZZ)Lnet/minecraft/client/player/LocalPlayer;", at=@At("HEAD"), cancellable = true)
    private void sceneCraftReplay_createReplayCamera(ClientLevel worldIn, StatsCounter statisticsManager, ClientRecipeBook recipeBookClient, boolean lastSneaking, boolean lastSprinting, CallbackInfoReturnable<LocalPlayer> ci) {
        if (SceneCraftReplay.instance.getReplayHandler() != null) {
            ci.setReturnValue(new CameraEntity(this.minecraft, worldIn, this.connection, statisticsManager, recipeBookClient));
            ci.cancel();
        }
    }
}
