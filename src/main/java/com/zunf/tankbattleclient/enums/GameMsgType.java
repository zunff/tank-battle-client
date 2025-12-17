package com.zunf.tankbattleclient.enums;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

import java.util.Arrays;

public enum GameMsgType {
    ERROR(0),
    LOGIN(1),
    LOGOUT(2),
    CREATE_ROOM(3),

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
