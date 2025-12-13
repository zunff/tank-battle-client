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

    private final AuthService authService = new AuthService();
    private final GameConnectionManager gameConnectionManager = GameConnectionManager.getInstance();

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
            try {
                if (!gameConnectionManager.isConnected()) {
                    gameConnectionManager.connect();
                }
                gameConnectionManager.sendAndListen(GameMsgType.LOGIN, AuthProto.LoginRequest.newBuilder().setToken(token).build(), response -> {
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
                });
            } catch (IOException e) {
                messageLabel.setText("无法连接到服务器");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
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
