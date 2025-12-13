package com.zunf.tankbattleclient;

import com.zunf.tankbattleclient.manager.ViewManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class TankBattleApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        ViewManager viewManager = ViewManager.newInstance(stage);
        viewManager.show("login-view.fxml", "坦克大战 - 登录", 350, 400);
    }

    public static void main(String[] args) {
        launch();
    }
}