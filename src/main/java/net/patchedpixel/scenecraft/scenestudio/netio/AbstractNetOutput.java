package net.patchedpixel.scenecraft.scenestudio.netio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Implements everything except the raw byte access on top of {@link #writeByte(int)} and
 * {@link #writeBytes(byte[], int)}.
 */
public abstract class AbstractNetOutput implements NetOutput {
    @Override
    public void writeBoolean(boolean value) throws IOException {
        writeByte(value ? 1 : 0);
    }

    @Override
    public void writeShort(int value) throws IOException {
        writeByte((value >>> 8) & 0xFF);
        writeByte(value & 0xFF);
    }

    @Override
    public void writeChar(int value) throws IOException {
        writeShort(value);
    }

    @Override
    public void writeInt(int value) throws IOException {
        writeByte((value >>> 24) & 0xFF);
        writeByte((value >>> 16) & 0xFF);
        writeByte((value >>> 8) & 0xFF);
        writeByte(value & 0xFF);
    }

    @Override
    public void writeVarInt(int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        writeByte(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        writeInt((int) (value >>> 32));
        writeInt((int) value);
    }

    @Override
    public void writeVarLong(long value) throws IOException {
        while ((value & ~0x7FL) != 0) {
            writeByte((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        writeByte((int) value);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        writeInt(Float.floatToIntBits(value));
    }

    @Override
    public void writeDouble(double value) throws IOException {
        writeLong(Double.doubleToLongBits(value));
    }

    @Override
    public void writeBytes(byte[] bytes) throws IOException {
        writeBytes(bytes, bytes.length);
    }

    @Override
    public void writeShorts(short[] values) throws IOException {
        for (short value : values) {
            writeShort(value);
        }
    }

    @Override
    public void writeInts(int[] values) throws IOException {
        for (int value : values) {
            writeInt(value);
        }
    }

    @Override
    public void writeLongs(long[] values) throws IOException {
        for (long value : values) {
            writeLong(value);
        }
    }

    @Override
    public void writeString(String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        writeBytes(bytes);
    }

    @Override
    public void writeUUID(UUID value) throws IOException {
        writeLong(value.getMostSignificantBits());
        writeLong(value.getLeastSignificantBits());
    }

    @Override
    public void flush() throws IOException {
    }
}
