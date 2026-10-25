package net.patchedpixel.scenecraft.scenestudio.netio;

import java.io.IOException;
import java.util.UUID;

/**
 * Sequential writer for the primitive types of the Minecraft network protocol.
 */
public interface NetOutput {
    void writeBoolean(boolean value) throws IOException;

    void writeByte(int value) throws IOException;

    void writeShort(int value) throws IOException;

    void writeChar(int value) throws IOException;

    void writeInt(int value) throws IOException;

    void writeVarInt(int value) throws IOException;

    void writeLong(long value) throws IOException;

    void writeVarLong(long value) throws IOException;

    void writeFloat(float value) throws IOException;

    void writeDouble(double value) throws IOException;

    void writeBytes(byte[] bytes) throws IOException;

    void writeBytes(byte[] bytes, int length) throws IOException;

    void writeShorts(short[] values) throws IOException;

    void writeInts(int[] values) throws IOException;

    void writeLongs(long[] values) throws IOException;

    void writeString(String value) throws IOException;

    void writeUUID(UUID value) throws IOException;

    void flush() throws IOException;
}
