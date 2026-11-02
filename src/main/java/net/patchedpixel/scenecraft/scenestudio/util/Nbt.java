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
package net.patchedpixel.scenecraft.scenestudio.util;

import net.patchedpixel.scenecraft.scenestudio.netio.NetInput;
import net.patchedpixel.scenecraft.scenestudio.netio.NetOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The only place where the scenestudio talks to the NBT API of Minecraft.
 * Network NBT is nameless: a type net followed by the payload (type net 0 means "no tag").
 */
public final class Nbt {
    private Nbt() {
    }

    /**
     * Reads a network NBT tag.
     *
     * @return The tag or null if the packet contained the "end" tag (i.e. no tag)
     */
    public static Tag readNetwork(NetInput in) throws IOException {
        int type = in.readUnsignedByte();
        if(type == 0) {
            return null;
        }
        // readAnyTag expects to read the type net itself, so hand it back as the first byte of the stream
        InputStream stream = new InputStream() {
            private boolean typeSent;

            @Override
            public int read() throws IOException {
                if(!typeSent) {
                    typeSent = true;
                    return type;
                }
                return in.readUnsignedByte();
            }
        };
        return NbtIo.readAnyTag(new DataInputStream(stream), NbtAccounter.unlimitedHeap());
    }

    /**
     * Reads a network NBT tag which must be a compound.
     *
     * @return The compound or null if there is no tag or it is not a compound
     */
    public static CompoundTag readNetworkCompound(NetInput in) throws IOException {
        Tag tag = readNetwork(in);
        return tag instanceof CompoundTag ? (CompoundTag) tag : null;
    }

    /**
     * Writes a network NBT tag.
     *
     * @param tag The tag, null is written as the "end" tag
     */
    public static void writeNetwork(NetOutput out, Tag tag) throws IOException {
        if(tag == null) {
            out.writeByte(0);
            return;
        }
        OutputStream stream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.writeByte(b);
            }
        };
        NbtIo.writeAnyTag(tag, new DataOutputStream(stream));
    }

    /**
     * @return The integer stored under the given key of a compound tag, or the default if absent or not a compound
     */
    public static int intOr(Tag compound, String key, int def) {
        if(compound instanceof CompoundTag) {
            return ((CompoundTag) compound).getIntOr(key, def);
        }
        return def;
    }
}
