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
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;
import net.patchedpixel.scenecraft.scenestudio.protocol.packets.PacketNotifyClient;
import net.patchedpixel.scenecraft.scenestudio.rar.PacketSink;
import net.patchedpixel.scenecraft.scenestudio.rar.RandomAccessState;

import java.io.IOException;

public class Weather extends TransientThing implements RandomAccessState {

    public Weather(PacketTypeRegistry registry, NetInput in) throws IOException {
        super(registry, in);
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
    }

    public static class Builder extends TransientThing.Builder {
        public Builder(PacketTypeRegistry registry) throws IOException {
            addSpawnPacket(PacketNotifyClient.write(registry, PacketNotifyClient.Action.START_RAIN, 0));
            addDespawnPacket(PacketNotifyClient.write(registry, PacketNotifyClient.Action.STOP_RAIN, 0));
        }
    }
}
