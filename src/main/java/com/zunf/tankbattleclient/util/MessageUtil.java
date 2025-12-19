package com.zunf.tankbattleclient.util;

import com.zunf.tankbattleclient.manager.ViewManager;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * 消息提示工具类
 * 支持不同类型的消息提示（成功、错误、信息等）
 */
public class MessageUtil {

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        SUCCESS,    // 成功 - 绿色
        ERROR,      // 错误 - 红色
        WARNING,    // 警告 - 橙色
        INFO        // 信息 - 蓝色
    }

    /**
     * 显示消息提示
     * @param message 消息内容
     * @param type 消息类型
     */
    public static void show(String message, MessageType type) {
        Platform.runLater(() -> {
            try {
                Stage mainStage = ViewManager.getInstance().getStage();
                if (mainStage == null) {
                    // 如果无法获取主窗口，使用屏幕中心
                    showOnScreenCenter(message, type);
                    return;
                }
                
                Stage toastStage = new Stage();
                toastStage.initStyle(StageStyle.TRANSPARENT);
                toastStage.setAlwaysOnTop(true);
                toastStage.initOwner(mainStage);
                toastStage.setResizable(false);

                Label label = new Label(message);
                label.setStyle(getStyleForType(type));
                label.setWrapText(true);
                label.setMaxWidth(350);
                label.setPadding(new Insets(5));

                StackPane root = new StackPane(label);
                root.setStyle(getBackgroundStyleForType(type));
                root.setAlignment(Pos.CENTER);
                root.setPadding(new Insets(12, 20, 12, 20));
                
                // 添加阴影效果
                DropShadow shadow = new DropShadow();
                shadow.setColor(Color.rgb(0, 0, 0, 0.3));
                shadow.setRadius(10);
                shadow.setOffsetX(0);
                shadow.setOffsetY(2);
                root.setEffect(shadow);

                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                toastStage.setScene(scene);

                // 等待窗口尺寸计算完成后再设置位置
                root.widthProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() > 0) {
                        updateToastPosition(toastStage, mainStage, newVal.doubleValue());
                    }
                });
                
                // 立即尝试设置位置（如果尺寸已确定）
                Platform.runLater(() -> {
                    if (root.getWidth() > 0) {
                        updateToastPosition(toastStage, mainStage, root.getWidth());
                    } else {
                        // 如果尺寸未确定，使用估算值
                        updateToastPosition(toastStage, mainStage, 350);
                    }
                });

                // 淡入淡出动画
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                PauseTransition pause = new PauseTransition(Duration.millis(2000));

                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);

                SequentialTransition seqTransition = new SequentialTransition(fadeIn, pause, fadeOut);
                seqTransition.setOnFinished(e -> toastStage.close());
                seqTransition.play();

                toastStage.show();
            } catch (Exception e) {
                // 如果获取主窗口失败，使用屏幕中心
                showOnScreenCenter(message, type);
            }
        });
    }

    /**
     * 更新 Toast 窗口位置（窗口中间顶部）
     */
    private static void updateToastPosition(Stage toastStage, Stage mainStage, double toastWidth) {
        double mainX = mainStage.getX();
        double mainY = mainStage.getY();
        double mainWidth = mainStage.getWidth();
        double mainHeight = mainStage.getHeight();
        
        // 窗口正中间顶部，距离顶部 10% 的位置
        toastStage.setX(mainX + (mainWidth - toastWidth) / 2);
        toastStage.setY(mainY + mainHeight * 0.1);
    }

    /**
     * 在屏幕中心显示消息（备用方案）
     */
    private static void showOnScreenCenter(String message, MessageType type) {
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);
        toastStage.setResizable(false);

        Label label = new Label(message);
        label.setStyle(getStyleForType(type));
        label.setWrapText(true);
        label.setMaxWidth(350);
        label.setPadding(new Insets(5));

        StackPane root = new StackPane(label);
        root.setStyle(getBackgroundStyleForType(type));
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(12, 20, 12, 20));
        
        // 添加阴影效果
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(10);
        shadow.setOffsetX(0);
        shadow.setOffsetY(2);
        root.setEffect(shadow);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        // 计算位置（屏幕中央）
        double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
        toastStage.setX((screenWidth - 350) / 2);
        toastStage.setY(screenHeight * 0.3);

        // 淡入淡出动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.millis(2000));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seqTransition = new SequentialTransition(fadeIn, pause, fadeOut);
        seqTransition.setOnFinished(e -> toastStage.close());
        seqTransition.play();

        toastStage.show();
    }

    /**
     * 显示成功消息
     */
    public static void showSuccess(String message) {
        show(message, MessageType.SUCCESS);
    }

    /**
     * 显示错误消息
     */
    public static void showError(String message) {
        show(message, MessageType.ERROR);
    }

    /**
     * 显示警告消息
     */
    public static void showWarning(String message) {
        show(message, MessageType.WARNING);
    }

    /**
     * 显示信息消息
     */
    public static void showInfo(String message) {
        show(message, MessageType.INFO);
    }

    /**
     * 根据消息类型获取文字样式
     */
    private static String getStyleForType(MessageType type) {
        return switch (type) {
            case SUCCESS -> "-fx-text-fill: #27ae60; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;";
            case ERROR -> "-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;";
            case WARNING -> "-fx-text-fill: #f39c12; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;";
            case INFO -> "-fx-text-fill: #3498db; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;";
        };
    }

    /**
     * 根据消息类型获取背景样式
     */
    private static String getBackgroundStyleForType(MessageType type) {
        return switch (type) {
            case SUCCESS -> "-fx-background-color: rgba(39, 174, 96, 0.15); -fx-background-radius: 8; -fx-border-color: rgba(39, 174, 96, 0.3); -fx-border-width: 1px; -fx-border-radius: 8;";
            case ERROR -> "-fx-background-color: rgba(231, 76, 60, 0.15); -fx-background-radius: 8; -fx-border-color: rgba(231, 76, 60, 0.3); -fx-border-width: 1px; -fx-border-radius: 8;";
            case WARNING -> "-fx-background-color: rgba(243, 156, 18, 0.15); -fx-background-radius: 8; -fx-border-color: rgba(243, 156, 18, 0.3); -fx-border-width: 1px; -fx-border-radius: 8;";
            case INFO -> "-fx-background-color: rgba(52, 152, 219, 0.15); -fx-background-radius: 8; -fx-border-color: rgba(52, 152, 219, 0.3); -fx-border-width: 1px; -fx-border-radius: 8;";
        };
    }
}

