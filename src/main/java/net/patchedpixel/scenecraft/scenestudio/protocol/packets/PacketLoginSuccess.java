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
import net.patchedpixel.scenecraft.scenestudio.util.Property;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PacketLoginSuccess {
    private final UUID id;
    private final String name;
    private final List<Property> properties;

    public PacketLoginSuccess(UUID id, String name, List<Property> properties) {
        this.id = id;
        this.name = name;
        this.properties = properties;
    }

    public static PacketLoginSuccess read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            UUID id = in.readUUID();
            String name = in.readString();
            List<Property> properties = in.readList(() -> Property.read(in));
            return new PacketLoginSuccess(id, name, properties);
        }
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.LoginSuccess);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeUUID(id);
            out.writeString(name);
            out.writeList(properties, it -> it.write(out));
        }
        return packet;
    }
}
