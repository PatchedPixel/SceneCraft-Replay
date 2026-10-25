package net.patchedpixel.scenecraft.render.capturer;

import net.patchedpixel.scenecraft.render.RenderSettings;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;

public interface RenderInfo {
    ReadableDimension getFrameSize();

    int getFramesDone();

    int getTotalFrames();

    float updateForNextFrame();

    RenderSettings getRenderSettings();
}
