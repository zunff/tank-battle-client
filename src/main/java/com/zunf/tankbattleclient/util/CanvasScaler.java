package com.zunf.tankbattleclient.util;

import com.zunf.tankbattleclient.constant.GameConstants;
import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;

import java.util.function.Consumer;

/**
 * Canvas缩放工具类
 * 负责Canvas的缩放和居中显示
 */
public class CanvasScaler {

    private final Canvas canvas;
    private final BorderPane container;
    private Scale scale;
    private ChangeListener<Number> sizeListener;
    private Consumer<Void> onScaleChanged;

    /**
     * 设置缩放变化回调
     */
    public void setOnScaleChanged(Consumer<Void> onScaleChanged) {
        this.onScaleChanged = onScaleChanged;
    }

    public CanvasScaler(Canvas canvas, BorderPane container) {
        this.canvas = canvas;
        this.container = container;
    }

    /**
     * 初始化Canvas大小和缩放
     */
    public void initialize() {
        // Canvas固定逻辑大小为1024x1024（32x32格子，每个32px）
        canvas.setWidth(GameConstants.CANVAS_SIZE);
        canvas.setHeight(GameConstants.CANVAS_SIZE);

        // 使用Scale变换来缩放Canvas以适应容器，保持1:1宽高比
        scale = new Scale();
        canvas.getTransforms().add(scale);

        // 监听容器大小变化，调整缩放比例
        sizeListener = (obs, oldVal, newVal) -> updateScale();
        container.widthProperty().addListener(sizeListener);
        container.heightProperty().addListener(sizeListener);

        // 初始设置缩放
        updateScale();
    }

    /**
     * 更新Canvas的缩放比例，使其适应容器并保持1:1宽高比
     */
    public void updateScale() {
        if (container == null || canvas == null) {
            return;
        }

        double containerWidth = container.getWidth();
        double containerHeight = container.getHeight();

        if (containerWidth <= 0 || containerHeight <= 0) {
            return;
        }

        // 计算缩放比例，保持1:1宽高比，适应容器
        double scaleX = containerWidth / GameConstants.CANVAS_SIZE;
        double scaleY = containerHeight / GameConstants.CANVAS_SIZE;
        double scaleValue = Math.min(scaleX, scaleY); // 取较小的值，确保完全显示

        scale.setX(scaleValue);
        scale.setY(scaleValue);
        scale.setPivotX(0);
        scale.setPivotY(0);

        // 调整Canvas在StackPane中的位置，使其居中
//        StackPane parent = (StackPane) canvas.getParent();
//        if (parent != null) {
//            // Canvas缩放后的实际大小
//            double scaledWidth = GameConstants.CANVAS_SIZE * scaleValue;
//            double scaledHeight = GameConstants.CANVAS_SIZE * scaleValue;
//
//            // 计算偏移量使Canvas居中
//            double offsetX = (containerWidth - scaledWidth) / 2;
//            double offsetY = (containerHeight - scaledHeight) / 2;
//
//            canvas.setLayoutX(offsetX);
//            canvas.setLayoutY(offsetY);
//        }

        // 调用缩放变化回调
        if (onScaleChanged != null) {
            onScaleChanged.accept(null);
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (container != null && sizeListener != null) {
            container.widthProperty().removeListener(sizeListener);
            container.heightProperty().removeListener(sizeListener);
        }
    }
}

