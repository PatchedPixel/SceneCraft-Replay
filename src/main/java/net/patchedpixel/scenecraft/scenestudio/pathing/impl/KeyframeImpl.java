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
package net.patchedpixel.scenecraft.scenestudio.pathing.impl;

import net.patchedpixel.scenecraft.scenestudio.pathing.path.Keyframe;
import net.patchedpixel.scenecraft.scenestudio.pathing.property.Property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

public class KeyframeImpl implements Keyframe {
    private final long time;
    private final Map<Property, Object> properties = new HashMap<>();

    public KeyframeImpl(long time) {
        this.time = time;
    }

    @Override
    public long getTime() {
        return time;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getValue(Property<T> property) {
        return properties.containsKey(property) ? Optional.of((T) properties.get(property)) : Optional.empty();
    }

    @Override
    public <T> void setValue(Property<T> property, T value) {
        properties.put(property, value);
    }

    @Override
    public void removeProperty(Property property) {
        properties.remove(property);
    }

    @Override
    public Set<Property> getProperties() {
        return Collections.unmodifiableSet(properties.keySet());
    }
}
