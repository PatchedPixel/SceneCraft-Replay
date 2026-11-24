package net.patchedpixel.scenecraft.common.render.capturer;

import net.patchedpixel.scenecraft.common.render.RenderSettings;
import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;

public interface RenderInfo {
    Size getFrameSize();

    int getFramesDone();

    int getTotalFrames();

    float updateForNextFrame();

    RenderSettings getRenderSettings();
}
