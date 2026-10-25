package net.patchedpixel.scenecraft.scenestudio.netio;

import java.io.IOException;
import java.util.UUID;

/**
 * Sequential reader for the primitive types of the Minecraft network protocol.
 */
public interface NetInput {
    boolean readBoolean() throws IOException;

    byte readByte() throws IOException;

    int readUnsignedByte() throws IOException;

    short readShort() throws IOException;

    int readUnsignedShort() throws IOException;

    char readChar() throws IOException;

    int readInt() throws IOException;

    int readVarInt() throws IOException;

    long readLong() throws IOException;

    long readVarLong() throws IOException;

    float readFloat() throws IOException;

    double readDouble() throws IOException;

    byte[] readBytes(int length) throws IOException;

    /**
     * Fills the whole array.
     *
     * @return The number of bytes read
     */
    int readBytes(byte[] dest) throws IOException;

    int readBytes(byte[] dest, int offset, int length) throws IOException;

    short[] readShorts(int length) throws IOException;

    int[] readInts(int length) throws IOException;

    long[] readLongs(int length) throws IOException;

    String readString() throws IOException;

    UUID readUUID() throws IOException;

    int available() throws IOException;
}
