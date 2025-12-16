package com.zunf.tankbattleclient.manager;

/**
 * 用户信息缓存管理器
 * 单例模式，用于存储当前登录用户的信息
 */
public class UserInfoManager {
    private static volatile UserInfoManager INSTANCE;
    
    private String username;
    private String nickname;
    private Long playerId;
    
    private UserInfoManager() {}
    
    public static UserInfoManager getInstance() {
        if (INSTANCE == null) {
            synchronized (UserInfoManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserInfoManager();
                }
            }
        }
        return INSTANCE;
    }
    
    public void setUserinfo(String username, String nickname, Long playerId) {
        this.username = username;
        this.nickname = nickname;
        this.playerId = playerId;
    }
    
    public void clearUserinfo() {
        this.username = null;
        this.nickname = null;
        this.playerId = null;
    }
    
    // Getters
    public String getUsername() {
        return username;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public Long getPlayerId() {
        return playerId;
    }

}