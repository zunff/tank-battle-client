package com.zunf.tankbattleclient.enums;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.zunf.tankbattleclient.protobuf.game.auth.AuthProto;

import java.util.Arrays;

public enum GameMsgType {
    ERROR(0),
    LOGIN(1),
    LOGOUT(2),
    CHAT(3),
    MOVE(4),
    ATTACK(5),
    // ... 其他游戏相关类型

    UNKNOWN(255);

    private final int code;
    private Parser<? extends MessageLite> parser;

    public static GameMsgType of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst().orElse(UNKNOWN);
    }

    GameMsgType(int code) {
        this.code = code;
        this.parser = null;
    }

    GameMsgType(int code, Parser<? extends MessageLite> parser) {
        this.code = code;
        this.parser = parser;
    }

    public int getCode() {
        return code;
    }

    public Parser<? extends MessageLite> getParser() {
        return parser;
    }
}
