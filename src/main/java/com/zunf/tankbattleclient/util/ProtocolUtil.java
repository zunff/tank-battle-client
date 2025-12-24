package com.zunf.tankbattleclient.util;

import java.io.IOException;
import java.util.zip.CRC32;

import static com.zunf.tankbattleclient.constant.ProtocolConstant.*;

public class ProtocolUtil {


    /**
     * 校验某一帧：packet[off .. off+frameSize)
     * 要求 frameSize == HEADER_TOTAL_LENGTH + bodyLength
     */
    public static boolean verify(byte[] packet, int off, int frameSize) {
        if (packet == null) return false;
        if (off < 0 || frameSize < HEADER_TOTAL_LENGTH) return false;
        if (off + frameSize > packet.length) return false;

        int bodyLength = ByteArrUtil.readInt(packet, off + BODY_LENGTH_FIELD_OFFSET);
        if (bodyLength < 0) return false;
        if (frameSize != HEADER_TOTAL_LENGTH + bodyLength) return false;

        int expectedCrc = ByteArrUtil.readInt(packet, off + CRC32_FIELD_OFFSET);

        // 计算范围：头部 [off, off+CRC32_FIELD_OFFSET) + body [off+HEADER_TOTAL_LENGTH, off+HEADER_TOTAL_LENGTH+bodyLength)
        int actualCrc = crc32TwoParts(
                packet,
                off,                       // part1 start
                CRC32_FIELD_OFFSET,        // part1 len（不含crc字段本身）
                off + HEADER_TOTAL_LENGTH, // part2 start（body起点）
                bodyLength                 // part2 len
        );

        return expectedCrc == actualCrc;
    }

    public static int crc32TwoParts(byte[] data, int headerStart, int headerPartLen, int bodyStart, int bodyPartLen) {
        CRC32 crc32 = new CRC32();
        crc32.update(data, headerStart, headerPartLen);
        crc32.update(data, bodyStart, bodyPartLen);
        return (int) crc32.getValue();
    }

}
