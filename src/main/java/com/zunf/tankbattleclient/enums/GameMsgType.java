package com.zunf.tankbattleclient.enums;

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

    public static GameMsgType of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst().orElse(UNKNOWN);
    }

    GameMsgType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
