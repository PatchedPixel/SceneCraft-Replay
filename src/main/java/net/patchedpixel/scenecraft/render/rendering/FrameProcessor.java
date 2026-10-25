package net.patchedpixel.scenecraft.render.rendering;

import java.io.Closeable;

public interface FrameProcessor<R extends Frame, P extends Frame> extends Closeable {

    P process(R rawFrame);

}
