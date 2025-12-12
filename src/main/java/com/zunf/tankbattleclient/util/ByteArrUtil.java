package com.zunf.tankbattleclient.util;

public class ByteArrUtil {

    public static void writeInt(byte[] data, int offset, int value) {
        data[offset]     = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) (value);
    }

    public static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                |  (data[offset + 3] & 0xFF);
    }
}
