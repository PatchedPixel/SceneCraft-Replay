/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.patchedpixel.scenecraft.scenestudio.protocol.packets;

import net.patchedpixel.scenecraft.scenestudio.protocol.Packet;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketType;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;
import net.patchedpixel.scenecraft.scenestudio.util.DPosition;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;

public class PacketEntityMovement {
    /**
     * Reads a move_entity_* packet (26.x format).
     * <pre>
     * Pos:    varint net, varint properties (onGround | stepCount &lt;&lt; 1), VecDelta
     * PosRot: Pos + byte yaw + byte pitch
     * Rot:    varint net, boolean onGround, byte yaw, byte pitch
     * VecDelta: stepCount == 0: 3 shorts; otherwise stepCount * (varint ticks, 3 shorts)
     * </pre>
     * For stepped movement the returned delta is the sum of all steps (the final displacement).
     */
    public static Triple<DPosition, Pair<Float, Float>, Boolean> getMovement(Packet packet) throws IOException {
        PacketType type = packet.getType();
        boolean hasPos = type == PacketType.EntityPosition || type == PacketType.EntityPositionRotation;
        boolean hasRot = type == PacketType.EntityRotation || type == PacketType.EntityPositionRotation;
        try (Packet.Reader in = packet.reader()) {
            in.readVarInt(); // entity net

            boolean onGround;
            DPosition pos = null;
            if(hasPos) {
                int properties = in.readVarInt();
                onGround = (properties & 1) != 0;
                int steps = properties >>> 1;
                long dx = 0, dy = 0, dz = 0;
                if(steps <= 0) {
                    dx = in.readShort();
                    dy = in.readShort();
                    dz = in.readShort();
                } else {
                    for(int i = 0; i < steps; i++) {
                        in.readVarInt(); // ticks
                        dx += in.readShort();
                        dy += in.readShort();
                        dz += in.readShort();
                    }
                }
                pos = new DPosition(dx / 4096.0, dy / 4096.0, dz / 4096.0);
            } else {
                onGround = in.readBoolean();
            }
            Pair<Float, Float> yawPitch = null;
            if(hasRot) {
                yawPitch = Pair.of(
                        in.readByte() / 256f * 360,
                        in.readByte() / 256f * 360
                );
            }
            return Triple.of(pos, yawPitch, onGround);
        }
    }

    public static Packet write(PacketTypeRegistry registry, int entityId, DPosition deltaPos, Pair<Float, Float> yawPitch, boolean onGround) throws IOException {
        if(deltaPos == null && yawPitch == null) {
            // There is no "no movement" packet, a move by zero is the closest equivalent
            deltaPos = new DPosition(0, 0, 0);
        }
        boolean hasPos = deltaPos != null;
        boolean hasRot = yawPitch != null;
        PacketType type;
        if(hasPos) {
            type = hasRot ? PacketType.EntityPositionRotation : PacketType.EntityPosition;
        } else {
            type = PacketType.EntityRotation;
        }
        Packet packet = new Packet(registry, type);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeVarInt(entityId);

            if(hasPos) {
                out.writeVarInt(onGround ? 1 : 0); // properties: no steps
                out.writeShort((int) Math.round(deltaPos.getX() * 4096));
                out.writeShort((int) Math.round(deltaPos.getY() * 4096));
                out.writeShort((int) Math.round(deltaPos.getZ() * 4096));
            } else {
                out.writeBoolean(onGround);
            }
            if(hasRot) {
                out.writeByte((int) (yawPitch.getKey() / 360 * 256));
                out.writeByte((int) (yawPitch.getValue() / 360 * 256));
            }
        }
        return packet;
    }
}
