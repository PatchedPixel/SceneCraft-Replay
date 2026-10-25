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

package net.patchedpixel.scenecraft.scenestudio.protocol.registry;

import net.patchedpixel.scenecraft.scenestudio.netio.NetInput;
import net.patchedpixel.scenecraft.scenestudio.netio.NetOutput;
import net.patchedpixel.scenecraft.scenestudio.protocol.Packet;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.util.Objects;

public class Registries {
    public CompoundTag codec;

    public Registries() {
    }

    public Registries(CompoundTag codec) {
        this.codec = codec;
    }

    public Registries copy() {
        return new Registries(this.codec != null ? this.codec.copy() : null);
    }

    private ListTag values(String registryName) {
        if (codec == null || !codec.contains(registryName, 10)) {
            return null;
        }
        return codec.getCompound(registryName).getList("value", 10);
    }

    public Entry getEntry(String registryName, String entryName) {
        ListTag values = values(registryName);
        if (values == null) {
            return null;
        }
        for (int i = 0; i < values.size(); i++) {
            CompoundTag entry = values.getCompound(i);
            if (entry.getString("name").equals(entryName)) {
                return new Entry(entry.getInt("id"), entryName, entry.get("element"));
            }
        }
        return null;
    }

    public Entry getEntry(String registryName, int entryId) {
        ListTag values = values(registryName);
        if (values == null) {
            return null;
        }
        for (int i = 0; i < values.size(); i++) {
            CompoundTag entry = values.getCompound(i);
            if (entry.getInt("id") == entryId) {
                return new Entry(entryId, entry.getString("name"), entry.get("element"));
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Registries that = (Registries) o;
        return Objects.equals(codec, that.codec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codec);
    }

    public void writeInternal(PacketTypeRegistry registry, NetOutput out) throws IOException {
        Packet.Writer.writeNBT(registry, out, codec);
    }

    public static Registries readInternal(PacketTypeRegistry registry, NetInput in) throws IOException {
        Tag tag = Packet.Reader.readNBT(registry, in);
        return new Registries(tag instanceof CompoundTag ? (CompoundTag) tag : null);
    }

    public static class Entry {
        public int id;
        public String name;
        public Tag value;

        public Entry(int id, String name, Tag value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }

        public CompoundTag asCompoundOrEmpty() {
            if (value instanceof CompoundTag) {
                return (CompoundTag) value;
            } else {
                return new CompoundTag();
            }
        }
    }
}
