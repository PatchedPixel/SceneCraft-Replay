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

import net.patchedpixel.scenecraft.scenestudio.Studio;
import net.patchedpixel.scenecraft.scenestudio.filter.ChangeTimestampFilter;
import net.patchedpixel.scenecraft.scenestudio.filter.DimensionTracker;
import net.patchedpixel.scenecraft.scenestudio.filter.PacketCountFilter;
import net.patchedpixel.scenecraft.scenestudio.filter.RemoveFilter;
import net.patchedpixel.scenecraft.scenestudio.filter.StreamFilter;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayMetaData;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class SceneStudio implements Studio {
    // Filters are registered explicitly instead of through a ServiceLoader, which does not reliably
    // see classes of a mod from the context class loader.
    private final Map<String, Supplier<StreamFilter>> streamFilters = new LinkedHashMap<>();

    public SceneStudio() {
        register(new ChangeTimestampFilter().getName(), ChangeTimestampFilter::new);
        register(new DimensionTracker().getName(), DimensionTracker::new);
        register(new PacketCountFilter().getName(), PacketCountFilter::new);
        register(new RemoveFilter().getName(), RemoveFilter::new);
    }

    /**
     * Makes a stream filter available by its name.
     */
    public void register(String name, Supplier<StreamFilter> factory) {
        streamFilters.put(name.toLowerCase(Locale.ROOT), factory);
    }

    @Override
    public String getName() {
        return "SceneStudio";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public StreamFilter loadStreamFilter(String name) {
        Supplier<StreamFilter> factory = streamFilters.get(name.toLowerCase(Locale.ROOT));
        return factory != null ? factory.get() : null;
    }

    @Override
    public boolean isCompatible(int fileVersion, int protocolVersion, int currentVersion) {
        // No packet conversion is done, the replay has to be recorded with the protocol that is in use
        return fileVersion >= 14 && protocolVersion == currentVersion;
    }

    @Override
    public int getCurrentFileFormatVersion() {
        return ReplayMetaData.CURRENT_FILE_FORMAT_VERSION;
    }
}
