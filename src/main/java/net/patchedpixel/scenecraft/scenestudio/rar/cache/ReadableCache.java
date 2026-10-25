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
import net.patchedpixel.scenecraft.scenestudio.netio.NetInput;
import net.patchedpixel.scenecraft.scenestudio.util.ByteBufExtNetInput;

public class ReadableCache {
    private final ByteBuf buf;
    private final NetInput in;

    public ReadableCache(ByteBuf buf) {
        this.buf = buf;
        this.in = new ByteBufExtNetInput(buf);
    }

    public NetInput seek(int index) {
        buf.readerIndex(index);
        return in;
    }

    public void release() {
        buf.release();
    }
}
