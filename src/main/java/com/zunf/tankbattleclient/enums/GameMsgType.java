package com.zunf.tankbattleclient.enums;


import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.zunf.tankbattleclient.proto.AuthProto;

import java.util.Arrays;

public enum GameMsgType {
    ERROR(0, null),
    LOGIN(1, AuthProto.LoginResponse.parser()),
    LOGOUT(2, null),
    CHAT(3, null),
    MOVE(4, null),
    ATTACK(5, null),
    // ... 其他游戏相关类型

    UNKNOWN(255, null);

    private final int code;
    private final Parser<? extends MessageLite> respMsgParser;

    public static GameMsgType of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst().orElse(UNKNOWN);
    }

    GameMsgType(int code, Parser<? extends MessageLite> respMsg) {
        this.code = code;
        this.respMsgParser = respMsg;
    }

    public int getCode() {
        return code;
    }

    public Parser<? extends MessageLite> getRespMsg() {
        return respMsgParser;
    }
}
