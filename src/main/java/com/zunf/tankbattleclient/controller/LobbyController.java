package com.zunf.tankbattleclient.controller;

import com.zunf.tankbattleclient.manager.UserInfoManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;

public class LobbyController extends ViewLifecycle {

    @FXML
    private Label welcomeLabel;

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField messageField;

    @FXML
    private ListView<String> roomListView;

    @FXML
    private Button sendButton;

    @FXML
    private Button createRoomButton;

    @FXML
    private Button joinRoomButton;

    @FXML
    private Button logoutButton;

    @FXML
    public void initialize() {
        // 从缓存中获取用户名
        String username = UserInfoManager.getInstance().getUsername();
        if (username != null && !username.isEmpty()) {
            welcomeLabel.setText("欢迎, " + username);
        }
        
        // 初始化假数据
        initializeChat();
        initializeRooms();
    }

    private void initializeChat() {
        // 添加一些假的聊天记录
        chatArea.appendText("系统消息: 欢迎来到大厅!\n");
        chatArea.appendText("管理员: 请遵守聊天规则，文明发言。\n");
        chatArea.appendText("玩家1: 有人一起开黑吗？\n");
        chatArea.appendText("玩家2: 我在! 我在!\n");
    }

    private void initializeRooms() {
        // 添加一些假的房间数据
        roomListView.getItems().addAll(
                "房间1 - 经典模式 (2/4)",
                "房间2 - 团队对抗 (3/4)",
                "房间3 - 生存模式 (1/4)",
                "房间4 - 自定义地图 (0/4)",
                "房间5 - 快速对战 (4/4)",
                "房间6 - 排位赛 (2/4)",
                "房间7 - 娱乐模式 (1/4)",
                "房间8 - 新手练习 (0/4)"
        );
    }

    @FXML
    protected void onSendClick(ActionEvent event) {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            String username = UserInfoManager.getInstance().getUsername();
            if (username == null || username.isEmpty()) {
                username = "未知用户";
            }
            chatArea.appendText(username + ": " + message + "\n");
            messageField.clear();
            // 自动滚动到底部
            chatArea.positionCaret(chatArea.getText().length());
        }
    }

    @FXML
    protected void onCreateRoomClick(ActionEvent event) {
        showAlert("提示", "创建房间功能将在后续版本中实现");
    }

    @FXML
    protected void onJoinRoomClick(ActionEvent event) {
        String selectedRoom = roomListView.getSelectionModel().getSelectedItem();
        if (selectedRoom != null) {
            showAlert("提示", "加入房间功能将在后续版本中实现\n选中的房间: " + selectedRoom);
        } else {
            showAlert("提示", "请先选择一个房间");
        }
    }

    @FXML
    protected void onLogoutClick(ActionEvent event) {
        // 清除用户信息
        UserInfoManager.getInstance().clearUserinfo();
        ViewManager.getInstance().show("login-view.fxml", "登录", 350, 400);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}