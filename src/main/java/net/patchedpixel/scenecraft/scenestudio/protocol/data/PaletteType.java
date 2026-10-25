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
package net.patchedpixel.scenecraft.scenestudio.protocol.data;

/**
 * The kind of data stored in a paletted container of a chunk section.
 */
public enum PaletteType {
    /** 16 x 16 x 16 block states; palettes with more than 8 bits per entry use the global palette. */
    BLOCKS(16 * 16 * 16, 8),
    /** 4 x 4 x 4 biomes; palettes with more than 3 bits per entry use the global palette. */
    BIOMES(4 * 4 * 4, 3);

    private final int size;
    private final int highestBitsPerValue;

    PaletteType(int size, int highestBitsPerValue) {
        this.size = size;
        this.highestBitsPerValue = highestBitsPerValue;
    }

    /**
     * @return The number of entries in a container of this type
     */
    public int size() {
        return size;
    }

    /**
     * @return The highest number of bits per entry for which a local palette is used
     */
    public int highestBitsPerValue() {
        return highestBitsPerValue;
    }
}
