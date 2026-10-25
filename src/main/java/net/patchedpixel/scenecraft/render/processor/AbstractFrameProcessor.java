package net.patchedpixel.scenecraft.render.processor;

import net.patchedpixel.scenecraft.render.rendering.Frame;
import net.patchedpixel.scenecraft.render.rendering.FrameProcessor;

import java.io.IOException;

public abstract class AbstractFrameProcessor<R extends Frame, P extends Frame> implements FrameProcessor<R, P> {
    @Override
    public void close() throws IOException {}
}
