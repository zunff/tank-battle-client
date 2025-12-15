package com.zunf.tankbattleclient.manager;

import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.model.message.InboundMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

    private final AtomicInteger requestIdAtomic = new AtomicInteger(1);

    private GameConnectionManager(String host, int port) {
        super(host, port);
        msgCallbackEventManager = MsgCallbackEventManager.getInstance();
        requestCallbackEventManager = RequestCallbackEventManager.getInstance();
        System.out.println("创建游戏连接管理器");
    }

    public void send(GameMsgType type, MessageLite message) {
        byte[] body = message == null ? new byte[0] : message.toByteArray();
        int requestId  = requestIdAtomic.getAndIncrement();
        System.out.println("发送消息: " + type + " 请求ID: " + requestId);
        super.send((byte) type.getCode(), (byte) ConfigManager.getInstance().getProtocolVersion(), requestId, body);
    }

    public CompletableFuture<CommonProto.BaseResponse> sendAndListenFuture(GameMsgType type, MessageLite message) {
        return sendAndListenFuture(type, message, 5000);
    }

    public CompletableFuture<CommonProto.BaseResponse> sendAndListenFuture(GameMsgType type, MessageLite message, long timeoutMs) {

        int requestId = requestIdAtomic.getAndIncrement();

        CompletableFuture<CommonProto.BaseResponse> f = new CompletableFuture<>();

        requestCallbackEventManager.listenRequest(requestId, resp -> {
            // 这里在 messageExecutor 线程里触发（因为 onMessage 在 messageExecutor）
            f.complete(resp);
        });

        super.send((byte) type.getCode(), (byte) ConfigManager.getInstance().getProtocolVersion(), requestId, message.toByteArray());

        return f.orTimeout(timeoutMs, TimeUnit.MILLISECONDS).whenComplete((r, e) -> requestCallbackEventManager.removeListener(requestId));
    }

    @Override
    protected void onMessage(InboundMessage msg) {
        GameMsgType msgType = GameMsgType.of(msg.getType() & 0xFF);
        System.out.println("收到服务器消息: " + msgType + " 请求ID: " + msg.getRequestId());
        msgCallbackEventManager.triggerCallback(msgType, msg);
        requestCallbackEventManager.triggerCallback(msg.getRequestId(), msgType, msg);
    }
}
