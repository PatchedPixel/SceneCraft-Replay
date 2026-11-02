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
import net.patchedpixel.scenecraft.scenestudio.protocol.data.VersionedIdentifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

public class Registries {
    public Map<String, List<Pair<String, Tag>>> registriesMap;
    public List<VersionedIdentifier> enabledPacks;

    public Registries() {
    }

    public Registries copy() {
        Registries copy = new Registries();
        copy.registriesMap = this.registriesMap != null ? new HashMap<>(this.registriesMap) : null;
        copy.enabledPacks = this.enabledPacks != null ? new ArrayList<>(this.enabledPacks) : null;
        return copy;
    }

    public Entry getEntry(String registryName, String entryName) {
        if(registriesMap == null) {
            return null;
        }
        List<Pair<String, Tag>> registry = registriesMap.get(registryName);
        if(registry == null) return null;
        int id = 0;
        for(Pair<String, Tag> entry : registry) {
            if(entry.getKey().equals(entryName)) {
                return new Entry(id, entry.getKey(), entry.getValue());
            }
            id++;
        }
        return null;
    }

    public Entry getEntry(String registryName, int entryId) {
        if(registriesMap == null) {
            return null;
        }
        List<Pair<String, Tag>> registry = registriesMap.get(registryName);
        if(registry == null) return null;
        if(entryId < 0 || entryId >= registry.size()) return null;
        Pair<String, Tag> entry = registry.get(entryId);
        return new Entry(entryId, entry.getKey(), entry.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Registries that = (Registries) o;
        return Objects.equals(registriesMap, that.registriesMap)
                && Objects.equals(enabledPacks, that.enabledPacks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registriesMap, enabledPacks);
    }

    public void writeInternal(PacketTypeRegistry registry, NetOutput out) throws IOException {
        Map<String, List<Pair<String, Tag>>> registriesMap = this.registriesMap;
        if(registriesMap == null) {
            registriesMap = Collections.emptyMap();
        }
        List<VersionedIdentifier> enabledPacks = this.enabledPacks;
        if(enabledPacks == null) {
            enabledPacks = Collections.emptyList();
        }
        Packet.Writer.writeList(registry, out, new ArrayList<>(registriesMap.entrySet()), registryEntry -> {
            out.writeString(registryEntry.getKey());
            Packet.Writer.writeList(registry, out, registryEntry.getValue(), entry -> {
                out.writeString(entry.getKey());
                Tag value = entry.getValue();
                if(value != null) {
                    out.writeBoolean(true);
                    Packet.Writer.writeNBT(registry, out, value);
                } else {
                    out.writeBoolean(false);
                }
            });
        });
        Packet.Writer.writeList(registry, out, enabledPacks, entry -> entry.write(out));
    }

    public static Registries readInternal(PacketTypeRegistry registry, NetInput in) throws IOException {
        Registries registries = new Registries();
        registries.registriesMap = new HashMap<>();
        Packet.Reader.readList(registry, in, () -> {
            String registryName = in.readString();
            List<Pair<String, Tag>> registryEntries = Packet.Reader.readList(registry, in, () -> {
                String key = in.readString();
                Tag value = in.readBoolean() ? Packet.Reader.readNBT(registry, in) : null;
                return Pair.of(key, value);
            });
            registries.registriesMap.put(registryName, registryEntries);
            return null;
        });
        registries.enabledPacks = Packet.Reader.readList(registry, in, () -> VersionedIdentifier.read(in));
        return registries;
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
            if(value instanceof CompoundTag) {
                return (CompoundTag) value;
            } else {
                return new CompoundTag();
            }
        }
    }
}
