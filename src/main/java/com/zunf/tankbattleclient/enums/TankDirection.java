package com.zunf.tankbattleclient.enums;

public enum TankDirection {
    UP(0), // 上
    DOWN(1), // 下
    LEFT(2), // 左
    RIGHT(3); // 右

    private int code;

    TankDirection(int code) {
        this.code = code;
    }
}