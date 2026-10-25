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

import net.minecraft.nbt.CompoundTag;
import net.patchedpixel.scenecraft.scenestudio.netio.NetInput;
import net.patchedpixel.scenecraft.scenestudio.netio.NetOutput;
import net.patchedpixel.scenecraft.scenestudio.netio.StreamNetInput;
import net.patchedpixel.scenecraft.scenestudio.netio.StreamNetOutput;
import net.patchedpixel.scenecraft.scenestudio.protocol.data.PaletteType;
import net.patchedpixel.scenecraft.scenestudio.protocol.Packet;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketType;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class PacketChunkData {
    private Column column;

    private boolean isUnload;
    private int unloadX;
    private int unloadZ;

    public static PacketChunkData read(Packet packet, int sections) throws IOException {
        PacketChunkData chunkData = new PacketChunkData();
        try (Packet.Reader reader = packet.reader()) {
            if (packet.getType() == PacketType.UnloadChunk) {
                chunkData.readUnload(packet, reader);
            } else {
                chunkData.readLoad(packet, reader, sections);
            }
        }
        return chunkData;
    }

    public static PacketChunkData readUnload(Packet packet) throws IOException {
        PacketChunkData chunkData = new PacketChunkData();
        try (Packet.Reader reader = packet.reader()) {
            chunkData.readUnload(packet, reader);
        }
        return chunkData;
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, isUnload ? PacketType.UnloadChunk : PacketType.ChunkData);
        try (Packet.Writer writer = packet.overwrite()) {
            if (isUnload) {
                writeUnload(packet, writer);
            } else {
                writeLoad(packet, writer);
            }
        }
        return packet;
    }

    public static PacketChunkData load(Column column) {
        PacketChunkData chunkData = new PacketChunkData();
        chunkData.column = column;
        return chunkData;
    }

    public static PacketChunkData unload(int chunkX, int chunkZ) {
        PacketChunkData chunkData = new PacketChunkData();
        chunkData.isUnload = true;

        chunkData.unloadX = chunkX;
        chunkData.unloadZ = chunkZ;
        return chunkData;
    }

    private PacketChunkData() {
    }

    public Column getColumn() {
        return column;
    }

    public boolean isUnload() {
        return isUnload;
    }

    public int getUnloadX() {
        return unloadX;
    }

    public int getUnloadZ() {
        return unloadZ;
    }

    private void readUnload(Packet packet, NetInput in) throws IOException {
        this.isUnload = true;
        this.unloadX = in.readInt();
        this.unloadZ = in.readInt();
    }

    private void writeUnload(Packet packet, Packet.Writer out) throws IOException {
        out.writeInt(this.unloadX);
        out.writeInt(this.unloadZ);
    }

    private void readLoad(Packet packet, Packet.Reader in, int sections) throws IOException {
        int x = in.readInt();
        int z = in.readInt();

        Heightmaps heightmaps = new Heightmaps(packet, in);

        byte[] data = in.readBytes(in.readVarInt());
        this.column = readColumn(packet, data, x, z, sections, heightmaps);

        TileEntity[] tileEntities = new TileEntity[in.readVarInt()];
        for (int i = 0; i < tileEntities.length; i++) {
            tileEntities[i] = new TileEntity(packet, in);
        }
        this.column.tileEntities = tileEntities;

        this.column.lightData = PacketUpdateLight.readData(packet, in);
    }

    private void writeLoad(Packet packet, Packet.Writer out) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        NetOutput netOut = new StreamNetOutput(byteOut);
        writeColumn(packet, netOut, this.column);

        out.writeInt(this.column.x);
        out.writeInt(this.column.z);

        this.column.heightmaps.write(packet, out);

        int len = byteOut.size();
        out.writeVarInt(len);
        out.writeBytes(byteOut.toByteArray(), len);
        out.writeVarInt(this.column.tileEntities.length);
        for (TileEntity tileEntity : this.column.tileEntities) {
            tileEntity.write(packet, out);
        }

        PacketUpdateLight.writeData(packet, out, this.column.lightData);
    }

    private static Column readColumn(Packet packet, byte[] data, int x, int z, int sections, Heightmaps heightmaps) throws IOException {
        NetInput in = new StreamNetInput(new ByteArrayInputStream(data));
        Chunk[] chunks = new Chunk[sections];
        for (int index = 0; index < chunks.length; index++) {
            chunks[index] = new Chunk(packet, in);
        }
        return new Column(x, z, chunks, null, null, heightmaps, null, true, null);
    }

    private static void writeColumn(Packet packet, NetOutput out, Column column) throws IOException {
        for (Chunk chunk : column.chunks) {
            if (chunk != null) {
                chunk.write(packet, out);
            }
        }
    }

    public static class Column {
        public int x;
        public int z;
        public Chunk[] chunks;
        public byte[] biomeData; // unused, kept for the squash filter
        public TileEntity[] tileEntities;
        public Heightmaps heightmaps;
        public int[] biomes; // unused, kept for the squash filter
        public boolean useExistingLightData; // unused, kept for the squash filter
        public PacketUpdateLight.Data lightData;

        public Column(int x, int z, Chunk[] chunks, byte[] biomeData, TileEntity[] tileEntities, Heightmaps heightmaps, int[] biomes, boolean useExistingLightData, PacketUpdateLight.Data lightData) {
            this.x = x;
            this.z = z;
            this.chunks = chunks;
            this.biomeData = biomeData;
            this.tileEntities = tileEntities;
            this.heightmaps = heightmaps;
            this.biomes = biomes;
            this.useExistingLightData = useExistingLightData;
            this.lightData = lightData;
        }

        public boolean isFull() {
            return this.biomeData != null || this.biomes != null || (this.lightData != null && this.tileEntities != null);
        }

        public BitSet getChunkMask() {
            BitSet mask = new BitSet();
            for (int index = 0; index < chunks.length; index++) {
                if (chunks[index] != null) {
                    mask.set(index);
                }
            }
            return mask;
        }

        public static long coordToLong(int x, int z) {
            return (long)x << 32 | (long)z & 0xFFFFFFFFL;
        }

        public static int longToX(long v) {
            return (int) (v >> 32);
        }

        public static int longToZ(long v) {
            return (int) (v & 0xFFFFFFFFL);
        }

        public long coordToLong() {
            return coordToLong(x, z);
        }
    }

    public static class Heightmaps {
        public CompoundTag tag;

        public Heightmaps(Packet packet, Packet.Reader in) throws IOException {
            tag = in.readNBTCompound();
        }

        public void write(Packet packet, Packet.Writer out) throws IOException {
            out.writeNBT(tag);
        }
    }

    public static class Chunk {
        private final int blockCount;
        public final PalettedStorage blocks;
        public final PalettedStorage biomes;

        private Chunk(Chunk from) {
            this.blockCount = from.blockCount;
            this.blocks = from.blocks != null ? from.blocks.copy() : null;
            this.biomes = from.biomes != null ? from.biomes.copy() : null;
        }

        public Chunk(PacketTypeRegistry registry) {
            this.blockCount = 0;
            this.blocks = new PalettedStorage(PaletteType.BLOCKS, registry);
            this.biomes = new PalettedStorage(PaletteType.BIOMES, registry);
        }

        Chunk(Packet packet, NetInput in) throws IOException {
            this.blockCount = in.readShort();
            this.blocks = new PalettedStorage(PaletteType.BLOCKS, packet, in);
            this.biomes = new PalettedStorage(PaletteType.BIOMES, packet, in);
        }

        void write(Packet packet, NetOutput out) throws IOException {
            out.writeShort(this.blockCount + this.blocks.countDelta);

            this.blocks.write(packet, out);
            this.biomes.write(packet, out);
        }

        public Chunk copy() {
            return new Chunk(this);
        }
    }

    public static class PalettedStorage {
        private final PaletteType type;
        private final PacketTypeRegistry registry;
        private int countDelta;
        private int bitsPerEntry;
        private List<Integer> states;
        private FlexibleStorage storage;

        private PalettedStorage(PalettedStorage from) {
            this.type = from.type;
            this.registry = from.registry;
            this.countDelta = from.countDelta;
            this.bitsPerEntry = from.bitsPerEntry;
            if (from.states != null) {
                this.states = new ArrayList<>(from.states);
            }
            if (from.storage != null) {
                this.storage = FlexibleStorage.from(registry, bitsPerEntry, from.storage.entries, from.storage.data.clone());
            }
        }

        public PalettedStorage(PaletteType type, PacketTypeRegistry registry) {
            this.type = type;
            this.registry = registry;
            this.bitsPerEntry = type == PaletteType.BLOCKS ? 4 : 0;

            this.states = new ArrayList<>();
            this.states.add(0);

            this.storage = FlexibleStorage.empty(registry, bitsPerEntry, type.size());
        }

        PalettedStorage(PaletteType type, Packet packet, NetInput in) throws IOException {
            this.type = type;
            this.registry = packet.getRegistry();
            this.bitsPerEntry = in.readUnsignedByte();
            this.states = new ArrayList<>();
            int stateCount;
            if (this.bitsPerEntry > type.highestBitsPerValue()) {
                stateCount = 0;
            } else if (this.bitsPerEntry == 0) {
                stateCount = 1;
            } else {
                stateCount = in.readVarInt();
            }
            for(int i = 0; i < stateCount; ++i) {
                this.states.add(in.readVarInt());
            }

            long[] data = in.readLongs(in.readVarInt());

            this.storage = FlexibleStorage.from(registry, bitsPerEntry, type.size(), data);
        }

        void write(Packet packet, NetOutput out) throws IOException {
            out.writeByte(this.bitsPerEntry);
            if (this.bitsPerEntry == 0) {
                out.writeVarInt(this.states.get(0));
            } else if (this.bitsPerEntry <= type.highestBitsPerValue()) {
                out.writeVarInt(this.states.size());
                for (Integer state : this.states) {
                    out.writeVarInt(state);
                }
            }

            out.writeVarInt(storage.data.length);
            out.writeLongs(storage.data);
        }

        private int index(int x, int y, int z) {
            if (this.type == PaletteType.BIOMES) {
                return y << 4 | z << 2 | x;
            } else {
                return y << 8 | z << 4 | x;
            }
        }

        public int get(int x, int y, int z) {
            if (this.bitsPerEntry == 0) {
                return this.states.get(0);
            }
            int id = this.storage.get(index(x, y, z));
            return this.bitsPerEntry <= type.highestBitsPerValue() ? (id >= 0 && id < this.states.size() ? this.states.get(id) : 0) : id;
        }

        public void set(int x, int y, int z, int state) {
            int id = this.bitsPerEntry <= type.highestBitsPerValue() ? this.states.indexOf(state) : state;
            if(id == -1) {
                this.states.add(state);
                if(this.states.size() > 1 << this.bitsPerEntry) {
                    this.bitsPerEntry++;

                    if(this.bitsPerEntry > type.highestBitsPerValue()) {
                        this.states.clear();
                        // These match the size of the vanilla global palette and may be incorrect when it comes to
                        // modded servers.
                        // Unfortunately there is no easy way to determine what the actual size of the global palette
                        // should be without having to parse the mod loader handshake. Which is annoying, so it has not
                        // yet been implemented.
                        this.bitsPerEntry = 15;

                        // Luckily, we currently only modify the block storage for internal tracking, and as long as
                        // we don't send it, the actual size we use doesn't really matter and we can just increase it
                        // if it turns out to have been too small.
                        // We do this here initially, and then also below immediately before `set` if the new id is OOB.
                        int bitsUsed = (1 << this.bitsPerEntry) - 1;
                        for (int i = 0; i < this.storage.entries; i++) {
                            bitsUsed |= this.storage.get(i);
                        }
                        this.bitsPerEntry = 32 - Integer.numberOfLeadingZeros(bitsUsed);
                    }

                    FlexibleStorage oldStorage = this.storage;
                    this.storage = FlexibleStorage.empty(this.registry, this.bitsPerEntry, this.storage.entries);
                    for(int index = 0; index < this.storage.entries; index++) {
                        this.storage.set(index, oldStorage.get(index));
                    }
                }

                id = this.bitsPerEntry <= type.highestBitsPerValue() ? this.states.indexOf(state) : state;
            }

            if (this.bitsPerEntry == 0) {
                return;
            }

            int ind = index(x, y, z);
            int curr = this.storage.get(ind);
            if(state != 0 && curr == 0) {
                countDelta++;
            } else if(state == 0 && curr != 0) {
                countDelta--;
            }

            if (this.bitsPerEntry > type.highestBitsPerValue() && id > this.storage.maxEntryValue) {
                // Workaround for us not knowing the size of the global palette. See the two comment blocks above.
                // Determine how many bits we need per entry to fit this id
                this.bitsPerEntry = 32 - Integer.numberOfLeadingZeros(id);
                // Convert old storage to new entry size
                FlexibleStorage oldStorage = this.storage;
                this.storage = FlexibleStorage.empty(this.registry, this.bitsPerEntry, this.storage.entries);
                for (int i = 0; i < this.storage.entries; i++) {
                    this.storage.set(i, oldStorage.get(i));
                }
            }
            this.storage.set(ind, id);
        }

        public PalettedStorage copy() {
            return new PalettedStorage(this);
        }
    }

    private static abstract class FlexibleStorage {
        protected final long[] data;
        protected final int bitsPerEntry;
        protected final int entries;
        protected final long maxEntryValue;

        protected FlexibleStorage(long[] data, int bitsPerEntry, int entries) {
            this.data = data;
            this.bitsPerEntry = bitsPerEntry;
            this.entries = entries;
            this.maxEntryValue = (1L << this.bitsPerEntry) - 1;
        }

        public abstract int get(int index);
        public abstract void set(int index, int value);

        static FlexibleStorage empty(PacketTypeRegistry registry, int bitsPerEntry, int entries) {
            return new PaddedFlexibleStorage(bitsPerEntry, entries);
        }

        static FlexibleStorage from(PacketTypeRegistry registry, int bitsPerEntry, int entries, long[] data) {
            return new PaddedFlexibleStorage(bitsPerEntry, entries, data);
        }
    }

    private static class PaddedFlexibleStorage extends FlexibleStorage {
        private final int entriesPerLong;

        public PaddedFlexibleStorage(int bitsPerEntry, int entries) {
            this(bitsPerEntry, entries, new long[longsForEntries(bitsPerEntry, entries)]);
        }

        public PaddedFlexibleStorage(int bitsPerEntry, int entries, long[] data) {
            super(data, bitsPerEntry, entries);
            this.entriesPerLong = bitsPerEntry == 0 ? 0 : 64 / bitsPerEntry;
        }

        private static int longsForEntries(int bitsPerEntry, int entries) {
            if (bitsPerEntry == 0) {
                return 0;
            }
            int entriesPerLong = 64 / bitsPerEntry;
            return (entries + entriesPerLong - 1) / entriesPerLong;
        }

        @Override
        public int get(int index) {
            if (index < 0 || index > this.entries - 1) {
                throw new IndexOutOfBoundsException();
            }
            if (this.bitsPerEntry == 0) {
                return 0;
            }

            int blockIndex = index / this.entriesPerLong;
            int subIndex = index % this.entriesPerLong;
            int subIndexBits = subIndex * this.bitsPerEntry;
            return (int) (this.data[blockIndex] >>> subIndexBits & this.maxEntryValue);
        }

        @Override
        public void set(int index, int value) {
            if (index < 0 || index > this.entries - 1) {
                throw new IndexOutOfBoundsException();
            }

            if (value < 0 || value > this.maxEntryValue) {
                throw new IllegalArgumentException("Value cannot be outside of accepted range.");
            }

            int blockIndex = index / this.entriesPerLong;
            int subIndex = index % this.entriesPerLong;
            int subIndexBits = subIndex * this.bitsPerEntry;
            this.data[blockIndex] = this.data[blockIndex] & ~(this.maxEntryValue << subIndexBits) | ((long) value & this.maxEntryValue) << subIndexBits;
        }
    }

    public static class TileEntity {
        public byte xz;
        public short y;
        public int type;
        public CompoundTag tag;

        TileEntity(Packet packet, Packet.Reader in) throws IOException {
            this.xz = in.readByte();
            this.y = in.readShort();
            this.type = in.readVarInt();

            this.tag = in.readNBTCompound();
        }

        void write(Packet packet, Packet.Writer out) throws IOException {
            out.writeByte(this.xz);
            out.writeShort(this.y);
            out.writeVarInt(this.type);

            out.writeNBT(this.tag);
        }
    }
}
