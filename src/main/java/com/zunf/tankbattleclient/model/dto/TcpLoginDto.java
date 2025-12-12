package com.zunf.tankbattleclient.model.dto;

public class TcpLoginDto {

    private String token;

    public TcpLoginDto(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
