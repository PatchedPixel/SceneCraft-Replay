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

package net.patchedpixel.scenecraft.scenestudio.rar.state;

import net.patchedpixel.scenecraft.scenestudio.netio.NetInput;
import net.patchedpixel.scenecraft.scenestudio.netio.NetOutput;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;
import net.patchedpixel.scenecraft.scenestudio.protocol.packets.PacketDestroyEntities;
import net.patchedpixel.scenecraft.scenestudio.protocol.packets.PacketEntityHeadLook;
import net.patchedpixel.scenecraft.scenestudio.protocol.packets.PacketEntityTeleport;
import net.patchedpixel.scenecraft.scenestudio.rar.PacketSink;
import net.patchedpixel.scenecraft.scenestudio.rar.RandomAccessState;
import net.patchedpixel.scenecraft.scenestudio.rar.cache.ReadableCache;
import net.patchedpixel.scenecraft.scenestudio.rar.cache.WriteableCache;
import net.patchedpixel.scenecraft.scenestudio.rar.containers.LocationStateTree;
import net.patchedpixel.scenecraft.scenestudio.util.Location;

import java.io.IOException;

public class Entity extends TransientThing implements RandomAccessState {
    private final LocationStateTree locations;

    public Entity(PacketTypeRegistry registry, NetInput in) throws IOException {
        super(registry, in);

        int id = in.readVarInt();
        this.locations = LocationStateTree.withApply(in.readVarInt(), (sink, loc) -> {
            sink.accept(PacketEntityTeleport.write(registry, id, loc, false));
            sink.accept(PacketEntityHeadLook.write(registry, id, loc.getYaw()));
        });
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        super.load(sink, cache);
        locations.load(sink, cache);
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        super.unload(sink, cache);
        locations.unload(sink, cache);
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        locations.play(sink, currentTimeStamp, targetTime);
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        locations.rewind(sink, currentTimeStamp, targetTime);
    }

    public static class Builder extends TransientThing.Builder {
        private final int entityId;
        private final LocationStateTree.Builder locations = new LocationStateTree.Builder();

        public Builder(PacketTypeRegistry registry, int entityId) throws IOException {
            this.entityId = entityId;

            addDespawnPacket(PacketDestroyEntities.write(registry, entityId));
        }

        public Location getLocation() {
            return locations.getLatest();
        }

        public void updateLocation(int time, Location loc) {
            locations.put(time, loc);
        }

        @Override
        public void build(NetOutput out, WriteableCache cache) throws IOException {
            super.build(out, cache);

            out.writeVarInt(entityId);
            out.writeVarInt(locations.build(cache));
        }
    }
}
