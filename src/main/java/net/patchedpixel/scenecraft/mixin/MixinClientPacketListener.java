package net.patchedpixel.scenecraft.mixin;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.record.SceneCraftRecorder;
import net.patchedpixel.scenecraft.record.handler.RecordingEventHandler;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.FullReplaySender;
import net.patchedpixel.scenecraft.replay.ext.EntityExt;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    // The stupid name is required as otherwise Mixin treats it as a shadow, seemingly ignoring the lack of @Shadow
    private static Minecraft mcStatic = MCVer.getMinecraft();

    private static final String LERP_TO = "Lnet/minecraft/world/entity/Entity;lerpTo(DDDFFIZ)V";

    @Shadow
    private Map<UUID, PlayerInfo> playerInfoMap;

    @Unique private Entity scenecraft$entity;

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
                if (entity.isControlledByLocalInstance()) {
                    return;
                }

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
        SceneCraftReplay.pendingBorderReplayTime = FullReplaySender.borderPacketTime(packet);
    }

    @Inject(method = "handleSetBorderLerpSize", at = @At("RETURN"))
    private void scenecraft$lerpEnd(ClientboundSetBorderLerpSizePacket packet, CallbackInfo ci) {
        SceneCraftReplay.pendingBorderReplayTime = -1L;
        FullReplaySender.forgetBorderPacket(packet);
    }

    @Inject(method = "handleInitializeBorder", at = @At("HEAD"))
    private void scenecraft$initStart(ClientboundInitializeBorderPacket packet, CallbackInfo ci) {
        SceneCraftReplay.pendingBorderReplayTime = FullReplaySender.borderPacketTime(packet);
    }

    @Inject(method = "handleInitializeBorder", at = @At("RETURN"))
    private void scenecraft$initEnd(ClientboundInitializeBorderPacket packet, CallbackInfo ci) {
        SceneCraftReplay.pendingBorderReplayTime = -1L;
        FullReplaySender.forgetBorderPacket(packet);
    }

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
    private float scenecraft$getTrackedYaw(Entity instance) {
        return ((EntityExt) instance).scenecraft$getTrackedYaw();
    }

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getXRot()F"))
    private float scenecraft$getTrackedPitch(Entity instance) {
        return ((EntityExt) instance).scenecraft$getTrackedPitch();
    }

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double scenecraft$getTrackedX(Entity instance) {
        return instance.isPassenger() ? instance.getX() : instance.getPositionCodec().decode(0L, 0L, 0L).x();
    }

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getY()D"))
    private double scenecraft$getTrackedY(Entity instance) {
        return instance.isPassenger() ? instance.getY() : instance.getPositionCodec().decode(0L, 0L, 0L).y();
    }

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double scenecraft$getTrackedZ(Entity instance) {
        return instance.isPassenger() ? instance.getZ() : instance.getPositionCodec().decode(0L, 0L, 0L).z();
    }

    @ModifyVariable(method = { "handleMoveEntity", "handleTeleportEntity" }, at = @At(value = "INVOKE", target = LERP_TO), ordinal = 0)
    private Entity scenecraft$captureEntity(Entity entity) {
        return this.scenecraft$entity = entity;
    }

    @Inject(method = { "handleMoveEntity", "handleTeleportEntity" }, at = @At("RETURN"))
    private void scenecraft$resetEntityField(CallbackInfo ci) {
        this.scenecraft$entity = null;
    }

    @ModifyArg(method = { "handleMoveEntity", "handleTeleportEntity" }, at = @At(value = "INVOKE", target = LERP_TO), index = 3)
    private float scenecraft$captureTrackedYaw(float value) {
        ((EntityExt) this.scenecraft$entity).scenecraft$setTrackedYaw(value);
        return value;
    }

    @ModifyArg(method = { "handleMoveEntity", "handleTeleportEntity" }, at = @At(value = "INVOKE", target = LERP_TO), index = 4)
    private float scenecraft$captureTrackedPitch(float value) {
        ((EntityExt) this.scenecraft$entity).scenecraft$setTrackedPitch(value);
        return value;
    }
}
