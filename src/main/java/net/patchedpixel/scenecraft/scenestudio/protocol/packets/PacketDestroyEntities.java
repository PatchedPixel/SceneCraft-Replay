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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PacketDestroyEntities {
    public static List<Integer> getEntityIds(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            int len = in.readVarInt();
            List<Integer> result = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                result.add(in.readVarInt());
            }
            return result;
        }
    }

    public static Collection<Packet> write(PacketTypeRegistry registry, int...entityIds) throws IOException {
        return Collections.singletonList(writeDestroyEntities(registry, entityIds));
    }

    public static Packet write(PacketTypeRegistry registry, int entityId) throws IOException {
        return writeDestroyEntities(registry, entityId);
    }

    private static Packet writeDestroyEntities(PacketTypeRegistry registry, int...entityIds) throws IOException {
        Packet packet = new Packet(registry, PacketType.DestroyEntities);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeVarInt(entityIds.length);

            for (int entityId : entityIds) {
                out.writeVarInt(entityId);
            }
        }
        return packet;
    }
}
