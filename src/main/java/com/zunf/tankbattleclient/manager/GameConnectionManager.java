package com.zunf.tankbattleclient.manager;

import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.common.CommonProto;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.model.message.InboundMessage;
import com.zunf.tankbattleclient.protobuf.game.auth.AuthProto;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 游戏连接管理器 单例
 *
 * @author zunf
 * @date 2025/12/12 23:08
 */
public final class GameConnectionManager extends TcpClientManager {

    private static volatile GameConnectionManager INSTANCE;

    public static GameConnectionManager getInstance() {
        if (INSTANCE == null) {
            synchronized (GameConnectionManager.class) {
                if (INSTANCE == null) {
                    String host = ConfigManager.getInstance().getServerHost();
                    int port = ConfigManager.getInstance().getServerPort();
                    INSTANCE = new GameConnectionManager(host, port);
                }
            }
        }
        return INSTANCE;
    }

    private final MsgCallbackEventManager msgCallbackEventManager;

    private final RequestCallbackEventManager requestCallbackEventManager;

    private final AtomicInteger requestIdAtomic = new AtomicInteger(1);

    private GameConnectionManager(String host, int port) {
        super(host, port);
        msgCallbackEventManager = MsgCallbackEventManager.getInstance();
        requestCallbackEventManager = RequestCallbackEventManager.getInstance();
        System.out.println("创建游戏连接管理器");
    }

    public void send(GameMsgType type, MessageLite message) throws IOException {
        byte[] body = message == null ? new byte[0] : message.toByteArray();
        int requestId  = requestIdAtomic.getAndIncrement();
        System.out.println("发送消息: " + type + " 请求ID: " + requestId);
        super.send((byte) type.getCode(), (byte) ConfigManager.getInstance().getProtocolVersion(), requestId, body);
    }

    public void sendAndListen(GameMsgType type, MessageLite message, Consumer<MessageLite> callback) throws IOException {
        int requestId = requestIdAtomic.getAndIncrement();
        System.out.println("发送消息: " + type + " 监听ID: " + requestId);
        requestCallbackEventManager.listenRequest(requestId, callback);
        super.send((byte) type.getCode(), (byte) ConfigManager.getInstance().getProtocolVersion(), requestId, message.toByteArray());
    }


    public void connectAndLogin(String token) {
        try {
            if (!isConnected()) {
                connect();
            }
            // 使用通用send方法发送登录消息
            send(GameMsgType.LOGIN, AuthProto.LoginRequest.newBuilder().setToken(token).build());
            System.out.println("成功连接到游戏服务器并发送登录消息");
        } catch (IOException e) {
            System.err.println("连接或登录失败: " + e.getMessage());
        }
    }

    @Override
    protected void onMessage(InboundMessage msg) {
        GameMsgType msgType = GameMsgType.of(msg.getType() & 0xFF);
        System.out.println("收到服务器消息: " + msgType + " 请求ID: " + msg.getRequestId());
        msgCallbackEventManager.triggerCallback(msgType, msg);
        requestCallbackEventManager.triggerCallback(msg.getRequestId(), msgType, msg);
    }
}
