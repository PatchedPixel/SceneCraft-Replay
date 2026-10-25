package net.patchedpixel.scenecraft.render.processor;

import net.patchedpixel.scenecraft.render.frame.BitmapFrame;

import java.nio.FloatBuffer;

public class AbsoluteDepthProcessor extends AbstractFrameProcessor<BitmapFrame, BitmapFrame> {
    // absolute depth is 2 * near * far / (far + near - (far - near) * (2 * z - 1))
    // precomputed:     [      a       ]  [    b     ]  [    c     ]
    private final float a;
    private final float b;
    private final float c;

    public AbsoluteDepthProcessor(float near, float far) {
        a = 2 * near * far;
        b = far + near;
        c = far - near;
    }

    @Override
    public BitmapFrame process(BitmapFrame frame) {

        FloatBuffer buffer = frame.getByteBuffer().asFloatBuffer();
        int len = buffer.remaining();
        for (int i = 0; i < len; i++) {
            float z = buffer.get(i);
            z = a / (b - c * (2 * z - 1));
            buffer.put(i, z);
        }

        return frame;
    }
}
