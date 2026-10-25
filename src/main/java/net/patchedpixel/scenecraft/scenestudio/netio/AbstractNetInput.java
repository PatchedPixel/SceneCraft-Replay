package net.patchedpixel.scenecraft.scenestudio.netio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Implements everything except the raw byte access on top of {@link #readByte()} and
 * {@link #readBytes(byte[], int, int)}.
 */
public abstract class AbstractNetInput implements NetInput {
    @Override
    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    @Override
    public short readShort() throws IOException {
        return (short) readUnsignedShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return (readUnsignedByte() << 8) | readUnsignedByte();
    }

    @Override
    public char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    @Override
    public int readInt() throws IOException {
        return (readUnsignedByte() << 24) | (readUnsignedByte() << 16) | (readUnsignedByte() << 8) | readUnsignedByte();
    }

    @Override
    public int readVarInt() throws IOException {
        int value = 0;
        int size = 0;
        int b;
        while (((b = readByte()) & 0x80) == 0x80) {
            value |= (b & 0x7F) << (size++ * 7);
            if (size > 5) {
                throw new IOException("VarInt is too long (more than 5 bytes)");
            }
        }
        return value | ((b & 0x7F) << (size * 7));
    }

    @Override
    public long readLong() throws IOException {
        return ((long) readInt() << 32) | (readInt() & 0xFFFFFFFFL);
    }

    @Override
    public long readVarLong() throws IOException {
        long value = 0;
        int size = 0;
        int b;
        while (((b = readByte()) & 0x80) == 0x80) {
            value |= (long) (b & 0x7F) << (size++ * 7);
            if (size > 10) {
                throw new IOException("VarLong is too long (more than 10 bytes)");
            }
        }
        return value | ((long) (b & 0x7F) << (size * 7));
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public byte[] readBytes(int length) throws IOException {
        if (length < 0) {
            throw new IOException("Negative array length: " + length);
        }
        byte[] result = new byte[length];
        readBytes(result, 0, length);
        return result;
    }

    @Override
    public int readBytes(byte[] dest) throws IOException {
        return readBytes(dest, 0, dest.length);
    }

    @Override
    public short[] readShorts(int length) throws IOException {
        short[] result = new short[length];
        for (int i = 0; i < length; i++) {
            result[i] = readShort();
        }
        return result;
    }

    @Override
    public int[] readInts(int length) throws IOException {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = readInt();
        }
        return result;
    }

    @Override
    public long[] readLongs(int length) throws IOException {
        if (length < 0) {
            throw new IOException("Negative array length: " + length);
        }
        long[] result = new long[length];
        for (int i = 0; i < length; i++) {
            result[i] = readLong();
        }
        return result;
    }

    @Override
    public String readString() throws IOException {
        int length = readVarInt();
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    @Override
    public UUID readUUID() throws IOException {
        return new UUID(readLong(), readLong());
    }
}
