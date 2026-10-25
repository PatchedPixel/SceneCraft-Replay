package net.patchedpixel.scenecraft.mixin;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.record.SceneCraftRecorder;
import net.patchedpixel.scenecraft.record.handler.RecordingEventHandler;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.FullReplaySender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    // The stupid name is required as otherwise Mixin treats it as a shadow, seemingly ignoring the lack of @Shadow
    private static Minecraft mcStatic = MCVer.getMinecraft();

    @Shadow
    private Map<UUID, PlayerInfo> playerInfoMap;

    @Inject(method = "handlePlayerInfoUpdate", at=@At("HEAD"))
    public void recordOwnJoin(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        if (!mcStatic.isSameThread()) return;
        if (mcStatic.player == null) return;

        RecordingEventHandler handler = SceneCraftRecorder.instance.getConnectionEventHandler().getRecordingEventHandler();
        if (handler != null && packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
            for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
                UUID uuid = entry.profileId();
                if (uuid.equals(mcStatic.player.getGameProfile().getId()) && !this.playerInfoMap.containsKey(uuid)) {
                    handler.spawnRecordingPlayer();
                }
            }
        }
    }

    @Inject(method = "handleRespawn", at=@At("RETURN"))
    public void recordOwnRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        RecordingEventHandler handler = SceneCraftRecorder.instance.getConnectionEventHandler().getRecordingEventHandler();
        if (handler != null) {
            handler.spawnRecordingPlayer();
        }
    }

    @ModifyVariable(method = { "handleTeleportEntity", "handleMoveEntity", "handleSetEntityPassengersPacket" }, at = @At("RETURN"), ordinal = 0)
    private Entity updatePositionIfNotTracked$0(Entity entity) {
        if (entity != null) {
            entity.getSelfAndPassengers().forEach(this::updatePositionIfNotTracked);
        }
        return entity;
    }

    private void updatePositionIfNotTracked(Entity entity) {
        if (entity != null && entity.level() instanceof ClientLevel world) {
            if (!world.tickingEntities.contains(entity)) {

                int ticks = 0;
                Vec3 prevPos;
                do {
                    prevPos = entity.position();
                    if (entity.isPassenger()) {
                        entity.rideTick();
                    } else {
                        entity.tick();
                    }
                } while (prevPos.distanceToSqr(entity.position()) > 0.0001 && ticks++ < 100);
            }
        }
    }

    @Inject(method = "handleSetBorderLerpSize", at = @At("HEAD"))
    private void scenecraft$lerpStart(ClientboundSetBorderLerpSizePacket packet, CallbackInfo ci) {
        SceneCraftReplay.pending = FullReplaySender.borderPacketTime(packet);
    }

    @Inject(method = "handleSetBorderLerpSize", at = @At("RETURN"))
    private void scenecraft$lerpEnd(ClientboundSetBorderLerpSizePacket packet, CallbackInfo ci) {
        SceneCraftReplay.pending = -1L;
        FullReplaySender.forgetBorderPacket(packet);
    }

    @Inject(method = "handleInitializeBorder", at = @At("HEAD"))
    private void scenecraft$initStart(ClientboundInitializeBorderPacket packet, CallbackInfo ci) {
        SceneCraftReplay.pending = FullReplaySender.borderPacketTime(packet);
    }

    @Inject(method = "handleInitializeBorder", at = @At("RETURN"))
    private void scenecraft$initEnd(ClientboundInitializeBorderPacket packet, CallbackInfo ci) {
        SceneCraftReplay.pending = -1L;
        FullReplaySender.forgetBorderPacket(packet);
    }
}
