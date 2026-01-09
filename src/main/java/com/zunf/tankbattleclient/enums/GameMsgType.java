package com.zunf.tankbattleclient.enums;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.zunf.tankbattleclient.protobuf.game.auth.AuthProto;
import com.zunf.tankbattleclient.protobuf.game.match.MatchProto;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;

import java.util.Arrays;

public enum GameMsgType {
    // client -> server
    PING(1),
    LOGIN(2, AuthProto.LoginResponse.parser()),
    LOGOUT(3),
    CREATE_ROOM(4, GameRoomProto.CreateResponse.parser()),
    PAGE_ROOM(5, GameRoomProto.PageResponse.parser()),
    JOIN_ROOM(6, GameRoomProto.GameRoomDetail.parser()),
    LEAVE_ROOM(7),
    READY(8),
    START_GAME(9),
    LOADED_ACK(10),

    TANK_MOVE(11),
    TANK_SHOOT(12),
    LEAVE_MATCH(13),

    // server -> client
    PONG(10001),
    PLAYER_JOIN_ROOM(10002, GameRoomProto.PlayerInfo.parser()),
    PLAYER_LEAVE_ROOM(10003, GameRoomProto.PlayerInfo.parser()),
    PLAYER_READY(10004, GameRoomProto.PlayerInfo.parser()),
    GAME_STARTED(10005, GameRoomProto.StartNotice.parser()),
    GAME_TICK(10006, MatchProto.Tick.parser()),

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
