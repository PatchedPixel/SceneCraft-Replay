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
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagTypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Nbt {
    private Nbt() {
    }

    public static Tag readNetwork(NetInput in) throws IOException {
        int type = in.readUnsignedByte();
        if (type == 0) {
            return null;
        }
        int nameLength = (in.readUnsignedByte() << 8) | in.readUnsignedByte();
        for (int i = 0; i < nameLength; i++) {
            in.readUnsignedByte();
        }
        InputStream stream = new InputStream() {
            @Override
            public int read() throws IOException {
                return in.readUnsignedByte();
            }
        };
        return TagTypes.getType(type).load(new DataInputStream(stream), 0, NbtAccounter.UNLIMITED);
    }

    public static CompoundTag readNetworkCompound(NetInput in) throws IOException {
        Tag tag = readNetwork(in);
        return tag instanceof CompoundTag ? (CompoundTag) tag : null;
    }

    public static void writeNetwork(NetOutput out, Tag tag) throws IOException {
        if (tag == null) {
            out.writeByte(0);
            return;
        }
        OutputStream stream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.writeByte(b);
            }
        };
        out.writeByte(tag.getId());
        out.writeByte(0);
        out.writeByte(0);
        tag.write(new DataOutputStream(stream));
    }

    public static int intOr(Tag compound, String key, int def) {
        if (compound instanceof CompoundTag tag && tag.contains(key, 99)) {
            return tag.getInt(key);
        }
        return def;
    }
}
