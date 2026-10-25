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
package net.patchedpixel.scenecraft.scenestudio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.patchedpixel.scenecraft.scenestudio.netio.NetInput;
import net.patchedpixel.scenecraft.scenestudio.netio.NetOutput;
import net.patchedpixel.scenecraft.scenestudio.netio.ByteBufNetInput;
import net.patchedpixel.scenecraft.scenestudio.netio.ByteBufNetOutput;
import net.patchedpixel.scenecraft.scenestudio.protocol.data.StringOrNbtText;
import net.patchedpixel.scenecraft.scenestudio.util.IGlobalPosition;
import net.patchedpixel.scenecraft.scenestudio.util.IOConsumer;
import net.patchedpixel.scenecraft.scenestudio.util.IOSupplier;
import net.patchedpixel.scenecraft.scenestudio.util.IPosition;
import net.patchedpixel.scenecraft.scenestudio.util.Nbt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

public class Packet {
    private final PacketTypeRegistry registry;
    private final int id;
    private final PacketType type;
    private final ByteBuf buf;

    public Packet(PacketTypeRegistry registry, PacketType type) {
        this(registry, type, Unpooled.buffer());
    }

    public Packet(PacketTypeRegistry registry, PacketType type, ByteBuf buf) {
        this(registry, registry.getId(type), type, buf);
    }

    public Packet(PacketTypeRegistry registry, int packetId, ByteBuf buf) {
        this(registry, packetId, registry.getType(packetId), buf);
    }

    public Packet(PacketTypeRegistry registry, int id, PacketType type, ByteBuf buf) {
        this.registry = registry;
        this.id = id;
        this.type = type;
        this.buf = buf;
    }

    public PacketTypeRegistry getRegistry() {
        return registry;
    }

    public int getProtocolVersion() {
        return registry.getProtocolVersion();
    }

    public int getId() {
        return id;
    }

    public PacketType getType() {
        return type;
    }

    public ByteBuf getBuf() {
        return buf;
    }

    public Packet retain() {
        buf.retain();
        return this;
    }

    public Packet copy() {
        return new Packet(registry, id, type, buf.retainedSlice());
    }

    public boolean release() {
        return buf.release();
    }

    public Reader reader() {
        return new Reader(this, buf);
    }

    public Writer overwrite() {
        buf.writerIndex(buf.readerIndex());
        return new Writer(this, buf);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Packet packet = (Packet) o;
        return id == packet.id &&
                registry.equals(packet.registry) &&
                buf.equals(packet.buf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registry, id, buf);
    }

    public static class Reader extends ByteBufNetInput implements AutoCloseable {
        private final Packet packet;
        private final ByteBuf buf;
        private int orgReaderIndex;

        Reader(Packet packet, ByteBuf buf) {
            super(buf);
            this.packet = packet;
            this.buf = buf;
            this.orgReaderIndex = buf.readerIndex();
        }

        public ByteBuf asBuf() {
            return buf;
        }

        public int pos() {
            return buf.readerIndex() - orgReaderIndex;
        }

        @Override
        public void close() {
            buf.readerIndex(orgReaderIndex);
        }

        public IPosition readPosition() throws IOException {
            return readPosition(packet.registry, this);
        }

        public static IPosition readPosition(PacketTypeRegistry registry, NetInput in) throws IOException {
            long val = in.readLong();
            long x, y, z;
            x = val >> 38;
            y = val;
            z = val >> 12;

            return new IPosition((int) (x << 38 >> 38), (int) (y << 52 >> 52), (int) (z << 38 >> 38));
        }

        public IGlobalPosition readGlobalPosition() throws IOException {
            return readGlobalPosition(packet.registry, this);
        }

        public static IGlobalPosition readGlobalPosition(PacketTypeRegistry registry, NetInput in) throws IOException {
            String dimension = in.readString();
            return new IGlobalPosition(dimension, readPosition(registry, in));
        }

        /**
         * Reads a network NBT tag.
         *
         * @return The tag or null if the packet contained no tag
         */
        public Tag readNBT() throws IOException {
            return readNBT(packet.registry, this);
        }

        public static Tag readNBT(PacketTypeRegistry registry, NetInput in) throws IOException {
            return Nbt.readNetwork(in);
        }

        public CompoundTag readNBTCompound() throws IOException {
            Tag tag = readNBT();
            return tag instanceof CompoundTag ? (CompoundTag) tag : null;
        }

        public BitSet readBitSet() throws IOException {
            return readBitSet(packet.registry, this);
        }

        public static BitSet readBitSet(PacketTypeRegistry registry, NetInput in) throws IOException {
            int length = in.readVarInt();
            long[] longs = new long[length];
            for (int i = 0; i < length; i++) {
                longs[i] = in.readLong();
            }
            return BitSet.valueOf(longs);
        }

        public <T> List<T> readList(IOSupplier<T> entryReader) throws IOException {
            return readList(packet.registry, this, entryReader);
        }

        public static <T> List<T> readList(PacketTypeRegistry registry, NetInput in, IOSupplier<T> entryReader) throws IOException {
            int len = in.readVarInt();
            List<T> result = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                result.add(entryReader.get());
            }
            return result;
        }

        public StringOrNbtText readText() throws IOException {
            return new StringOrNbtText(readString());
        }
    }

    public static class Writer extends ByteBufNetOutput implements AutoCloseable {
        private final Packet packet;
        private final ByteBuf buf;

        private Writer(Packet packet, ByteBuf buf) {
            super(buf);
            this.packet = packet;
            this.buf = buf;
        }

        @Override
        public void close() {
        }

        public void writePosition(IPosition pos) throws IOException {
            writePosition(packet.registry, this, pos);
        }

        public static void writePosition(PacketTypeRegistry registry, NetOutput out, IPosition pos) throws IOException {
            long x = pos.getX() & 0x3ffffff;
            long y = pos.getY() & 0xfff;
            long z = pos.getZ() & 0x3ffffff;
            out.writeLong(x << 38 | z << 12 | y);
        }

        public void writeGlobalPosition(IGlobalPosition pos) throws IOException {
            writeGlobalPosition(packet.registry, this, pos);
        }

        public static void writeGlobalPosition(PacketTypeRegistry registry, NetOutput out, IGlobalPosition pos) throws IOException {
            out.writeString(pos.getDimension());
            writePosition(registry, out, pos.getPosition());
        }

        public void writeNBT(Tag tag) throws IOException {
            writeNBT(packet.registry, this, tag);
        }

        public static void writeNBT(PacketTypeRegistry registry, NetOutput out, Tag tag) throws IOException {
            Nbt.writeNetwork(out, tag);
        }

        public void writeBitSet(BitSet bitSet) throws IOException {
            writeBitSet(packet.registry, this, bitSet);
        }

        public static void writeBitSet(PacketTypeRegistry registry, NetOutput out, BitSet bitSet) throws IOException {
            long[] longs = bitSet.toLongArray();
            out.writeVarInt(longs.length);
            for (long value : longs) {
                out.writeLong(value);
            }
        }

        public <T> void writeList(List<T> list, IOConsumer<T> entryWriter) throws IOException {
            writeList(packet.registry, this, list, entryWriter);
        }

        public static <T> void writeList(PacketTypeRegistry registry, NetOutput out, List<T> list, IOConsumer<T> entryWriter) throws IOException {
            out.writeVarInt(list.size());
            for (T entry : list) {
                entryWriter.consume(entry);
            }
        }

        public void writeText(StringOrNbtText value) throws IOException {
            writeString(value.str);
        }
    }
}
