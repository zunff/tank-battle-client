package com.zunf.tankbattleclient.controller;


import cn.hutool.core.util.StrUtil;
import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.model.bo.ResponseBo;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.protobuf.game.auth.AuthProto;
import com.zunf.tankbattleclient.service.AuthService;
import com.zunf.tankbattleclient.ui.AsyncButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.concurrent.CompletableFuture;

public class RegisterController {

    private final AuthService authService = AuthService.getInstance();

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

            return CompletableFuture.supplyAsync(() -> authService.register(username, password, nickname, confirmPassword)
            ).thenApply(token -> {
                if (StrUtil.isBlank(token)) {
                    return null;
                } else {
                    return new ResponseBo(CommonProto.BaseResponse.newBuilder().setCode(0).build());
                }
            });
        });
        registerButton.setOnSuccess(responseBo -> {
            ViewManager.getInstance().show(ViewEnum.LOGIN);
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
        ViewManager.getInstance().show(ViewEnum.LOGIN);
    }
}