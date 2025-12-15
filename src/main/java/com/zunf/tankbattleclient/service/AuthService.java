package com.zunf.tankbattleclient.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zunf.tankbattleclient.manager.ConfigManager;
import com.zunf.tankbattleclient.model.qo.LoginQo;
import com.zunf.tankbattleclient.model.qo.RegisterQo;

public class AuthService {

    private static volatile AuthService INSTANCE;
    private final ConfigManager configManager = ConfigManager.getInstance();

    public static AuthService getInstance() {
        if (INSTANCE == null) {
            synchronized (AuthService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AuthService();
                }
            }
        }
        return INSTANCE;
    }


    public String login(String username, String password) {
        // 实际登录实现，向后端服务器发送请求
        try {
            String backendUrl = configManager.getBackendServerUrl();
            
            // 构造请求参数
            LoginQo param = new LoginQo(username, password);
            
            // 发送POST请求到后端服务器
            HttpResponse response = HttpRequest.post(backendUrl + "/user/login")
                    .body(JSONUtil.toJsonStr(param)).execute();
            
            // 处理响应
            if (response.isOk()) {
                String responseBody = response.body();
                if (JSONUtil.isTypeJSON(responseBody)) {
                    JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
                    if (jsonResponse.getInt("code", -1) == 0) {
                        return jsonResponse.getStr("data");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean register(String username, String password, String nickname, String confirmPassword) {
        // 实际注册实现，向后端服务器发送请求
        try {
            String backendUrl = configManager.getBackendServerUrl();
            
            // 构造请求参数
            RegisterQo registerQo = new RegisterQo(username, password, confirmPassword, nickname);
            // 发送POST请求到后端服务器
            HttpResponse response = HttpRequest.post(backendUrl + "/user/register")
                    .body(JSONUtil.toJsonStr(registerQo)).execute();
            
            // 处理响应
            if (response.isOk()) {
                String responseBody = response.body();
                if (JSONUtil.isTypeJSON(responseBody)) {
                    JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
                    return jsonResponse.getInt("code", -1) == 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
