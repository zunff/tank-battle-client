package com.zunf.tankbattleclient.manager;

import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.proto.AuthProto;

import java.io.IOException;

/**
 * 游戏连接管理器 单例
 *
 * @author zunf
 * @date 2025/12/12 23:08
 */
public final class GameConnectionManager {

    private static volatile GameConnectionManager INSTANCE;

    private TcpClientManager tcpClientManager;

    private GameConnectionManager() {}

    public static GameConnectionManager getInstance() {
        if (INSTANCE == null) {
            synchronized (GameConnectionManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GameConnectionManager();
                }
            }
        }
        return INSTANCE;
    }

    private void ensureConnected() throws IOException {
        if (tcpClientManager == null || !tcpClientManager.isConnected()) {
            synchronized (GameConnectionManager.class) {
                if (tcpClientManager == null || !tcpClientManager.isConnected()) {
                    String host = ConfigManager.getInstance().getServerHost();
                    int port = ConfigManager.getInstance().getServerPort();
                    tcpClientManager = new TcpClientManager(host, port);
                    tcpClientManager.connect();
                }
            }
        }
    }

    public void send(GameMsgType type, MessageLite message) throws IOException {
        ensureConnected();
        byte[] body = message == null ? new byte[0] : message.toByteArray();
        tcpClientManager.send((byte) type.getCode(), (byte) ConfigManager.getInstance().getProtocolVersion(), body);
    }

    public void connectAndLogin(String token) {
        try {
            ensureConnected();
            send(GameMsgType.LOGIN, AuthProto.LoginRequest.newBuilder().setToken(token).build());
            System.out.println("成功连接到游戏服务器并发送登录消息");
        } catch (IOException e) {
            System.err.println("连接或登录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
