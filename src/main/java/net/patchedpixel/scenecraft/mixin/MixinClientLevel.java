package net.patchedpixel.scenecraft.mixin;

import net.patchedpixel.scenecraft.record.SceneCraftRecorder;
import net.patchedpixel.scenecraft.record.handler.RecordingEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel extends Level {
    @Shadow
    private Minecraft minecraft;

    @SuppressWarnings("ConstantConditions")
    protected MixinClientLevel() {
        super(null, null, null, null, null, false, false, 0, 0);
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"))
    public void sceneCraftRecording_recordClientSound(
            Player player,
            double x, double y, double z,
            Holder<SoundEvent> sound,
            SoundSource category,
            float volume, float pitch,
            long seed,
            CallbackInfo ci) {
        if (player == this.minecraft.player) {
            RecordingEventHandler handler = SceneCraftRecorder.instance.getConnectionEventHandler().getRecordingEventHandler();
            if (handler != null) {
                handler.onPacket(new ClientboundSoundPacket(
                        sound, category, x, y, z, volume, pitch
                        , seed
                ));
            }
        }
    }

    @Inject(method = "levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V", at = @At("HEAD"))
    private void playLevelEvent(
            Player player,
            int type, BlockPos pos, int data, CallbackInfo ci
    ) {
        if (player == this.minecraft.player) {
            RecordingEventHandler handler = SceneCraftRecorder.instance.getConnectionEventHandler().getRecordingEventHandler();
            if (handler != null) {
                handler.onClientEffect(type, pos, data);
            }
        }
    }

    @Inject(method = "destroyBlockProgress(ILnet/minecraft/core/BlockPos;I)V", at = @At("HEAD"))
    public void saveBlockBreakProgressPacket(int breakerId,
                                             BlockPos pos,
                                             int progress, CallbackInfo info) {
        RecordingEventHandler recordingEventHandler = SceneCraftRecorder.instance.getConnectionEventHandler().getRecordingEventHandler();
        if (recordingEventHandler != null) {
            recordingEventHandler.onBlockBreakAnim(breakerId,
                    pos,
                    progress);
        }
    }
}
