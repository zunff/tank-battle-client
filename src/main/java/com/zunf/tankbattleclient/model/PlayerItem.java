package com.zunf.tankbattleclient.model;

/**
 * 玩家列表项
 * 用于在 ListView 中显示玩家信息
 */
public class PlayerItem {
    private final Long playerId;
    private final String nickname;
    private final boolean isCreator;
    private boolean ready;

    public PlayerItem(Long playerId, String nickname, boolean isCreator) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.isCreator = isCreator;
        this.ready = false;
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

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(nickname);
        if (isCreator) {
            sb.append(" [房主]");
        }
        if (ready) {
            sb.append(" [已准备]");
        }
        return sb.toString();
    }
}

