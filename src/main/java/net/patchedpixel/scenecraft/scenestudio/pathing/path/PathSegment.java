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
package net.patchedpixel.scenecraft.scenestudio.pathing.path;

import net.patchedpixel.scenecraft.scenestudio.pathing.interpolation.Interpolator;

/**
 * Represents a segment of a Path consisting of one start property and one end property.
 * Each segment is interpolated by one Interpolator. Multiple segments may have the same Interpolator and can only
 * be interpolated together (they may or may not influence each other indirectly).
 */
public interface PathSegment {
    Path getPath();

    /**
     * Return the start property of this segment.
     *
     * @return The first property
     */
    Keyframe getStartKeyframe();

    /**
     * Return the end property of this segment.
     *
     * @return The second property
     */
    Keyframe getEndKeyframe();

    /**
     * Return the interpolator responsible for this path segment.
     *
     * @return The interpolator or {@code null} if not yet set
     * @throws IllegalStateException If no interpolator has been set yet
     */
    Interpolator getInterpolator();

    /**
     * Set the interpolator responsible for this path segment.
     * @param interpolator The interpolator
     */
    void setInterpolator(Interpolator interpolator);
}
