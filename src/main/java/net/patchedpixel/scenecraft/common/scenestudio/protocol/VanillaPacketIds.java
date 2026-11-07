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
package net.patchedpixel.scenecraft.common.scenestudio.protocol;

import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.login.LoginProtocols;

import java.util.HashMap;
import java.util.Map;

/**
 * Looks up the numeric ids of clientbound packets in the protocol of the running game.
 * <p>
 * This is the only class that depends on the protocol registration of Minecraft. If the names of the
 * protocol templates or of {@link ProtocolInfo.Details#listPackets} differ in your Minecraft version, only this
 * class has to be adapted.
 */
final class VanillaPacketIds {
    private VanillaPacketIds() {
    }

    /**
     * @return The packet ids by vanilla packet name (the path of the packet type net) for the given state
     */
    static Map<String, Integer> forState(State state) {
        switch(state) {
            case LOGIN:
                return collect(LoginProtocols.CLIENTBOUND_TEMPLATE.details());
            case CONFIGURATION:
                return collect(ConfigurationProtocols.CLIENTBOUND_TEMPLATE.details());
            case PLAY:
                return collect(GameProtocols.CLIENTBOUND_TEMPLATE.details());
            default:
                return new HashMap<>();
        }
    }

    private static Map<String, Integer> collect(ProtocolInfo.Details details) {
        Map<String, Integer> result = new HashMap<>();
        details.listPackets((type, id) -> result.put(type.id().getPath(), id));
        return result;
    }
}
