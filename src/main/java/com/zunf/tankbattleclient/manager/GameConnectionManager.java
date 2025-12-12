package com.zunf.tankbattleclient.manager;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.model.message.InboundMessage;
import com.zunf.tankbattleclient.proto.AuthProto;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 游戏连接管理器 单例
 *
 * @author zunf
 * @date 2025/12/12 23:08
 */
public final class GameConnectionManager extends TcpClientManager {

    private static volatile GameConnectionManager INSTANCE;

    
    // 回调映射表：消息类型 -> 回调函数
    private final Map<GameMsgType, Consumer<? extends MessageLite>> responseCallbacks = new ConcurrentHashMap<>();
    
    // 消息解析器映射表：消息类型 -> ProtoParser
    private final Map<GameMsgType, Parser<? extends MessageLite>> messageParsers = new ConcurrentHashMap<>();

    /**
     * 初始化消息解析器映射表
     */
    private void initMessageParsers() {
        // 注册消息解析器
        messageParsers.put(GameMsgType.LOGIN, AuthProto.LoginResponse.parser());
        // 可以在这里添加其他消息类型的解析器
    }

    private GameConnectionManager(String host, int port) {
        super(host, port);
        initMessageParsers();
    }

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
    
    /**
     * 添加响应回调
     */
    public <T extends MessageLite> void addResponseCallback(GameMsgType msgType, Parser<T> parser, Consumer<T> callback) {
        messageParsers.put(msgType, parser);
        responseCallbacks.put(msgType, callback);
    }
    
    /**
     * 移除响应回调
     */
    public void removeResponseCallback(GameMsgType msgType) {
        responseCallbacks.remove(msgType);
    }

    public void send(GameMsgType type, MessageLite message) throws IOException {
        send(type, message, null);
    }
    
    /**
     * 通用消息发送方法，支持回调处理
     */
    public <T extends MessageLite> void send(GameMsgType type, MessageLite message, Consumer<T> callback) throws IOException {
        if (!isConnected()) {
            connect();
        }
        
        // 如果提供了回调，则注册
        if (callback != null) {
            responseCallbacks.put(type, callback);
        }
        
        byte[] body = message == null ? new byte[0] : message.toByteArray();
        send((byte) type.getCode(), (byte) ConfigManager.getInstance().getProtocolVersion(), body);
        System.out.println("发送消息: " + type + ", 长度: " + body.length);
    }

    public void connectAndLogin(String token, Consumer<AuthProto.LoginResponse> callback) {
        try {
            if (!isConnected()) {
                connect();
            }
            // 使用通用send方法发送登录消息
            send(GameMsgType.LOGIN, AuthProto.LoginRequest.newBuilder().setToken(token).build(), callback);
            System.out.println("成功连接到游戏服务器并发送登录消息");
        } catch (IOException e) {
            System.err.println("连接或登录失败: " + e.getMessage());
            e.printStackTrace();
            if (callback != null) {
                // 创建一个登录失败的响应对象
                AuthProto.LoginResponse response = AuthProto.LoginResponse.newBuilder()
                        .setCode(-1)
                        .setMessage("连接服务器失败: " + e.getMessage())
                        .build();
                callback.accept(response);
            }
        }
    }

    @Override
    protected void onMessage(InboundMessage msg) {
        GameMsgType msgType = GameMsgType.of(msg.getType() & 0xFF);
        System.out.println("收到服务器消息: " + msgType);
        
        try {
            // 获取对应的消息解析器
            Parser<? extends MessageLite> parser = messageParsers.get(msgType);
            if (parser != null) {
                // 解析消息体
                MessageLite response = parser.parseFrom(msg.getBody());
                System.out.println("解析消息成功: " + response);
                
                // 获取对应的回调函数
                Consumer<? extends MessageLite> callback = responseCallbacks.get(msgType);
                if (callback != null) {
                    // 调用回调函数处理响应
                    @SuppressWarnings("unchecked")
                    Consumer<MessageLite> typedCallback = (Consumer<MessageLite>) callback;
                    typedCallback.accept(response);
                    
                    // 移除回调，避免内存泄漏
                    responseCallbacks.remove(msgType);
                }
            } else {
                System.out.println("未找到消息解析器: " + msgType);
            }
        } catch (Exception e) {
            System.err.println("解析消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
