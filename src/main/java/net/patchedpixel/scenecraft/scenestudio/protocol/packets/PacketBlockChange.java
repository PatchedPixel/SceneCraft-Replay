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
import net.patchedpixel.scenecraft.scenestudio.util.IPosition;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PacketBlockChange {
    private IPosition pos;
    private int id;

    private PacketBlockChange() {}
    public PacketBlockChange(IPosition pos, int id) {
        this.pos = pos;
        this.id = id;
    }

    public static PacketBlockChange read(Packet packet) throws IOException {
        PacketBlockChange p = new PacketBlockChange();
        try (Packet.Reader in = packet.reader()) {
            p.pos = in.readPosition();
            p.id = in.readVarInt();
        }
        return p;
    }

    public static Packet write(PacketTypeRegistry registry, IPosition pos, int id) throws IOException {
        return new PacketBlockChange(pos, id).write(registry);
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.BlockChange);
        try (Packet.Writer out = packet.overwrite()) {
            out.writePosition(pos);
            out.writeVarInt(id);
        }
        return packet;
    }

    public static List<PacketBlockChange> readBulk(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            int chunkX;
            int chunkY;
            int chunkZ;
            long coord = in.readLong();
            chunkX = (int)(coord >> 42);
            chunkY = (int)(coord << 44 >> 44);
            chunkZ = (int)(coord << 22 >> 42);

            PacketBlockChange[] result;
            result = new PacketBlockChange[in.readVarInt()];

            for(int index = 0; index < result.length; index++) {
                PacketBlockChange p = new PacketBlockChange();

                long change = in.readVarLong();
                int x = (chunkX << 4) + (int) (change >> 8 & 15);
                int y = (chunkY << 4) + (int) (change & 15);
                int z = (chunkZ << 4) + (int) (change >> 4 & 15);
                p.pos = new IPosition(x, y, z);
                p.id = (int) (change >>> 12);

                result[index] = p;
            }
            return Arrays.asList(result);
        }
    }

    public static List<PacketBlockChange> readSingleOrBulk(Packet packet) throws IOException {
        if (packet.getType() == PacketType.BlockChange) {
            return Collections.singletonList(read(packet));
        } else {
            return readBulk(packet);
        }
    }

    public IPosition getPosition() {
        return pos;
    }

    public int getId() {
        return id;
    }
}
