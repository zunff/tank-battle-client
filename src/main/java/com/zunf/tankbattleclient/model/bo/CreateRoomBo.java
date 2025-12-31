package com.zunf.tankbattleclient.model.bo;

public record CreateRoomBo(String roomName, int maxPlayers) {

    @Override
    public int maxPlayers() {
        return maxPlayers;
    }

    @Override
    public String roomName() {
        return roomName;
    }
}
