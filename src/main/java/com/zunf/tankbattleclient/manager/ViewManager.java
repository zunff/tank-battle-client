package com.zunf.tankbattleclient.manager;

import com.zunf.tankbattleclient.TankBattleApplication;
import com.zunf.tankbattleclient.controller.ViewLifecycle;
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

    public ViewManager(Stage stage) {
        this.stage = stage;

        stage.setOnCloseRequest(e -> {
            if (currentLifecycle != null) currentLifecycle.onClose();
        });
    }

    public void show(String fxml, String title, double w, double h) throws IOException {
        // 切换前：通知旧页面隐藏（释放资源）
        if (currentLifecycle != null) {
            currentLifecycle.onHide();
        }

        FXMLLoader loader = new FXMLLoader(TankBattleApplication.class.getResource("fxml/" + fxml));
        Parent root = loader.load();

        Object controller = loader.getController();
        currentLifecycle = (controller instanceof ViewLifecycle vl) ? vl : null;

        stage.setTitle(title);
        stage.setScene(new Scene(root, w, h));
        stage.show();

        if (currentLifecycle != null) {
            currentLifecycle.onShow();
        }
    }
}
