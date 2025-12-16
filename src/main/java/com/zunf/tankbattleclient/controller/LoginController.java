package com.zunf.tankbattleclient.controller;

import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.exception.BusinessException;
import com.zunf.tankbattleclient.exception.ErrorCode;
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

public class LoginController extends ViewLifecycle {

    private final AuthService authService = AuthService.getInstance();
    private final GameConnectionManager gameConnectionManager = GameConnectionManager.getInstance();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private AsyncButton asyncLoginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label messageLabel;

    @FXML
    private VBox mainContainer;

    @FXML
    public void initialize() {
        initLoginBtn();
    }

    private void initLoginBtn() {
        asyncLoginButton.setAction(() -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            // http 请求业务服务器获取token
            return CompletableFuture.supplyAsync(() -> authService.login(username, password))
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

        asyncLoginButton.setOnSuccess(obj -> {
            Object[] arr = (Object[]) obj;
            CommonProto.BaseResponse resp = (CommonProto.BaseResponse) arr[0];
            AuthProto.LoginResponse lr = (AuthProto.LoginResponse) arr[1];

            if (resp.getCode() == 0) {
                messageLabel.setText("登录成功，playerId=" + lr.getPlayerId());
            } else {
                messageLabel.setText("登录失败：" + resp.getCode());
            }
        });

        asyncLoginButton.setOnError(ex -> {
            ex.printStackTrace();
            messageLabel.setText("登录失败：" + ex.getCode());
        });

    }

    @FXML
    protected void onRegisterClick(ActionEvent event) {
        try {
            // 跳转到注册页面
            ViewManager.getInstance().show("register-view.fxml", "注册", 350, 400);
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("无法打开注册页面");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
}
