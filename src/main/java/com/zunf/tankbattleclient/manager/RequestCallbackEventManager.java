package com.zunf.tankbattleclient.manager;

import cn.hutool.core.lang.Pair;
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

    public void triggerCallback(int requestId, InboundMessage message) {
        Consumer<CommonProto.BaseResponse> callback = requestCallbacks.get(requestId);
        if (callback != null) {
            CommonProto.BaseResponse baseResponse;
            try {
                baseResponse = CommonProto.BaseResponse.parseFrom(message.getBody());
            } catch (Exception e) {
                System.out.println("RequestCallbackEventManager 解析消息失败: " + e.getMessage());
                // 移除监听
                removeListener(requestId);
                return;
            }
            // 因为是请求-响应式的，accept方法里需要baseResponse，没法像msgType一样，把baseResponse的body解析成对应的protobuf对象
            // 所以需要accept方法里解析
            // 通常搭配CompletableFuture.complete()跟thenApply()使用，complete的是messageExecutor线程池线程
            // thenApply()就会在messageExecutor线程池线程里执行，所以这里可以解析baseResponse的body而不阻塞UI线程
            callback.accept(baseResponse);
            // 移除监听
            removeListener(requestId);
        }
    }
}
