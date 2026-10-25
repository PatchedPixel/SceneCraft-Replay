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

package net.patchedpixel.scenecraft.scenestudio.protocol.registry;

import net.patchedpixel.scenecraft.scenestudio.util.Nbt;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

public class DimensionType {
    private final CompoundTag tag;
    private final String name;
    private final int id;
    private final int minY;
    private final int height;

    /**
     * Creates a dimension type with default values, used when the registry data is not available.
     */
    public DimensionType(String name) {
        this(new CompoundTag(), name, 0);
    }

    public DimensionType(CompoundTag tag, String name, int id) {
        this.tag = tag;
        this.name = name;
        this.id = id;
        this.minY = Nbt.intOr(tag, "min_y", 0);
        this.height = Nbt.intOr(tag, "height", 256);
    }

    public String getName() {
        return this.name;
    }

    public CompoundTag getTag() {
        return this.tag;
    }

    public int getId() {
        return this.id;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMinSectionY() {
        return this.minY >> 4;
    }

    public int getHeight() {
        return this.height;
    }

    public int getSections() {
        return this.height >> 4;
    }

    public int sectionYToIndex(int sectionY) {
        return sectionY - this.getMinSectionY();
    }

    public int indexToSectionY(int index) {
        return index + this.getMinSectionY();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DimensionType that = (DimensionType) o;
        return tag.equals(that.tag) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, name);
    }

    public static DimensionType fromRegistry(Registries registries, String name) {
        if (registries == null) {
            return new DimensionType(name);
        }
        Registries.Entry entry = registries.getEntry("minecraft:dimension_type", name);
        if (entry == null) {
            return new DimensionType(name);
        }
        return DimensionType.fromRegistry(entry);
    }

    public static DimensionType fromRegistry(Registries registries, int id) {
        Registries.Entry entry = registries.getEntry("minecraft:dimension_type", id);
        if (entry == null) {
            return new DimensionType(new CompoundTag(), "", id);
        }
        return DimensionType.fromRegistry(entry);
    }

    public static DimensionType fromRegistry(Registries.Entry entry) {
        return new DimensionType(entry.asCompoundOrEmpty(), entry.name, entry.id);
    }
}
