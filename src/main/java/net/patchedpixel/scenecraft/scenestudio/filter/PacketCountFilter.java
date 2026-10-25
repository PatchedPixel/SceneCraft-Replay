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
package net.patchedpixel.scenecraft.scenestudio.filter;

import com.google.gson.JsonObject;
import net.patchedpixel.scenecraft.scenestudio.PacketData;
import net.patchedpixel.scenecraft.scenestudio.Studio;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketType;
import net.patchedpixel.scenecraft.scenestudio.stream.PacketStream;
import org.apache.commons.lang3.mutable.MutableInt;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;

public class PacketCountFilter implements StreamFilter {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EnumMap<PacketType, MutableInt> count = new EnumMap<>(PacketType.class);

    @Override
    public String getName() {
        return "packet_count";
    }

    @Override
    public void init(Studio studio, JsonObject config) {}

    @Override
    public void onStart(PacketStream stream) {
        count.clear();
    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        PacketType type = data.getPacket().getType();

        count.computeIfAbsent(type, key -> new MutableInt()).increment();
        return true;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {
        count.entrySet().stream()
                .sorted(Map.Entry.<PacketType, MutableInt>comparingByValue().reversed())
                .forEach(e -> LOGGER.info("[{}x] {}", e.getValue().intValue(), e.getKey()));
    }
}
