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
import java.util.ArrayList;
import java.util.List;

public class PacketJoinGame {
    public int entityId;
    public boolean hardcore;
    public byte gameMode;
    public byte prevGameMode;
    public List<String> dimensions;
    public Registries registries;
    public DimensionType dimensionType;
    public String dimension;
    public long seed;
    public int difficulty;
    public int maxPlayers;
    public int viewDistance;
    public int simulationDistance;
    public boolean reducedDebugInfo;
    public boolean respawnScreen;
    public boolean debugWorld;
    public boolean flatWorld;
    public IGlobalPosition lastDeathPosition;
    public int portalCooldown;

    public PacketJoinGame() {
    }

    public PacketJoinGame(PacketJoinGame other) {
        this.entityId = other.entityId;
        this.hardcore = other.hardcore;
        this.gameMode = other.gameMode;
        this.prevGameMode = other.prevGameMode;
        this.dimensions = other.dimensions;
        this.registries = other.registries;
        this.dimensionType = other.dimensionType;
        this.dimension = other.dimension;
        this.seed = other.seed;
        this.difficulty = other.difficulty;
        this.maxPlayers = other.maxPlayers;
        this.viewDistance = other.viewDistance;
        this.simulationDistance = other.simulationDistance;
        this.reducedDebugInfo = other.reducedDebugInfo;
        this.respawnScreen = other.respawnScreen;
        this.debugWorld = other.debugWorld;
        this.flatWorld = other.flatWorld;
        this.lastDeathPosition = other.lastDeathPosition;
        this.portalCooldown = other.portalCooldown;
    }

    public static PacketJoinGame read(Packet packet, Registries registries) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            PacketJoinGame joinGame = new PacketJoinGame();
            joinGame.read(packet, in);
            return joinGame;
        }
    }

    public void read(Packet packet, Packet.Reader in) throws IOException {
        this.entityId = in.readInt();
        this.hardcore = in.readBoolean();
        this.gameMode = in.readByte();
        this.prevGameMode = in.readByte();

        int count = in.readVarInt();
        this.dimensions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.dimensions.add(in.readString());
        }

        this.registries = new Registries(in.readNBTCompound());

        this.dimensionType = DimensionType.fromRegistry(this.registries, in.readString());
        this.dimension = in.readString();
        this.seed = in.readLong();
        this.maxPlayers = in.readVarInt();
        this.viewDistance = in.readVarInt();
        this.simulationDistance = in.readVarInt();
        this.reducedDebugInfo = in.readBoolean();
        this.respawnScreen = in.readBoolean();
        this.debugWorld = in.readBoolean();
        this.flatWorld = in.readBoolean();

        if (in.readBoolean()) {
            this.lastDeathPosition = in.readGlobalPosition();
        } else {
            this.lastDeathPosition = null;
        }

        this.portalCooldown = in.readVarInt();
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.JoinGame);
        try (Packet.Writer out = packet.overwrite()) {
            write(packet, out);
        }

        return packet;
    }

    public void write(Packet packet, Packet.Writer out) throws IOException {
        out.writeInt(this.entityId);
        out.writeBoolean(this.hardcore);
        out.writeByte(this.gameMode);
        out.writeByte(this.prevGameMode);

        out.writeVarInt(this.dimensions.size());
        for (String dimension : this.dimensions) {
            out.writeString(dimension);
        }

        out.writeNBT(this.registries.codec);

        out.writeString(this.dimensionType.getName());
        out.writeString(this.dimension);
        out.writeLong(this.seed);
        out.writeVarInt(this.maxPlayers);
        out.writeVarInt(this.viewDistance);
        out.writeVarInt(this.simulationDistance);
        out.writeBoolean(this.reducedDebugInfo);
        out.writeBoolean(this.respawnScreen);
        out.writeBoolean(this.debugWorld);
        out.writeBoolean(this.flatWorld);

        if (this.lastDeathPosition != null) {
            out.writeBoolean(true);
            out.writeGlobalPosition(this.lastDeathPosition);
        } else {
            out.writeBoolean(false);
        }

        out.writeVarInt(this.portalCooldown);
    }
}
