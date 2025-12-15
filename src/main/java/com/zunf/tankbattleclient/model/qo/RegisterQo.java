package com.zunf.tankbattleclient.model.qo;


public class RegisterQo {

    private String username;

    private String password;

    private String checkPassword;
    
    private String nickname;

    public RegisterQo(String username, String password, String checkPassword, String nickname) {
        this.username = username;
        this.password = password;
        this.checkPassword = checkPassword;
        this.nickname = nickname;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCheckPassword() {
        return checkPassword;
    }

    public String getNickname() {
        return nickname;
    }
}
