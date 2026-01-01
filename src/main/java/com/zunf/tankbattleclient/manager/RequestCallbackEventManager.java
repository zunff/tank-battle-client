package com.zunf.tankbattleclient.manager;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.model.bo.ResponseBo;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.model.message.InboundMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 请求ID回调事件总线
 *
 * @author zunf
 * @date 2025/12/14 00:17
 */
public class RequestCallbackEventManager {

    private static volatile RequestCallbackEventManager INSTANCE;

    public static RequestCallbackEventManager getInstance() {
        if (INSTANCE == null) {
            synchronized (RequestCallbackEventManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RequestCallbackEventManager();
                }
            }
        }
        return INSTANCE;
    }

    private final Map<Integer, Consumer<ResponseBo>> requestCallbacks = new ConcurrentHashMap<>();

    /**
     * 监听请求响应
     */
    public void listenRequest(int requestId, Consumer<ResponseBo> callback) {
        requestCallbacks.put(requestId, callback);
    }

    /**
     * 移除监听
     */
    public void removeListener(int requestId) {
        requestCallbacks.remove(requestId);
    }

    /**
     * 检查是否存在指定 requestId 的监听器
     */
    public boolean hasListener(int requestId) {
        return requestCallbacks.containsKey(requestId);
    }

    /**
     * 收到消息时触发回调
     */
    public void triggerCallback(int requestId, InboundMessage message, GameMsgType type) {
        Consumer<ResponseBo> callback = requestCallbacks.get(requestId);
        if (callback == null) {
            return;
        }

        // 确保无论成功还是失败，都会移除监听器，避免内存泄漏
        try {
            CommonProto.BaseResponse baseResponse;
            try {
                baseResponse = CommonProto.BaseResponse.parseFrom(message.getBody());
            } catch (InvalidProtocolBufferException e) {
                System.err.println(
                        "RequestCallbackEventManager 解析消息失败: requestId=" + requestId + ", error=" + e.getMessage());
                removeListener(requestId);
                return;
            }
            ResponseBo responseBo = new ResponseBo(baseResponse);
            Parser<? extends MessageLite> parser = type.getParser();
            if (parser != null) {
                try {
                    MessageLite messageLite = parser.parseFrom(baseResponse.getPayloadBytes());
                    responseBo.setPayload(messageLite);
                } catch (InvalidProtocolBufferException e) {
                    System.out.println("RequestCallbackEventManager 解析消息体失败 " + e.getMessage());
                    removeListener(requestId);
                    return;
                }
            }

            // 执行回调，添加异常处理
            try {
                callback.accept(responseBo);
            } catch (Exception e) {
                System.err.println(
                        "RequestCallbackEventManager 回调执行异常: requestId=" + requestId + ", error=" + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            // 确保监听器被移除，即使回调执行异常
            removeListener(requestId);
        }
    }
}
