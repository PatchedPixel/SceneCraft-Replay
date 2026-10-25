package net.patchedpixel.scenecraft.scenestudio.netio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads from a plain {@link InputStream}.
 */
public class StreamNetInput extends AbstractNetInput {
    private final InputStream in;

    public StreamNetInput(InputStream in) {
        this.in = in;
    }

    @Override
    public byte readByte() throws IOException {
        int b = in.read();
        if (b < 0) {
            throw new EOFException();
        }
        return (byte) b;
    }

    @Override
    public int readBytes(byte[] dest, int offset, int length) throws IOException {
        int total = 0;
        while (total < length) {
            int read = in.read(dest, offset + total, length - total);
            if (read < 0) {
                throw new EOFException();
            }
            total += read;
        }
        return total;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }
}
