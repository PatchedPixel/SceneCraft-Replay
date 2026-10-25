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

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps between packet ids and {@link PacketType}s for one connection state of the running game's protocol.
 */
public class PacketTypeRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<State, PacketTypeRegistry> INSTANCES = new EnumMap<>(State.class);

    /**
     * Returns the registry for the given state. The registries are created on first use because the protocol
     * of the game has to be fully registered by then.
     */
    public static synchronized PacketTypeRegistry get(State state) {
        return INSTANCES.computeIfAbsent(state, PacketTypeRegistry::new);
    }

    private final State state;
    private final PacketType unknown;
    private final Map<Integer, PacketType> typeForId = new HashMap<>();
    private final Map<PacketType, Integer> idForType = new HashMap<>();

    private PacketTypeRegistry(State state) {
        this.state = state;

        Map<String, Integer> vanillaIds = VanillaPacketIds.forState(state);
        PacketType unknown = null;
        for (PacketType type : PacketType.values()) {
            if (type.getState() != state) {
                continue;
            }
            if (type.isUnknown()) {
                unknown = type;
                continue;
            }
            if (type.getVanillaName() == null) {
                continue; // removed from the protocol
            }
            Integer id = vanillaIds.get(type.getVanillaName());
            if (id == null) {
                LOGGER.warn("Packet {} ('{}') does not exist in the {} protocol, it will be treated as unknown",
                        type, type.getVanillaName(), state);
                continue;
            }
            PacketType existing = typeForId.get(id);
            if (existing != null) {
                LOGGER.error("Packet id {} in the {} protocol is used by both {} and {}, keeping {}",
                        id, state, existing, type, existing);
                continue;
            }
            typeForId.put(id, type);
            idForType.put(type, id);
        }
        this.unknown = unknown;
    }

    /**
     * @return The protocol version of the running game
     */
    public int getProtocolVersion() {
        return CurrentProtocol.get();
    }

    public State getState() {
        return state;
    }

    /**
     * @return The id of the given type or null if that type does not exist in the current protocol
     */
    public Integer getId(PacketType type) {
        return idForType.get(type);
    }

    public PacketType getType(int id) {
        return typeForId.getOrDefault(id, unknown);
    }

    public PacketTypeRegistry withState(State state) {
        return PacketTypeRegistry.get(state);
    }

    /**
     * @return The registry for the phase that follows a successful login, which is the configuration phase
     */
    public PacketTypeRegistry withLoginSuccess() {
        return withState(State.PLAY);
    }
}
