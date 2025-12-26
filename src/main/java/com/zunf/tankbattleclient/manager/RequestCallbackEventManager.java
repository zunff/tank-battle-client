package com.zunf.tankbattleclient.manager;

import com.google.protobuf.InvalidProtocolBufferException;
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

    private final Map<Integer, Consumer<CommonProto.BaseResponse>> requestCallbacks = new ConcurrentHashMap<>();

    /**
     * 监听请求响应
     */
    public void listenRequest(int requestId, Consumer<CommonProto.BaseResponse> callback) {
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
     * 因为是请求-响应式的，accept方法里需要baseResponse，没法像msgType一样，把baseResponse的body解析成对应的protobuf对象
     * 所以需要accept方法里解析
     * 通常搭配CompletableFuture.complete()跟thenApply()使用，complete的是messageExecutor线程池线程
     * thenApply()就会在messageExecutor线程池线程里执行，所以这里可以解析baseResponse的body而不阻塞UI线程
     */
    public void triggerCallback(int requestId, InboundMessage message) {
        Consumer<CommonProto.BaseResponse> callback = requestCallbacks.get(requestId);
        if (callback == null) {
            return;
        }

        // 确保无论成功还是失败，都会移除监听器，避免内存泄漏
        try {
            CommonProto.BaseResponse baseResponse;
            try {
                baseResponse = CommonProto.BaseResponse.parseFrom(message.getBody());
            } catch (InvalidProtocolBufferException e) {
                System.err.println("RequestCallbackEventManager 解析消息失败: requestId=" + requestId + ", error=" + e.getMessage());
                removeListener(requestId);
                return;
            }

            // 执行回调，添加异常处理
            try {
                callback.accept(baseResponse);
            } catch (Exception e) {
                System.err.println("RequestCallbackEventManager 回调执行异常: requestId=" + requestId + ", error=" + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            // 确保监听器被移除，即使回调执行异常
            removeListener(requestId);
        }
    }
}
