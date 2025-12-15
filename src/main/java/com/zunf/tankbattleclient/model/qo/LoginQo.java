package com.zunf.tankbattleclient.model.qo;


public class LoginQo {

    private String username;

    private String password;

    public LoginQo(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
