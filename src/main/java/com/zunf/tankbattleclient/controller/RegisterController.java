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
import com.zunf.tankbattleclient.service.remote.AuthService;

import java.io.IOException;

public class RegisterController {

    private AuthService authService = new AuthService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button registerButton;

    @FXML
    private Button backToLoginButton;

    @FXML
    private Label messageLabel;

    @FXML
    private VBox mainContainer;

    @FXML
    protected void onRegisterClick(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 简单验证
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("请填写所有字段");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("两次输入的密码不一致");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 调用注册服务
        boolean success = authService.register(username, password);
        if (success) {
            messageLabel.setText("注册成功！欢迎 " + username);
            messageLabel.setStyle("-fx-text-fill: green;");
            System.out.println("新用户 " + username + " 注册成功");
        } else {
            messageLabel.setText("注册失败，请重试");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    protected void onBackToLoginClick(ActionEvent event) {
        try {
            // 返回到登录页面
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zunf/tankbattleclient/login-view.fxml"));
            Parent root = fxmlLoader.load();
            
            Stage stage = (Stage) backToLoginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("无法返回登录页面");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
}