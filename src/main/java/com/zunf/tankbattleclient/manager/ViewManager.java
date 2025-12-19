package com.zunf.tankbattleclient.manager;

import com.zunf.tankbattleclient.TankBattleApplication;
import com.zunf.tankbattleclient.controller.ViewLifecycle;
import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.exception.BusinessException;
import com.zunf.tankbattleclient.exception.ErrorCode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewManager {

    private static volatile ViewManager INSTANCE;

    public static ViewManager newInstance(Stage stage) {
        if (INSTANCE == null) {
            synchronized (ViewManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ViewManager(stage);
                }
            }
        }
        return INSTANCE;
    }

    public static ViewManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("请先调用 getInstance(Stage) 创建实例");
        }
        return INSTANCE;
    }

    private final Stage stage;
    private ViewLifecycle currentLifecycle;
    private Object viewData;

    public ViewManager(Stage stage) {
        this.stage = stage;

        stage.setOnCloseRequest(e -> {
            if (currentLifecycle != null) currentLifecycle.onClose();
        });
    }

    /**
     * 获取当前主窗口 Stage
     * @return 主窗口 Stage
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * 显示视图（使用枚举）
     * @param viewEnum 视图类型枚举
     */
    public void show(ViewEnum viewEnum) {
        show(viewEnum, null);
    }

    /**
     * 显示视图（使用枚举，带参数）
     * @param viewEnum 视图类型枚举
     * @param data 传递给视图的数据
     */
    public void show(ViewEnum viewEnum, Object data) {
        this.viewData = data;
        show(viewEnum.getFxmlFileName(), viewEnum.getTitle(), viewEnum.getWidth(), viewEnum.getHeight());
    }

    /**
     * 显示视图
     * @param fxml FXML文件名
     * @param title 窗口标题
     * @param w 窗口宽度
     * @param h 窗口高度
     */
    public void show(String fxml, String title, double w, double h) {
        // 切换前：通知旧页面隐藏（释放资源）
        if (currentLifecycle != null) {
            currentLifecycle.onHide();
        }

        FXMLLoader loader = new FXMLLoader(TankBattleApplication.class.getResource("fxml/" + fxml));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.UNKNOWN_ERROR, "无法加载 FXML 文件：" + fxml);
        }

        Object controller = loader.getController();
        currentLifecycle = (controller instanceof ViewLifecycle vl) ? vl : null;

        stage.setTitle(title);
        stage.setScene(new Scene(root, w, h));
        stage.show();

        if (currentLifecycle != null) {
            currentLifecycle.onShow(viewData);
            viewData = null; // 使用后清空，避免影响下次跳转
        }
    }
}
