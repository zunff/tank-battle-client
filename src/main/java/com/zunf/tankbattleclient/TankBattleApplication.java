package com.zunf.tankbattleclient;

import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.manager.ViewManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class TankBattleApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        ViewManager viewManager = ViewManager.newInstance(stage);
        viewManager.show(ViewEnum.LOGIN);
    }

    public static void main(String[] args) {
        launch();
    }
}