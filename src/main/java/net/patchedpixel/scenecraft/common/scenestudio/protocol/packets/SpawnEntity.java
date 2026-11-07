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
package net.patchedpixel.scenecraft.common.scenestudio.protocol.packets;

import net.patchedpixel.scenecraft.common.scenestudio.protocol.Packet;
import net.patchedpixel.scenecraft.common.scenestudio.protocol.PacketType;
import net.patchedpixel.scenecraft.common.scenestudio.util.Location;

import java.io.IOException;

public class SpawnEntity {
    public static Location getLocation(Packet packet) throws IOException {
        if(packet.getType() != PacketType.SpawnObject) {
            return null;
        }
        try (Packet.Reader in = packet.reader()) {
            in.readVarInt(); // net
            in.readUUID(); // uuid
            in.readVarInt(); // type
            return readXYZYaPi(in);
        }
    }

    private static Location readXYZYaPi(Packet.Reader in) throws IOException {
        double x = in.readDouble();
        double y = in.readDouble();
        double z = in.readDouble();

        skipMovementVector(in);

        // Yaw and pitch are stored in this order for entities
        float pitch = in.readByte() / 256f * 360;
        float yaw = in.readByte() / 256f * 360;
        return new Location(x, y, z, yaw, pitch);
    }

    /**
     * Skips a low precision 3d vector (the velocity of the entity).
     * It takes 6 bytes, followed by a var int if the "has continuation" flag in the first byte is set.
     */
    private static void skipMovementVector(Packet.Reader in) throws IOException {
        int lowest = in.readUnsignedByte();
        if(lowest == 0) {
            return; // zero vector, nothing follows
        }
        in.readUnsignedByte();
        in.readInt();
        if((lowest & 4) == 4) {
            in.readVarInt();
        }
    }
}
