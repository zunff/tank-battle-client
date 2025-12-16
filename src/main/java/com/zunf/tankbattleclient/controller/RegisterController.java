package com.zunf.tankbattleclient.controller;

import cn.hutool.core.util.StrUtil;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.manager.GameConnectionManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.protobuf.game.auth.AuthProto;
import com.zunf.tankbattleclient.service.AuthService;
import com.zunf.tankbattleclient.ui.AsyncButton;
import com.zunf.tankbattleclient.util.ProtoBufUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class RegisterController {

    private final AuthService authService = AuthService.getInstance();

    private final GameConnectionManager gameConnectionManager = GameConnectionManager.getInstance();

    @FXML
    private TextField usernameField;

    @FXML
    private TextField nicknameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private AsyncButton registerButton;

    @FXML
    private Button backToLoginButton;

    @FXML
    private Label messageLabel;

    @FXML
    private VBox mainContainer;

    @FXML
    public void initialize() {
        registerButton.setAction(() -> {
            String username = usernameField.getText();
            String nickname = nicknameField.getText();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            
            // 清除之前的样式
            clearFieldStyles();
            StringBuilder errorMsg = new StringBuilder();
            
            // 输入验证
            if (username.isEmpty()) {
                usernameField.setStyle("-fx-border-color: red;");
                errorMsg.append("用户名不能为空 ");
            } else if (username.length() < 3 || username.length() > 20) {
                usernameField.setStyle("-fx-border-color: red;");
                errorMsg.append("用户名长度必须在3-20个字符之间 ");
            }
            
            if (nickname.isEmpty()) {
                nicknameField.setStyle("-fx-border-color: red;");
                errorMsg.append("昵称不能为空 ");
            }
            
            if (password.isEmpty()) {
                passwordField.setStyle("-fx-border-color: red;");
                errorMsg.append("密码不能为空 ");
            } else if (password.length() < 6 || password.length() > 20) {
                passwordField.setStyle("-fx-border-color: red;");
                errorMsg.append("密码长度必须在6-20个字符之间 ");
            }
            
            if (confirmPassword.isEmpty()) {
                confirmPasswordField.setStyle("-fx-border-color: red;");
                errorMsg.append("确认密码不能为空 ");
            }
            
            // 检查密码是否匹配
            if (!password.isEmpty() && !confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
                passwordField.setStyle("-fx-border-color: red;");
                confirmPasswordField.setStyle("-fx-border-color: red;");
                errorMsg.append("两次输入的密码不一致 ");
            }
            
            if (errorMsg.length() > 0) {
                messageLabel.setText(errorMsg.toString().trim());
                messageLabel.setStyle("-fx-text-fill: red;");
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.supplyAsync(() -> authService.register(username, password, nickname, confirmPassword))
                    .thenCompose(token -> {
                        // 校验账号密码成功后建立tcp连接
                        if (!gameConnectionManager.isConnected()) {
                            gameConnectionManager.connect();
                        }
                        return gameConnectionManager.sendAndListenFuture(GameMsgType.LOGIN, AuthProto.LoginRequest.newBuilder().setToken(token).build());
                    })
                    .thenApply(resp -> {
                        AuthProto.LoginResponse lr = ProtoBufUtil.parseRespBody(resp, AuthProto.LoginResponse.class);
                        return new Object[]{resp, lr};
                    });
        });
        registerButton.setOnSuccess(obj -> {
            Object[] arr = (Object[]) obj;
            CommonProto.BaseResponse resp = (CommonProto.BaseResponse) arr[0];
            AuthProto.LoginResponse lr = (AuthProto.LoginResponse) arr[1];

            if (resp.getCode() == 0) {
                messageLabel.setText("注册成功，=" + lr.getPlayerName());
            } else {
                messageLabel.setText("注册失败：" + resp.getCode());
            }
        });
        registerButton.setOnError(e -> {
            messageLabel.setText("注册失败：" + e.getCode());
            messageLabel.setStyle("-fx-text-fill: red;");
        });
    }
    
    private void clearFieldStyles() {
        usernameField.setStyle("");
        nicknameField.setStyle("");
        passwordField.setStyle("");
        confirmPasswordField.setStyle("");
    }

    @FXML
    protected void onBackToLoginClick(ActionEvent event) {
        try {
            // 返回到登录页面
            ViewManager.getInstance().show("login-view.fxml", "登录", 350, 400);
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("无法返回登录页面");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
}