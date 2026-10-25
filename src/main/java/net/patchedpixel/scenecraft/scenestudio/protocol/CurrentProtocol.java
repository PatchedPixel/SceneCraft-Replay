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

import net.minecraft.SharedConstants;

/**
 * The protocol version of the running game. Replays can only be read when they were recorded with the
 * exact same protocol version because no packet conversion is done.
 */
public final class CurrentProtocol {
    private CurrentProtocol() {
    }

    /**
     * @return The protocol version number of the running Minecraft version
     */
    public static int get() {
        return SharedConstants.getCurrentVersion().getProtocolVersion();
    }
}
