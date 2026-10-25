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

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;

import java.util.HashMap;
import java.util.Map;

final class VanillaPacketIds {
    private VanillaPacketIds() {
    }

    static Map<String, Integer> forState(State state) {
        switch (state) {
            case LOGIN:
                return collect(ConnectionProtocol.LOGIN);
            case PLAY:
                return collect(ConnectionProtocol.PLAY);
            default:
                return new HashMap<>();
        }
    }

    private static Map<String, Integer> collect(ConnectionProtocol protocol) {
        Map<String, Integer> result = new HashMap<>();
        protocol.getPacketsByIds(PacketFlow.CLIENTBOUND).forEach((id, type) -> result.put(nameOf(type), id));
        return result;
    }

    private static String nameOf(Class<?> type) {
        String name = type.getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        name = name.replace("Packet$", "").replace("$", "");
        if (name.startsWith("Clientbound")) {
            name = name.substring("Clientbound".length());
        }
        if (name.endsWith("Packet")) {
            name = name.substring(0, name.length() - "Packet".length());
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(ch));
        }
        return result.toString();
    }
}
