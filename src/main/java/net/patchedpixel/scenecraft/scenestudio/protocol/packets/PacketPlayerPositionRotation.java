/*
 * Copyright (c) 2024
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

import java.io.IOException;

public class PacketPlayerPositionRotation {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public int flags;
    public int teleportId;

    public static PacketPlayerPositionRotation read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            PacketPlayerPositionRotation result = new PacketPlayerPositionRotation();
            result.read(packet, in);
            return result;
        }
    }

    private void read(Packet packet, Packet.Reader in) throws IOException {
        x = in.readDouble();
        y = in.readDouble();
        z = in.readDouble();
        yaw = in.readFloat();
        pitch = in.readFloat();
        flags = in.readUnsignedByte();
        teleportId = in.readVarInt();
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.PlayerPositionRotation);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeDouble(x);
            out.writeDouble(y);
            out.writeDouble(z);
            out.writeFloat(yaw);
            out.writeFloat(pitch);
            out.writeByte(flags);
            out.writeVarInt(teleportId);
        }
        return packet;
    }
}
