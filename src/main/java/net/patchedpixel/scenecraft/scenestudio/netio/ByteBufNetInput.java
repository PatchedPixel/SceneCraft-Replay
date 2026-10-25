package net.patchedpixel.scenecraft.scenestudio.netio;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * Reads from a Netty {@link ByteBuf}. Advances the reader index of the buffer.
 */
public class ByteBufNetInput extends AbstractNetInput {
    private final ByteBuf buf;

    public ByteBufNetInput(ByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public boolean readBoolean() {
        return buf.readBoolean();
    }

    @Override
    public byte readByte() {
        return buf.readByte();
    }

    @Override
    public int readUnsignedByte() {
        return buf.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return buf.readShort();
    }

    @Override
    public int readUnsignedShort() {
        return buf.readUnsignedShort();
    }

    @Override
    public int readInt() {
        return buf.readInt();
    }

    @Override
    public long readLong() {
        return buf.readLong();
    }

    @Override
    public float readFloat() {
        return buf.readFloat();
    }

    @Override
    public double readDouble() {
        return buf.readDouble();
    }

    @Override
    public int readBytes(byte[] dest, int offset, int length) throws IOException {
        if (length > buf.readableBytes()) {
            throw new IOException("Tried to read " + length + " bytes but only " + buf.readableBytes() + " are available");
        }
        buf.readBytes(dest, offset, length);
        return length;
    }

    @Override
    public int available() {
        return buf.readableBytes();
    }
}
