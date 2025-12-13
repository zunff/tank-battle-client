package com.zunf.tankbattleclient.handler;

import com.zunf.tankbattleclient.util.ByteArrUtil;
import com.zunf.tankbattleclient.util.ProtocolUtil;

import static com.zunf.tankbattleclient.constant.ProtocolConstant.*;

public class ProtocolEncoder {

    public static byte[] encode(byte type, byte version, int requestId, byte[] body) {
        if (body == null) body = new byte[0];
        int length = body.length;

        byte[] packet = new byte[HEADER_TOTAL_LENGTH + length];

        // 1) 写 header（crc 先占位 0）
        packet[0] = type;
        packet[1] = version;
        ByteArrUtil.writeInt(packet, REQUEST_ID_FIELD_OFFSET, requestId);
        ByteArrUtil.writeInt(packet, BODY_LENGTH_FIELD_OFFSET, length);     // length at offset 2
        ByteArrUtil.writeInt(packet, CRC32_FIELD_OFFSET, 0); // crc placeholder

        // 2) 写 body
        System.arraycopy(body, 0, packet, HEADER_TOTAL_LENGTH, length);

        // 3) 计算 CRC32：header 前 6 字节 + body
        int crc = ProtocolUtil.crc32TwoParts(packet, 0, CRC32_FIELD_OFFSET, HEADER_TOTAL_LENGTH, length);

        // 4) 回填 crc32
        ByteArrUtil.writeInt(packet, CRC32_FIELD_OFFSET, crc);

        return packet;
    }
}
