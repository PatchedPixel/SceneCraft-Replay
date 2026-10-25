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

package net.patchedpixel.scenecraft.scenestudio.rar.containers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.patchedpixel.scenecraft.scenestudio.netio.NetInput;
import net.patchedpixel.scenecraft.scenestudio.netio.NetOutput;
import net.patchedpixel.scenecraft.scenestudio.netio.ByteBufNetOutput;
import net.patchedpixel.scenecraft.scenestudio.protocol.Packet;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketType;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;
import net.patchedpixel.scenecraft.scenestudio.protocol.packets.PacketJoinGame;
import net.patchedpixel.scenecraft.scenestudio.rar.PacketSink;
import net.patchedpixel.scenecraft.scenestudio.rar.cache.ReadableCache;
import net.patchedpixel.scenecraft.scenestudio.rar.cache.WriteableCache;
import net.patchedpixel.scenecraft.scenestudio.rar.state.Replay;
import net.patchedpixel.scenecraft.scenestudio.rar.state.World;
import net.patchedpixel.scenecraft.scenestudio.util.IPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorldStateTree extends StateTree<World> {
    private final PacketTypeRegistry registry;
    private final Replay replay;
    private ReadableCache cache;
    private World activeWorld;

    public WorldStateTree(PacketTypeRegistry registry, Replay replay, int index) {
        super(index);
        this.registry = registry;
        this.replay = replay;
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        this.cache = cache;
        super.load(sink, cache);
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        if (activeWorld != null) {
            activeWorld.unload(sink, cache);
            activeWorld = null;
        }
        this.cache = null;
        super.unload(sink, cache);
    }

    @Override
    protected World read(NetInput in) throws IOException {
        return new World(registry, in);
    }

    @Override
    protected void discard(World value) {
    }

    private void ensureActiveWorld(World world) throws IOException {
        if (world == activeWorld) {
            return; // our cache is up-to-date, nothing to do
        }

        // Slow path, this should almost never be required for normal operation
        // unless the client state is completely reset.

        if (activeWorld != null) {
            activeWorld.unload(Packet::release, cache);
            activeWorld = null;
        }

        if (world != null) {
            world.load(Packet::release, cache);
            activeWorld = world;
        }
    }

    // If the world does not change between current and target time, simply returns the world.
    // Otherwise unloads the old one and switches to the new one (already playing it to the target time).
    private World getWorldOrSwitch(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        Map.Entry<Integer, World> previousEntry = map.floorEntry(currentTimeStamp);
        Map.Entry<Integer, World> targetEntry = map.floorEntry(targetTime);
        World previousWorld = previousEntry != null ? previousEntry.getValue() : null;
        World targetWorld = targetEntry != null ? targetEntry.getValue() : null;
        if (previousWorld == null && targetWorld == null) {
            return null;
        }

        ensureActiveWorld(previousWorld);

        if (previousWorld == targetWorld) {
            return targetWorld;
        } else {
            if (previousWorld != null) {
                previousWorld.unload(sink, cache);
            }

            activeWorld = targetWorld;

            if (targetWorld != null) {
                List<Packet> featuresChanged;
                // Need to check if any features need changing between the previous world and the current one.
                // If so, we definitely need to go through the config phase.
                featuresChanged = new ArrayList<>();
                replay.features.playOrRewind(featuresChanged::add, previousEntry != null ? previousEntry.getKey() : -1, targetEntry.getKey());

                if (previousWorld == null || !previousWorld.info.equals(targetWorld.info) || !featuresChanged.isEmpty()) {
                    boolean isRespawnSufficient = previousWorld != null && previousWorld.info.isRespawnSufficient(targetWorld.info);

                    boolean registriesChanged = previousWorld == null || !previousWorld.info.registries.equals(targetWorld.info.registries);
                    if (registriesChanged || !featuresChanged.isEmpty()) {
                        isRespawnSufficient = false;
                    }

                    if (!isRespawnSufficient) {
                        PacketJoinGame joinGame = targetWorld.info.toPacketJoinGame();

                        sink.accept(joinGame.write(registry));

                        featuresChanged.forEach(sink);

                        if (registriesChanged) {
                            replay.tags.play(sink, -1, targetTime);
                        }
                    } else {
                        sink.accept(targetWorld.info.toRespawnPacket().write(registry));
                    }

                    // This packet is required to close the "Loading Terrain" screen
                    Packet packet;
                    packet = new Packet(registry, PacketType.SpawnPosition);
                    try (Packet.Writer out = packet.overwrite()) {
                        out.writePosition(new IPosition(0, 0, 0));
                        out.writeFloat(0);
                    }

                    sink.accept(packet);
                }
                targetWorld.load(sink, cache);
                targetWorld.play(sink, -1, targetTime);
            }
            return null;
        }
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        World world = getWorldOrSwitch(sink, currentTimeStamp, targetTime);
        if (world != null) {
            world.play(sink, currentTimeStamp, targetTime);
        }
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        World world = getWorldOrSwitch(sink, currentTimeStamp, targetTime);
        if (world != null) {
            world.rewind(sink, currentTimeStamp, targetTime);
        }
    }

    public static class Builder {
        private final PacketTypeRegistry registry;
        private final WriteableCache cache;
        private final TreeBuilder builder = new TreeBuilder();
        public World.Builder world;

        public Builder(PacketTypeRegistry registry, WriteableCache cache) throws IOException {
            this.registry = registry;
            this.cache = cache;
        }

        public World.Builder newWorld(int time, World.Info info) throws IOException {
            World.Builder builder = new World.Builder(registry, cache, info);
            this.builder.put(time, world = builder);
            return builder;
        }

        public int build(int time) throws IOException {
            builder.endTime = time;
            return builder.build(cache);
        }

        private static class TreeBuilder extends StateTree.Builder<World.Builder> {
            private int endTime;

            @Override
            protected void write(NetOutput out, World.Builder world, int time) throws IOException {
                Integer nextTime = map.higherKey(time);
                world.build(out, nextTime != null ? nextTime : endTime);
            }

            @Override
            protected void discard(World.Builder world) {
                ByteBuf buf = Unpooled.buffer();
                try {
                    world.build(new ByteBufNetOutput(buf), 0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                buf.release();
            }
        }
    }
}
