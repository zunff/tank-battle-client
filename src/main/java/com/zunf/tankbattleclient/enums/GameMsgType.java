package com.zunf.tankbattleclient.enums;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;

import java.util.Arrays;

public enum GameMsgType {
    // client -> server
    PING(1),
    LOGIN(2),
    LOGOUT(3),
    CREATE_ROOM(4),
    PAGE_ROOM(5),
    JOIN_ROOM(6),
    LEAVE_ROOM(7),

    // server -> client
    PONG(10001),
    PLAYER_JOIN_ROOM(10002, GameRoomProto.GameRoomPlayerData.parser()),
    PLAYER_LEAVE_ROOM(10003, GameRoomProto.GameRoomPlayerData.parser()),

    // common
    ERROR(0),
    UNKNOWN(20001);

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
