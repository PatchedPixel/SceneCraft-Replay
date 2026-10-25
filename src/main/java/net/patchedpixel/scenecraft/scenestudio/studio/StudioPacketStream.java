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
package net.patchedpixel.scenecraft.scenestudio.studio;

import net.patchedpixel.scenecraft.scenestudio.PacketData;
import net.patchedpixel.scenecraft.scenestudio.io.ReplayInputStream;
import net.patchedpixel.scenecraft.scenestudio.stream.AbstractPacketStream;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;

public class StudioPacketStream extends AbstractPacketStream {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ReplayInputStream in;

    public StudioPacketStream(ReplayInputStream in) {
        this.in = in;
    }

    @Override
    protected PacketData nextInput() {
        try {
            return in.readPacket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {}

    @Override
    protected void cleanup() {
        try {
            in.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close the replay input stream", e);
        }
    }
}
