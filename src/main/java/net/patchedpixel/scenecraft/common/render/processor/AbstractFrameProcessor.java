package net.patchedpixel.scenecraft.common.render.processor;

import net.patchedpixel.scenecraft.common.render.rendering.Frame;
import net.patchedpixel.scenecraft.common.render.rendering.FrameProcessor;

import java.io.IOException;

public abstract class AbstractFrameProcessor<R extends Frame, P extends Frame> implements FrameProcessor<R, P> {
    @Override
    public void close() throws IOException {}
}
