package com.zunf.tankbattleclient.manager;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.model.message.InboundMessage;
import com.zunf.tankbattleclient.util.ProtoBufUtil;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
    // 使用 Set 快速检查回调是否已存在，避免 O(n) 的 contains 操作
    private final Map<GameMsgType, Set<Consumer<MessageLite>>> callbackSets = new ConcurrentHashMap<>();

    /**
     * 监听消息
     */
    public void listenMessage(GameMsgType msgType, Consumer<MessageLite> callback) {
        // 使用 Set 快速检查重复（O(1)），而不是 List.contains()（O(n)）
        Set<Consumer<MessageLite>> set = callbackSets.computeIfAbsent(msgType, k -> ConcurrentHashMap.newKeySet());
        if (set.add(callback)) {
            // 只有当 callback 是新添加的时候，才添加到 List 中
            CopyOnWriteArrayList<Consumer<MessageLite>> list = responseCallbacks.computeIfAbsent(msgType, k -> new CopyOnWriteArrayList<>());
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
        Set<Consumer<MessageLite>> set = callbackSets.get(msgType);
        if (set != null) {
            set.remove(callback);
        }
    }

    /**
     * 收到消息时触发
     * 优化：批量执行回调，只创建一个 Platform.runLater 任务
     */
    public void triggerCallback(GameMsgType msgType, InboundMessage message) {
        List<Consumer<MessageLite>> callbacks = responseCallbacks.get(msgType);
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }

        CommonProto.BaseResponse baseResponse;
        try {
            baseResponse = CommonProto.BaseResponse.parseFrom(message.getBody());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("MsgCallbackEventManager 解析消息失败: msgType=" + msgType + ", error=" + e.getMessage());
            return;
        }

        // 在自定义线程池线程中，根据消息类型，解析消息体，避免阻塞 UI 线程
        MessageLite messageLite = ProtoBufUtil.parseRespBody(baseResponse, msgType.getParser());
        if (messageLite == null) {
            return;
        }

        // 优化：只创建一个 Platform.runLater 任务，批量执行所有回调
        // 这样可以减少 JavaFX 应用线程的调度负担，并保证回调按顺序执行
        final MessageLite finalMessage = messageLite; // 需要 final 或 effectively final
        final List<Consumer<MessageLite>> callbacksCopy = new CopyOnWriteArrayList<>(callbacks); // 创建快照，避免并发修改
        
        Platform.runLater(() -> {
            for (Consumer<MessageLite> callback : callbacksCopy) {
                try {
                    callback.accept(finalMessage);
                } catch (Exception e) {
                    // 单个回调异常不应影响其他回调的执行
                    System.err.println("MsgCallbackEventManager 回调执行异常: msgType=" + msgType + ", error=" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
}
