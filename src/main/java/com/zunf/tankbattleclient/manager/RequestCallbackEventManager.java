package com.zunf.tankbattleclient.manager;

import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.enums.GameMsgType;
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

    private final Map<Integer, Consumer<CommonProto.BaseResponse>> requestCallbacks = new ConcurrentHashMap<>();

    public void listenRequest(int requestId, Consumer<CommonProto.BaseResponse> callback) {
        requestCallbacks.put(requestId, callback);
    }

    public void removeListener(int requestId) {
        requestCallbacks.remove(requestId);
    }

    public void triggerCallback(int requestId, GameMsgType msgType, InboundMessage message) {
        Consumer<CommonProto.BaseResponse> callback = requestCallbacks.get(requestId);
        if (callback != null) {
            CommonProto.BaseResponse baseResponse;
            try {
                baseResponse = CommonProto.BaseResponse.parseFrom(message.getBody());
            } catch (Exception e) {
                System.out.println("RequestCallbackEventManager 解析消息失败: " + e.getMessage());
                return;
            }
            callback.accept(baseResponse);
            // 移除监听
            removeListener(requestId);
        }
    }
}
