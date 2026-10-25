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
import net.patchedpixel.scenecraft.scenestudio.protocol.registry.DimensionType;
import net.patchedpixel.scenecraft.scenestudio.protocol.registry.Registries;
import net.patchedpixel.scenecraft.scenestudio.util.IGlobalPosition;

import java.io.IOException;

public class PacketRespawn {
    public byte gameMode;
    public byte prevGameMode;
    public DimensionType dimensionType;
    public String dimension;
    public long seed;
    public int difficulty;
    public boolean debugWorld;
    public boolean flatWorld;
    public boolean keepPlayerAttributes;
    public boolean keepPlayerDataTracker;
    public IGlobalPosition lastDeathPosition;
    public int portalCooldown;

    public static PacketRespawn read(Packet packet, Registries registries) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            PacketRespawn respawn = new PacketRespawn();
            respawn.read(packet, in, registries);
            return respawn;
        }
    }

    public void read(Packet packet, Packet.Reader in, Registries registries) throws IOException {
        this.dimensionType = DimensionType.fromRegistry(registries, in.readString());
        this.dimension = in.readString();
        this.seed = in.readLong();
        this.gameMode = in.readByte();
        this.prevGameMode = in.readByte();
        this.debugWorld = in.readBoolean();
        this.flatWorld = in.readBoolean();

        int flags = in.readByte();
        this.keepPlayerAttributes = (flags & 0x01) != 0;
        this.keepPlayerDataTracker = (flags & 0x02) != 0;

        if (in.readBoolean()) {
            this.lastDeathPosition = in.readGlobalPosition();
        } else {
            this.lastDeathPosition = null;
        }

        this.portalCooldown = in.readVarInt();
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.Respawn);
        try (Packet.Writer out = packet.overwrite()) {
            write(packet, out);
        }
        return packet;
    }

    public void write(Packet packet, Packet.Writer out) throws IOException {
        out.writeString(this.dimensionType.getName());
        out.writeString(this.dimension);
        out.writeLong(this.seed);
        out.writeByte(this.gameMode);
        out.writeByte(this.prevGameMode);
        out.writeBoolean(this.debugWorld);
        out.writeBoolean(this.flatWorld);

        int flags = 0;
        if (this.keepPlayerAttributes) flags |= 0x01;
        if (this.keepPlayerDataTracker) flags |= 0x02;
        out.writeByte(flags);

        if (this.lastDeathPosition != null) {
            out.writeBoolean(true);
            out.writeGlobalPosition(this.lastDeathPosition);
        } else {
            out.writeBoolean(false);
        }

        out.writeVarInt(this.portalCooldown);
    }
}
