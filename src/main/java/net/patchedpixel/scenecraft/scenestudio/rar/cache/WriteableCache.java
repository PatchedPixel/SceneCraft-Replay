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

package net.patchedpixel.scenecraft.scenestudio.rar.cache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.patchedpixel.scenecraft.scenestudio.netio.NetOutput;
import net.patchedpixel.scenecraft.scenestudio.netio.StreamNetOutput;
import net.patchedpixel.scenecraft.scenestudio.util.ByteBufExtNetOutput;
import net.patchedpixel.scenecraft.scenestudio.util.Utils;

import java.io.IOException;
import java.io.OutputStream;

public class WriteableCache {
    private final CountingOutputStream wrapper;
    private final NetOutput out;

    public WriteableCache(OutputStream out) {
        this.wrapper = new CountingOutputStream(out);
        this.out = new StreamNetOutput(wrapper);
    }

    public int index() {
        return wrapper.index;
    }

    public NetOutput write() {
        return out;
    }

    public Deferred deferred() {
        return new Deferred(Unpooled.buffer());
    }

    public class Deferred extends ByteBufExtNetOutput {
        private Deferred(ByteBuf buf) {
            super(buf);
        }

        public int commit() throws IOException {
            int index = index();
            Utils.writeBytes(out, getBuf());
            getBuf().release();
            return index;
        }
    }

    private static class CountingOutputStream extends OutputStream {
        private final OutputStream inner;
        private int index;

        private CountingOutputStream(OutputStream inner) {
            this.inner = inner;
        }

        @Override
        public void write(int i) throws IOException {
            inner.write(i);
            index++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            inner.write(b, off, len);
            index += len;
        }
    }
}
