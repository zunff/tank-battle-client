package com.zunf.tankbattleclient.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zunf.tankbattleclient.exception.BusinessException;
import com.zunf.tankbattleclient.exception.ErrorCode;
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
        String backendUrl = configManager.getBackendServerUrl();

        // 构造请求参数
        LoginQo param = new LoginQo(username, password);

        // 发送POST请求到后端服务器
        HttpResponse response = HttpRequest.post(backendUrl + "/user/login")
                .body(JSONUtil.toJsonStr(param)).execute();

        // 处理响应
        if (!response.isOk() || !JSONUtil.isTypeJSON(response.body())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        JSONObject jsonResponse = JSONUtil.parseObj(response.body());
        if (jsonResponse.getInt("code", ErrorCode.UNKNOWN_ERROR.getCode()) != 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return jsonResponse.getStr("data");
    }

    public boolean register(String username, String password, String nickname, String confirmPassword) {
        String backendUrl = configManager.getBackendServerUrl();

        // 构造请求参数
        RegisterQo registerQo = new RegisterQo(username, password, confirmPassword, nickname);
        // 发送POST请求到后端服务器
        HttpResponse response = HttpRequest.post(backendUrl + "/user/register")
                .body(JSONUtil.toJsonStr(registerQo)).execute();

        // 处理响应
        if (!response.isOk() || !JSONUtil.isTypeJSON(response.body())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        JSONObject jsonResponse = JSONUtil.parseObj(response.body());
        return jsonResponse.getInt("code", ErrorCode.UNKNOWN_ERROR.getCode()) == 0;
    }
}
