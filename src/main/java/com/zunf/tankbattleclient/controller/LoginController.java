package com.zunf.tankbattleclient.controller;

import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.manager.GameConnectionManager;
import com.zunf.tankbattleclient.manager.MsgCallbackEventManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.proto.AuthProto;
import com.zunf.tankbattleclient.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.function.Consumer;

public class LoginController extends ViewLifecycle {

    @Override
    public void onShow() {
        // 只注册一次
        msgCallbackEventManager.listenMessage(GameMsgType.LOGIN, loginCallback);
        System.out.println("注册登录回调");
    }

    @Override
    public void onHide() {
        // 取消注册
        msgCallbackEventManager.removeListener(GameMsgType.LOGIN, loginCallback);
        System.out.println("取消登录回调");
    }

    private final AuthService authService = new AuthService();
    private final GameConnectionManager gameConnectionManager = GameConnectionManager.getInstance();
    private final MsgCallbackEventManager msgCallbackEventManager = MsgCallbackEventManager.getInstance();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label messageLabel;

    @FXML
    private VBox mainContainer;

    private final Consumer<MessageLite> loginCallback = response -> {
        String username = usernameField.getText();
        AuthProto.LoginResponse loginResponse = (AuthProto.LoginResponse) response;
        if (loginResponse.getCode() == 0) {
            // 登录成功
            messageLabel.setText("登录成功！欢迎 " + username);
            messageLabel.setStyle("-fx-text-fill: green;");
            System.out.println("用户 " + username + " 登录成功，player_id: " + loginResponse.getPlayerId());
            // 这里可以添加跳转到游戏主界面的逻辑
        } else {
            // 登录失败
            messageLabel.setText("登录失败: " + loginResponse.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
            System.err.println("用户 " + username + " 登录失败: " + loginResponse.getMessage());
        }
    };


    @FXML
    protected void onLoginClick(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 简单验证
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("请输入用户名和密码");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 使用AuthService进行登录
        String token = authService.login(username, password);
        if (token != null) {
            // 连接游戏服务器并发送登录消息
            gameConnectionManager.connectAndLogin(token);
        } else {
            messageLabel.setText("登录失败，请检查用户名和密码");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    protected void onRegisterClick(ActionEvent event) {
        try {
            // 跳转到注册页面
            ViewManager.getInstance().show("register-view.fxml", "注册", 300, 400);
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("无法打开注册页面");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
}
