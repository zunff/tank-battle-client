package com.zunf.tankbattleclient.controller;

import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.manager.GameConnectionManager;
import com.zunf.tankbattleclient.manager.UserInfoManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.protobuf.game.auth.AuthProto;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;
import com.zunf.tankbattleclient.ui.AsyncButton;
import com.zunf.tankbattleclient.util.ProtoBufUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    private AsyncButton createRoomButton;

    @FXML
    private AsyncButton joinRoomButton;

    @FXML
    private Button logoutButton;

    private GameConnectionManager gameConnectionManager = GameConnectionManager.getInstance();

    @FXML
    public void initialize() {
        // 从缓存中获取用户名
        String username = UserInfoManager.getInstance().getUsername();
        if (username != null && !username.isEmpty()) {
            welcomeLabel.setText("欢迎, " + username);
        }
        // 初始化按钮实践
        initCreateRoomButton();
        initJoinRoomButton();

        // 初始化假数据
        initializeChat();
        initializeRooms();
    }

    private void initCreateRoomButton() {
        createRoomButton.setAction(() -> {
            // 创建房间弹窗
            Dialog<RoomCreationData> dialog = new Dialog<>();
            dialog.setTitle("创建房间");

            // 设置确认和取消按钮
            ButtonType createButtonType = new ButtonType("创建", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

            // 创建表单
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // 房间名称输入框
            TextField roomNameField = new TextField();
            roomNameField.setPromptText("输入房间名称");

            // 最大人数选择框
            ComboBox<Integer> maxPlayersComboBox = new ComboBox<>();
            maxPlayersComboBox.getItems().addAll(2, 5, 10);
            maxPlayersComboBox.setValue(2); // 默认值

            // 添加表单元素
            grid.add(new Label("房间名称:"), 0, 0);
            grid.add(roomNameField, 1, 0);
            grid.add(new Label("最大人数:"), 0, 1);
            grid.add(maxPlayersComboBox, 1, 1);

            // 禁用确认按钮，直到房间名称不为空
            Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
            createButton.setDisable(true);

            // 监听房间名称输入，启用/禁用确认按钮
            roomNameField.textProperty().addListener((observable, oldValue, newValue) -> {
                createButton.setDisable(newValue.trim().isEmpty());
            });

            dialog.getDialogPane().setContent(grid);

            // 转换结果
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == createButtonType) {
                    return new RoomCreationData(roomNameField.getText().trim(), maxPlayersComboBox.getValue());
                }
                return null;
            });

            // 显示弹窗并等待用户响应
            Optional<RoomCreationData> result = dialog.showAndWait();

            // 处理用户选择
            return result.map(data -> {
                // 这里可以添加实际的创建房间逻辑
                System.out.println("创建房间: " + data.roomName + ", 最大人数: " + data.maxPlayers);
                return gameConnectionManager.sendAndListenFuture(GameMsgType.CREATE_ROOM, GameRoomProto.CreateGameRoomRequest.newBuilder()
                        .setName(data.roomName)
                        .setMaxPlayers(data.maxPlayers)
                        .setPlayerId(UserInfoManager.getInstance().getPlayerId())
                        .build())
                        .thenApply(resp -> {
                            GameRoomProto.CreateGameRoomResponse r = ProtoBufUtil.parseRespBody(resp, GameRoomProto.CreateGameRoomResponse.parser());
                            return new Object[]{resp, r};
                        });
            }).orElseGet(() -> CompletableFuture.completedFuture(null));
        });
        createRoomButton.setOnSuccess(obj -> {
            // 创建房间成功
            showAlert("创建房间成功", "");
        });
        createRoomButton.setOnError(ex -> {
            // 创建房间失败
            showAlert("创建房间失败", "请检查网络连接或稍后再试");
        });
    }

    // 房间创建数据类
    private static class RoomCreationData {
        private final String roomName;
        private final int maxPlayers;

        public RoomCreationData(String roomName, int maxPlayers) {
            this.roomName = roomName;
            this.maxPlayers = maxPlayers;
        }
    }

    private void initJoinRoomButton() {
        joinRoomButton.setAction(() -> {
            // 加入房间逻辑
            return CompletableFuture.completedFuture(null);
        });
        joinRoomButton.setOnSuccess(obj -> {
            // 加入房间成功
            showAlert("加入房间成功", "欢迎来到游戏房间");
        });
        joinRoomButton.setOnError(ex -> {
            // 加入房间失败
            showAlert("加入房间失败", "请检查房间ID或网络连接");
        });
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