package com.zunf.tankbattleclient.manager;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.handler.CycleAtomicInteger;
import com.zunf.tankbattleclient.model.bo.ResponseBo;
import com.zunf.tankbattleclient.model.message.InboundMessage;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
                    String host = ConfigManager.getInstance().getTcpServerHost();
                    int port = ConfigManager.getInstance().getTcpServerPort();
                    INSTANCE = new GameConnectionManager(host, port);
                }
            }
        }
        return INSTANCE;
    }

    private final MsgCallbackEventManager msgCallbackEventManager;

    private final RequestCallbackEventManager requestCallbackEventManager;


    private final CycleAtomicInteger requestIdGenerator = new CycleAtomicInteger();


    private GameConnectionManager(String host, int port) {
        super(host, port);
        msgCallbackEventManager = MsgCallbackEventManager.getInstance();
        requestCallbackEventManager = RequestCallbackEventManager.getInstance();
        System.out.println("创建游戏连接管理器");
    }


    public void send(GameMsgType type, MessageLite message) {
        byte[] body = message == null ? new byte[0] : message.toByteArray();
        int requestId = requestIdGenerator.getNextRequestId();
        System.out.println("发送消息: " + type + " 请求ID: " + requestId);
        super.send((byte) type.getCode(), (byte) ConfigManager.getInstance().getProtocolVersion(), requestId, body);
    }

    public void listenMessage(GameMsgType msgType, Consumer<MessageLite> callback) {
        msgCallbackEventManager.listenMessage(msgType, callback);
    }

    public void removeListener(GameMsgType msgType, Consumer<MessageLite> callback) {
        msgCallbackEventManager.removeListener(msgType, callback);
    }

    public CompletableFuture<ResponseBo> sendAndListenFuture(GameMsgType type, MessageLite message) {
        return sendAndListenFuture(type, message, 5000);
    }

    public CompletableFuture<ResponseBo> sendAndListenFuture(GameMsgType type, MessageLite message, long timeoutMs) {
        int requestId = requestIdGenerator.getNextRequestId();
        CompletableFuture<ResponseBo> f = new CompletableFuture<>();

        requestCallbackEventManager.listenRequest(requestId, responseBo -> {
            // 这里在 messageExecutor 线程里触发（因为 onMessage 在 messageExecutor）
            // 确保只完成一次，避免重复完成
            if (!f.isDone()) {
                f.complete(responseBo);
            }
        });

        super.send((byte) type.getCode(), (byte) ConfigManager.getInstance().getProtocolVersion(), requestId, message.toByteArray());

        // 确保超时或完成时都移除监听器，避免内存泄漏
        return f.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((r, e) -> {
                    // 无论成功、失败还是超时，都移除监听器
                    requestCallbackEventManager.removeListener(requestId);
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        System.out.println("请求超时: requestId=" + requestId + ", type=" + type);
                    }
                });
    }

    @Override
    protected void onMessage(InboundMessage msg) {
        GameMsgType msgType = GameMsgType.of(msg.getType());
        int requestId = msg.getRequestId();

        System.out.println("收到服务器消息: " + msgType + " 请求ID: " + requestId + " 原始MsgType: " + msg.getType());

        // 优化消息路由：优先处理请求-响应模式（requestId > 0）
        // 如果存在对应的请求监听器，说明这是请求-响应消息，优先处理
        // 否则作为广播消息处理
        if (requestId > 0 && requestCallbackEventManager.hasListener(requestId)) {
            // 请求-响应模式：只触发请求回调
            requestCallbackEventManager.triggerCallback(requestId, msg, msgType);
        } else {
            // 广播消息：触发类型回调
            msgCallbackEventManager.triggerCallback(msgType, msg);
        }
    }
}
