package com.zunf.tankbattleclient.manager;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.model.message.InboundMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 消息类型回调事件总线
 *
 * @author zunf
 * @date 2025/12/12 23:07
 */
public class MsgCallbackEventManager {

    private static volatile MsgCallbackEventManager INSTANCE;

    public static MsgCallbackEventManager getInstance() {
        if (INSTANCE == null) {
            synchronized (MsgCallbackEventManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MsgCallbackEventManager();
                }
            }
        }
        return INSTANCE;
    }

    // 回调映射表：消息类型 -> 多个回调函数，收到对应消息时执行对应回调
    private final Map<GameMsgType, CopyOnWriteArrayList<Consumer<MessageLite>>> responseCallbacks = new ConcurrentHashMap<>();

    /**
     * 监听消息
     */
    public void listenMessage(GameMsgType msgType, Consumer<MessageLite> callback) {
        CopyOnWriteArrayList<Consumer<MessageLite>> list = responseCallbacks.computeIfAbsent(msgType, k -> new CopyOnWriteArrayList<>());
        // 防重复：同一个 callback 实例不重复加入
        if (!list.contains(callback)) {
            list.add(callback);
        }
    }

    /**
     * 移除监听
     */
    public void removeListener(GameMsgType msgType, Consumer<MessageLite> callback) {
        CopyOnWriteArrayList<Consumer<MessageLite>> callbacks = responseCallbacks.get(msgType);
        if (callbacks != null) {
            callbacks.remove(callback);
        }
    }

    /**
     * 收到消息时触发
     */
    public void triggerCallback(GameMsgType msgType, InboundMessage message) {
        List<Consumer<MessageLite>> callbacks = responseCallbacks.get(msgType);
        if (callbacks != null) {
            MessageLite messageLite;
            try {
                messageLite = msgType.getRespMsg().parseFrom(message.getBody());
            } catch (InvalidProtocolBufferException e) {
                System.out.println("MsgCallbackEventManager 解析消息失败: " + e.getMessage());
                return;
            }
            // 触发回调
            for (Consumer<MessageLite> callback : callbacks) {
                callback.accept(messageLite);
            }
        }
    }
}
