package net.patchedpixel.scenecraft.scenestudio.netio;

import io.netty.buffer.ByteBuf;

/**
 * Writes to a Netty {@link ByteBuf}. Advances the writer index of the buffer.
 */
public class ByteBufNetOutput extends AbstractNetOutput {
    private final ByteBuf buf;

    public ByteBufNetOutput(ByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public void writeBoolean(boolean value) {
        buf.writeBoolean(value);
    }

    @Override
    public void writeByte(int value) {
        buf.writeByte(value);
    }

    @Override
    public void writeShort(int value) {
        buf.writeShort(value);
    }

    @Override
    public void writeInt(int value) {
        buf.writeInt(value);
    }

    @Override
    public void writeLong(long value) {
        buf.writeLong(value);
    }

    @Override
    public void writeFloat(float value) {
        buf.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) {
        buf.writeDouble(value);
    }

    @Override
    public void writeBytes(byte[] bytes, int length) {
        buf.writeBytes(bytes, 0, length);
    }
}
