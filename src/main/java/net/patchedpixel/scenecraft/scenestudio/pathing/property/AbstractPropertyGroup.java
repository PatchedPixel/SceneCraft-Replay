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
package net.patchedpixel.scenecraft.scenestudio.pathing.property;

import net.patchedpixel.scenecraft.scenestudio.util.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for most property groups.
 */
public abstract class AbstractPropertyGroup implements PropertyGroup {
    private final String id, localizationKey;
    private final List<Property> properties = new ArrayList<>();

    public AbstractPropertyGroup(String id, String localizationKey) {
        this.id = id;
        this.localizationKey = localizationKey;
    }

    @Override
    public String getLocalizedName() {
        return I18n.format(localizationKey);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<Property> getProperties() {
        return properties;
    }
}
