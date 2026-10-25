package net.patchedpixel.scenecraft.record.handler;

import net.patchedpixel.scenecraft.core.events.PreRenderCallback;
import net.patchedpixel.scenecraft.record.packet.PacketListener;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.versions.callbacks.PreTickCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.patchedpixel.scenecraft.core.versions.MCVer.*;

public class RecordingEventHandler extends EventRegistrations {
    private final Minecraft mc = getMinecraft();
    private final PacketListener packetListener;

    // Sending a position update every frame causes the interpolation code to never reach its target
    // so we'll record one only every other tick (this matches what the vanilla server does for player entities)
    private static final int POS_UPDATE_FREQUENCY = 2;
    private int ticksSinceLastPosUpdate;

    private static final int TRACKED_POSITION_RESOLUTION = 4096;
    private Double lastX, lastY, lastZ;

    private static final int EQUIPMENT_SLOTS = EquipmentSlot.values().length;
    private final List<ItemStack> playerItems = NonNullList.withSize(EQUIPMENT_SLOTS, ItemStack.EMPTY);
    private int ticksSinceLastCorrection;
    private boolean wasSleeping;
    private int lastRiding = -1;
    private Integer rotationYawHeadBefore;

    public RecordingEventHandler(PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    public void onPacket(Packet<?> packet) {
        packetListener.save(packet);
    }

    public void spawnRecordingPlayer() {
        try {
            LocalPlayer player = mc.player;
            assert player != null;
            packetListener.save(new ClientboundAddEntityPacket(
                    player.getId(),
                    player.getUUID(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getXRot(),
                    player.getYRot(),
                    player.getType(),
                    0,
                    player.getDeltaMovement(),
                    player.getYHeadRot()
            ));
            packetListener.save(new ClientboundSetEntityDataPacket(player.getId(), player.getEntityData().getNonDefaultValues()));
            lastX = lastY = lastZ = null;
            playerItems.clear();
            lastRiding = -1;
            wasSleeping = false;
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void onClientEffect(int type, BlockPos pos, int data) {
        try {
            // Send to all other players in ServerWorldEventHandler#playEvent
            packetListener.save(new ClientboundLevelEventPacket(type, pos, data, false));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    { on(PreTickCallback.EVENT, this::onPlayerTick); }
    private void onPlayerTick() {
        if (mc.player == null) return;
        LocalPlayer player = mc.player;
        try {
            boolean force = false;
            if(lastX == null || lastY == null || lastZ == null) {
                force = true;
                lastX = player.getX();
                lastY = player.getY();
                lastZ = player.getZ();
            }

            ticksSinceLastCorrection++;
            if(ticksSinceLastCorrection >= 100) {
                ticksSinceLastCorrection = 0;
                force = true;
            }

            long dx = Math.round((player.getX() - lastX) * TRACKED_POSITION_RESOLUTION);
            long dy = Math.round((player.getY() - lastY) * TRACKED_POSITION_RESOLUTION);
            long dz = Math.round((player.getZ() - lastZ) * TRACKED_POSITION_RESOLUTION);

            final double maxRelDist = 8.0 * TRACKED_POSITION_RESOLUTION;

            Packet packet = null;
            if (force || Math.abs(dx) > maxRelDist || Math.abs(dy) > maxRelDist || Math.abs(dz) > maxRelDist) {
                packet = new ClientboundTeleportEntityPacket(player);
                lastX = player.getX();
                lastY = player.getY();
                lastZ = player.getZ();
            } else if (++ticksSinceLastPosUpdate >= POS_UPDATE_FREQUENCY) {
                ticksSinceLastPosUpdate = 0;
                byte newYaw = (byte) ((int) (player.getYRot() * 256.0F / 360.0F));
                byte newPitch = (byte) ((int) (player.getXRot() * 256.0F / 360.0F));

                packet = new ClientboundMoveEntityPacket.PosRot(
                        player.getId(),
                        (short) dx, (short) dy, (short) dz,
                        newYaw, newPitch,
                        player.onGround()
                );

                lastX += (double) dx / TRACKED_POSITION_RESOLUTION;
                lastY += (double) dy / TRACKED_POSITION_RESOLUTION;
                lastZ += (double) dz / TRACKED_POSITION_RESOLUTION;
            }

            if (packet != null) {
                packetListener.save(packet);
            }

            //HEAD POS
            int rotationYawHead = ((int)(player.yHeadRot * 256.0F / 360.0F));

            if(!Objects.equals(rotationYawHead, rotationYawHeadBefore)) {
                packetListener.save(new ClientboundRotateHeadPacket(player, (byte) rotationYawHead));
                rotationYawHeadBefore = rotationYawHead;
            }

            packetListener.save(new ClientboundSetEntityMotionPacket(player.getId(),
                    player.getDeltaMovement()
            ));

            //Animation Packets
            //Swing Animation
            if (player.swinging && player.swingTime == 0) {
                packetListener.save(new ClientboundAnimatePacket(player, player.swingingArm == InteractionHand.MAIN_HAND ? 0 : 3));
            }

			/*
        //Potion Effect Handling
		List<Integer> found = new ArrayList<Integer>();
		for(PotionEffect pe : (Collection<PotionEffect>)player.getActivePotionEffects()) {
			found.add(pe.getPotionID());
			if(lastEffects.contains(found)) continue;
			S1DPacketEntityEffect pee = new S1DPacketEntityEffect(entityID, pe);
			packetListener.save(pee);
		}

		for(int id : lastEffects) {
			if(!found.contains(id)) {
				S1EPacketRemoveEntityEffect pre = new S1EPacketRemoveEntityEffect(entityID, new PotionEffect(id, 0));
				packetListener.save(pre);
			}
		}

		lastEffects = found;
			 */

            //Inventory Handling
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                int index = slot.ordinal();
                if (!ItemStack.matches(playerItems.get(index), stack)) {
                    // ItemStack has internal mutability, so we need to make a copy now if we want to compare its
                    // current state with future states (e.g. dropping on modern versions will set the count to zero).
                    stack = stack != null ? stack.copy() : null;
                    playerItems.set(index, stack);
                    packetListener.save(new ClientboundSetEquipmentPacket(player.getId(), Collections.singletonList(Pair.of(slot, stack))));
                }
            }

            //Leaving Ride

            Entity vehicle = player.getVehicle();
            int vehicleId = vehicle == null ? -1 : vehicle.getId();
            if (lastRiding != vehicleId) {
                lastRiding = vehicleId;
                packetListener.save(new ClientboundSetEntityLinkPacket(
                        player,
                        vehicle
                ));
            }

            //Sleeping
            if(!player.isSleeping() && wasSleeping) {
                packetListener.save(new ClientboundAnimatePacket(player, 2));
                wasSleeping = false;
            }

        } catch(Exception e1) {
            e1.printStackTrace();
        }
    }

    public void onBlockBreakAnim(int breakerId, BlockPos pos, int progress) {
        Player thePlayer = mc.player;
        if (thePlayer != null && breakerId == thePlayer.getId()) {
            packetListener.save(new ClientboundBlockDestructionPacket(breakerId,
                    pos,
                    progress));
        }
    }

    { on(PreRenderCallback.EVENT, this::checkForGamePaused); }
    private void checkForGamePaused() {
        if (mc.hasSingleplayerServer()) {
            IntegratedServer server =  mc.getSingleplayerServer();
            if (server != null && server.paused) {
                packetListener.setServerWasPaused();
            }
        }
    }
}
