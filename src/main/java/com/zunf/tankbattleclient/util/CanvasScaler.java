package com.zunf.tankbattleclient.util;

import com.zunf.tankbattleclient.constant.GameConstants;
import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

public class CanvasScaler {

    private final Canvas canvas;
    private final BorderPane container;
    private ChangeListener<Number> sizeListener;
    private Consumer<Void> onScaleChanged;

    public void setOnScaleChanged(Consumer<Void> onScaleChanged) {
        this.onScaleChanged = onScaleChanged;
    }

    public CanvasScaler(Canvas canvas, BorderPane container) {
        this.canvas = canvas;
        this.container = container;
    }

    public void initialize() {
        updateScale();
        sizeListener = (obs, oldVal, newVal) -> updateScale();
        container.widthProperty().addListener(sizeListener);
        container.heightProperty().addListener(sizeListener);
    }

    public void updateScale() {
        if (container == null || canvas == null) {
            return;
        }

        double containerWidth = container.getWidth();
        double containerHeight = container.getHeight();

        if (containerWidth <= 0 || containerHeight <= 0) {
            return;
        }

        double scaleX = containerWidth / GameConstants.CANVAS_SIZE;
        double scaleY = containerHeight / GameConstants.CANVAS_SIZE;
        double scaleValue = Math.min(scaleX, scaleY);

        double scaledWidth = GameConstants.CANVAS_SIZE * scaleValue;
        double scaledHeight = GameConstants.CANVAS_SIZE * scaleValue;

        canvas.setWidth(scaledWidth);
        canvas.setHeight(scaledHeight);

        StackPane parent = (StackPane) canvas.getParent();
        if (parent != null) {
            double offsetX = (containerWidth - scaledWidth) / 2;
            double offsetY = (containerHeight - scaledHeight) / 2;
            canvas.setLayoutX(offsetX);
            canvas.setLayoutY(offsetY);
        }

        if (onScaleChanged != null) {
            onScaleChanged.accept(null);
        }
    }

    public void cleanup() {
        if (container != null && sizeListener != null) {
            container.widthProperty().removeListener(sizeListener);
            container.heightProperty().removeListener(sizeListener);
        }
    }
}
