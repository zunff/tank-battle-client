package com.zunf.tankbattleclient.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.zunf.tankbattleclient.exception.BusinessException;
import com.zunf.tankbattleclient.exception.ErrorCode;
import com.zunf.tankbattleclient.protobuf.CommonProto;

public class ProtoBufUtil {

    public static <T extends MessageLite> T parseRespBody(CommonProto.BaseResponse baseResponse, Class<T> clazz) {
        if (baseResponse.getCode() != 0) {
            System.out.println("请求失败：" + baseResponse.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        try {
            return (T) clazz.getMethod("parseFrom", ByteString.class).invoke(null, baseResponse.getPayloadBytes());
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.UNKNOWN_ERROR);
        }
    }

    public static <T extends MessageLite> T parseRespBody(CommonProto.BaseResponse baseResponse, Parser<T> parser) {
        if (baseResponse.getCode() != 0) {
            System.out.println("请求失败：" + baseResponse.getMessage());
            return null;
        }
        try {
            return parser.parseFrom(baseResponse.getPayloadBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}