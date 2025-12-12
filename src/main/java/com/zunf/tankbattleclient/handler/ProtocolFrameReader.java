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
        if (data.length < HEADER_TOTAL_LENGTH) {
            return null;
        }
        // CRC 校验
        if (!ProtocolUtil.verify(data)) {
            throw new IOException("CRC32 mismatch");
        }

        int idx = 0;
        int type = data[idx++] & 0xFF;
        int version = data[idx++] & 0xFF;

        int length = ByteArrUtil.readInt(data, idx); idx += 4;

        if (length < 0 || length > 10_000_000) { // 防御：最大包体限制自己定
            throw new IOException("Invalid length: " + length);
        }

        int frameSize = HEADER_TOTAL_LENGTH + length;
        if (data.length < frameSize) return null; // 半包

        byte[] body = Arrays.copyOfRange(data, HEADER_TOTAL_LENGTH, frameSize);

        // 消费掉这一帧：把剩余数据重新放回 buffer
        byte[] remaining = Arrays.copyOfRange(data, frameSize, data.length);
        buffer.reset();
        buffer.write(remaining);

        return new InboundMessage((byte) type, (byte) version, body);
    }
    
}
