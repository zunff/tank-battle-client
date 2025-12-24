package com.zunf.tankbattleclient.handler;

import com.zunf.tankbattleclient.model.message.InboundMessage;
import com.zunf.tankbattleclient.util.ByteArrUtil;
import com.zunf.tankbattleclient.util.ProtocolUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import static com.zunf.tankbattleclient.constant.ProtocolConstant.*;

public final class ProtocolFrameReader {
    
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    // 从 InputStream 读一些数据进来
    public void readFrom(InputStream in) throws IOException {
        byte[] tmp = new byte[4096];
        int n = in.read(tmp); // 阻塞
        if (n < 0) throw new IOException("EOF");
        buffer.write(tmp, 0, n);
    }

    // 尝试取出一帧；没有完整帧则返回 null
    public InboundMessage tryDecodeOne() throws IOException {
        byte[] data = buffer.toByteArray();
        if (data.length < HEADER_TOTAL_LENGTH) return null;

        int length = ByteArrUtil.readInt(data, BODY_LENGTH_FIELD_OFFSET);
        if (length < 0 || length > 10_000_000) {
            throw new IOException("Invalid length: " + length);
        }

        int frameSize = HEADER_TOTAL_LENGTH + length;
        if (data.length < frameSize) return null; // 半包：等更多数据

        // 只对这一帧做 CRC 校验（不要校验整个 data）
        if (!ProtocolUtil.verify(data, 0, frameSize)) {
            throw new IOException("CRC32 mismatch");
        }

        int type = ByteArrUtil.readUnsignedShort(data, OPERATION_TYPE_FIELD_OFFSET); // type=2B
        int version = data[VERSION_FIELD_OFFSET] & 0xFF;
        int requestId = ByteArrUtil.readInt(data, REQUEST_ID_FIELD_OFFSET);

        byte[] body = Arrays.copyOfRange(data, HEADER_TOTAL_LENGTH, frameSize);

        // 消费掉这一帧
        byte[] remaining = Arrays.copyOfRange(data, frameSize, data.length);
        buffer.reset();
        buffer.write(remaining);

        return new InboundMessage(type, (byte) version, requestId, body);
    }
    
}
