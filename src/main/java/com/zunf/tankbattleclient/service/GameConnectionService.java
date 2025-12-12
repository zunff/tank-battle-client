package com.zunf.tankbattleclient.service;

import cn.hutool.json.JSONUtil;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.manager.TcpClientManager;
import com.zunf.tankbattleclient.model.dto.TcpLoginDto;
import com.zunf.tankbattleclient.proto.GameProto;

import java.io.IOException;

public class GameConnectionService {
    private static final String SERVER_HOST = "localhost"; // TODO: 从配置文件读取
    private static final int SERVER_PORT = 8888; // TODO: 从配置文件读取
    
    private TcpClientManager tcpClientManager;
    
    public void connectAndLogin(String token) {
        try {
            // 创建TCP客户端管理器
            tcpClientManager = new TcpClientManager(SERVER_HOST, SERVER_PORT);
            
            // 连接到服务器
            tcpClientManager.connect();
            
            // 发送登录消息
            sendLoginMessage(token);
            
            System.out.println("成功连接到游戏服务器并发送登录消息");
        } catch (IOException e) {
            System.err.println("连接游戏服务器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendLoginMessage(String token) {
        // 创建登录DTO
        byte[] body = GameProto.LoginRequest.newBuilder()
                .setToken(token)
                .build()
                .toByteArray();

        tcpClientManager.send((byte) GameMsgType.LOGIN.getCode(), (byte) 1, body);
    }
    
    public void disconnect() {
        if (tcpClientManager != null) {
            tcpClientManager.close();
        }
    }
}