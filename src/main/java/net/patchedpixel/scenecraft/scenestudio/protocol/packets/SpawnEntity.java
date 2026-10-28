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
import net.patchedpixel.scenecraft.scenestudio.util.Location;

import java.io.IOException;

public class SpawnEntity {
    public static Location getLocation(Packet packet) throws IOException {
        if (packet.getType() == PacketType.SpawnObject) {
            try (Packet.Reader in = packet.reader()) {
                in.readVarInt(); // id
                in.readUUID(); // uuid
                in.readVarInt(); // type
                double x = in.readDouble();
                double y = in.readDouble();
                double z = in.readDouble();
                // Pitch is stored before yaw for entities
                float pitch = in.readByte() / 256f * 360;
                float yaw = in.readByte() / 256f * 360;
                return new Location(x, y, z, yaw, pitch);
            }
        } else if (packet.getType() == PacketType.SpawnPlayer) {
            try (Packet.Reader in = packet.reader()) {
                in.readVarInt(); // id
                in.readUUID(); // uuid
                double x = in.readDouble();
                double y = in.readDouble();
                double z = in.readDouble();
                float yaw = in.readByte() / 256f * 360;
                float pitch = in.readByte() / 256f * 360;
                return new Location(x, y, z, yaw, pitch);
            }
        } else if (packet.getType() == PacketType.SpawnExpOrb) {
            try (Packet.Reader in = packet.reader()) {
                in.readVarInt(); // id
                double x = in.readDouble();
                double y = in.readDouble();
                double z = in.readDouble();
                return new Location(x, y, z, 0, 0);
            }
        }
        return null;
    }
}
