package com.zunf.tankbattleclient.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import com.zunf.tankbattleclient.service.AuthService;
import com.zunf.tankbattleclient.manager.GameConnectionManager;

import java.io.IOException;

public class LoginController {

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
            messageLabel.setText("登录成功！欢迎 " + username);
            messageLabel.setStyle("-fx-text-fill: green;");
            System.out.println("用户 " + username + " 登录成功，token: " + token);
            
            // 连接游戏服务器并发送登录消息
            gameConnectionManager.connectAndLogin(token, response -> {
                if (response.getCode() == 0) {
                    // 登录成功
                    messageLabel.setText("登录成功！欢迎 " + username);
                    messageLabel.setStyle("-fx-text-fill: green;");
                    System.out.println("用户 " + username + " 登录成功，player_id: " + response.getPlayerId());
                    // 这里可以添加跳转到游戏主界面的逻辑
                } else {
                    // 登录失败
                    messageLabel.setText("登录失败: " + response.getMessage());
                    messageLabel.setStyle("-fx-text-fill: red;");
                    System.err.println("用户 " + username + " 登录失败: " + response.getMessage());
                }
            });
            
            // 登录成功后的操作可以在这里添加
        } else {
            messageLabel.setText("登录失败，请检查用户名和密码");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    protected void onRegisterClick(ActionEvent event) {
        try {
            // 跳转到注册页面
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zunf/tankbattleclient/register-view.fxml"));
            Parent root = fxmlLoader.load();
            
            Stage stage = (Stage) registerButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("无法打开注册页面");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
}
