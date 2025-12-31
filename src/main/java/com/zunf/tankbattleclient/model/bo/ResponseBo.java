package com.zunf.tankbattleclient.model.bo;

import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.protobuf.CommonProto;

public class ResponseBo {

    private CommonProto.BaseResponse response;

    private MessageLite payload;

    public CommonProto.BaseResponse getResponse() {
        return response;
    }

    public MessageLite getPayload() {
        return payload;
    }

    public void setPayload(MessageLite payload) {
        this.payload = payload;
    }

    public ResponseBo(CommonProto.BaseResponse response) {
        this.response = response;
    }
}
