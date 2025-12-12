package com.zunf.tankbattleclient.util;

import java.io.IOException;
import java.util.zip.CRC32;

import static com.zunf.tankbattleclient.constant.ProtocolConstant.*;

public class ProtocolUtil {


    public static boolean verify(byte[] packet) {
        if (packet == null || packet.length < HEADER_TOTAL_LENGTH) {
            return false;
        }

        int length = ByteArrUtil.readInt(packet, 2);
        if (length < 0 || packet.length != HEADER_TOTAL_LENGTH + length) {
            return false;
        }

        int expectedCrc = ByteArrUtil.readInt(packet, CRC32_FIELD_OFFSET);

        int actualCrc = crc32TwoParts(packet, 0, CRC32_FIELD_OFFSET, HEADER_TOTAL_LENGTH, length);

        return expectedCrc == actualCrc;
    }

    public static int crc32TwoParts(byte[] data, int headerStart, int headerPartLen, int bodyStart, int bodyPartLen) {
        CRC32 crc32 = new CRC32();
        crc32.update(data, headerStart, headerPartLen);
        crc32.update(data, bodyStart, bodyPartLen);
        return (int) crc32.getValue();
    }

}
