package com.zunf.tankbattleclient.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.protobuf.CommonProto;

public class ProtoBufUtil {

    public static <T extends MessageLite> T parseRespBody(CommonProto.BaseResponse baseResponse, Class<T> clazz) {
        if (baseResponse.getCode() != 0) {
            System.out.println("请求失败：" + baseResponse.getMessage());
            return null;
        }
        try {
            return (T) clazz.getMethod("parseFrom", ByteString.class).invoke(null, baseResponse.getPayloadBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}