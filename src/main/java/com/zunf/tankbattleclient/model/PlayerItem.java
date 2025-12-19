package com.zunf.tankbattleclient.model;

/**
 * 玩家列表项
 * 用于在 ListView 中显示玩家信息
 */
public class PlayerItem {
    private final Long playerId;
    private final String nickname;
    private final boolean isCreator;

    public PlayerItem(Long playerId, String nickname, boolean isCreator) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.isCreator = isCreator;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isCreator() {
        return isCreator;
    }

    @Override
    public String toString() {
        return nickname + (isCreator ? " [房主]" : "");
    }
}

