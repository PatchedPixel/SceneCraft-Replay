package net.patchedpixel.scenecraft.scenestudio.netio;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes to a plain {@link OutputStream}.
 */
public class StreamNetOutput extends AbstractNetOutput {
    private final OutputStream out;

    public StreamNetOutput(OutputStream out) {
        this.out = out;
    }

    @Override
    public void writeByte(int value) throws IOException {
        out.write(value);
    }

    @Override
    public void writeBytes(byte[] bytes, int length) throws IOException {
        out.write(bytes, 0, length);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }
}
