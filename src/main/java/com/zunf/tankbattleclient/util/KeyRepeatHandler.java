package com.zunf.tankbattleclient.util;

import com.zunf.tankbattleclient.constant.GameConstants;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 按键重复处理工具类
 * 负责处理长按按键的重复触发逻辑
 */
public class KeyRepeatHandler {

    private final Set<KeyCode> pressedKeys;
    private Timeline keyRepeatTimeline;
    private final Consumer<KeyCode> onKeyRepeat;

    public KeyRepeatHandler(Set<KeyCode> pressedKeys, Consumer<KeyCode> onKeyRepeat) {
        this.pressedKeys = pressedKeys;
        this.onKeyRepeat = onKeyRepeat;
    }

    /**
     * 开始按键重复（持续移动）
     */
    public void start() {
        stop(); // 先停止之前的定时器

        keyRepeatTimeline = new Timeline(
                new KeyFrame(
                        Duration.millis(GameConstants.KEY_REPEAT_INTERVAL),
                        e -> {
                            KeyCode priorityKey = getPriorityKey();
                            if (priorityKey != null) {
                                onKeyRepeat.accept(priorityKey);
                            }
                        }));
        keyRepeatTimeline.setCycleCount(Animation.INDEFINITE);
        keyRepeatTimeline.play();
    }

    /**
     * 停止按键重复
     */
    public void stop() {
        if (keyRepeatTimeline != null) {
            keyRepeatTimeline.stop();
            keyRepeatTimeline = null;
        }
    }

    /**
     * 获取优先级最高的移动键（W > S > A > D）
     */
    private KeyCode getPriorityKey() {
        if (pressedKeys.contains(KeyCode.W)) {
            return KeyCode.W;
        } else if (pressedKeys.contains(KeyCode.S)) {
            return KeyCode.S;
        } else if (pressedKeys.contains(KeyCode.A)) {
            return KeyCode.A;
        } else if (pressedKeys.contains(KeyCode.D)) {
            return KeyCode.D;
        }
        return null;
    }

    /**
     * 检查是否有按下的移动键
     */
    public boolean hasPressedKeys() {
        return !pressedKeys.isEmpty();
    }
}

