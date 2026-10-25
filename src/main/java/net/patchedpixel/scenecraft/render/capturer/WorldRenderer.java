package net.patchedpixel.scenecraft.render.capturer;

import java.io.Closeable;

public interface WorldRenderer extends Closeable {
    void renderWorld(CaptureData data);
    void setOmnidirectional(boolean omnidirectional);
}
