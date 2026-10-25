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
import net.patchedpixel.scenecraft.scenestudio.util.Location;

import java.io.IOException;

public class PacketEntityTeleport {
    public static Location getLocation(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            in.readVarInt(); // entity id
            double x = in.readDouble();
            double y = in.readDouble();
            double z = in.readDouble();
            float yaw = in.readByte() / 256f * 360;
            float pitch = in.readByte() / 256f * 360;
            return new Location(x, y, z, yaw, pitch);
        }
    }

    public static Packet write(PacketTypeRegistry registry, int entityId, Location location, boolean onGround) throws IOException {
        Packet packet = new Packet(registry, PacketType.EntityTeleport);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeVarInt(entityId);
            out.writeDouble(location.getX());
            out.writeDouble(location.getY());
            out.writeDouble(location.getZ());
            out.writeByte((int) (location.getYaw() / 360 * 256));
            out.writeByte((int) (location.getPitch() / 360 * 256));
            out.writeBoolean(onGround);
        }
        return packet;
    }
}
