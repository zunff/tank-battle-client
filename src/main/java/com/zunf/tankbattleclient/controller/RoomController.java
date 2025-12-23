package com.zunf.tankbattleclient.controller;

import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.manager.GameConnectionManager;
import com.zunf.tankbattleclient.manager.UserInfoManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.model.PlayerItem;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;
import com.zunf.tankbattleclient.ui.AsyncButton;
import com.zunf.tankbattleclient.util.MessageUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

public class RoomController extends ViewLifecycle {

    @FXML
    private Label roomNameLabel;

    @FXML
    private Label roomStatusLabel;

    @FXML
    private Label playerCountLabel;

    @FXML
    private ListView<PlayerItem> playerListView;

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField messageField;

    @FXML
    private Button sendButton;

    @FXML
    private AsyncButton startGameButton;

    @FXML
    private Button leaveRoomButton;

    private GameRoomProto.GameRoomDetail roomDetail;

    private final GameConnectionManager gameConnectionManager = GameConnectionManager.getInstance();
    
    // 保存监听回调引用，用于移除监听
    private final Consumer<MessageLite> joinRoomCallback = this::onPlayerJoinRoom;
    private final Consumer<MessageLite> leaveRoomCallback = this::onPlayerLeaveRoom;

    @Override
    public void onShow(Object data) {
        // 接收传递的房间详情数据
        if (data instanceof GameRoomProto.GameRoomDetail) {
            this.roomDetail = (GameRoomProto.GameRoomDetail) data;
            initializeUI();
        } else {
            // 如果没有传递数据，关闭页面并显示错误提示
            MessageUtil.showError("房间数据加载失败，请重试");
            ViewManager.getInstance().show(ViewEnum.LOBBY);
        }
        // 监听加入房间和离开房间消息
        gameConnectionManager.listenMessage(GameMsgType.PLAYER_JOIN_ROOM, joinRoomCallback);
        gameConnectionManager.listenMessage(GameMsgType.PLAYER_LEAVE_ROOM, leaveRoomCallback);
    }

    private void onPlayerLeaveRoom(MessageLite messageLite) {
        if (roomDetail == null) {
            return;
        }
        
        try {
            GameRoomProto.GameRoomPlayerData roomPlayer = (GameRoomProto.GameRoomPlayerData) messageLite;
            Long playerId = roomPlayer.getPlayerId();

            // 从玩家列表中移除该玩家
            playerListView.getItems().removeIf(item -> item.getPlayerId().equals(playerId));

            // 更新玩家数量
            updatePlayerCount();

            // 在聊天区域显示离开消息
            chatArea.appendText("系统消息: " + roomPlayer.getNickName() + " 离开了房间\n");
            chatArea.positionCaret(chatArea.getText().length());
        } catch (Exception e) {
            System.err.println("处理玩家离开房间消息失败: " + e.getMessage());
        }
    }

    private void onPlayerJoinRoom(MessageLite messageLite) {
        if (roomDetail == null) {
            return;
        }
        
        try {
            GameRoomProto.GameRoomPlayerData roomPlayer = (GameRoomProto.GameRoomPlayerData) messageLite;
            Long playerId = roomPlayer.getPlayerId();
            String nickName = roomPlayer.getNickName();
            Long creatorId = roomDetail.getCreatorId();

            // 检查玩家是否已存在（避免重复添加）
            boolean exists = playerListView.getItems().stream()
                    .anyMatch(item -> item.getPlayerId().equals(playerId));

            if (!exists) {
                // 判断是否为房主
                boolean isCreator = playerId.equals(creatorId);
                PlayerItem playerItem = new PlayerItem(playerId, nickName, isCreator);
                playerListView.getItems().add(playerItem);

                // 更新玩家数量
                updatePlayerCount();

                // 在聊天区域显示加入消息
                chatArea.appendText("系统消息: " + nickName + " 加入了房间\n");
                chatArea.positionCaret(chatArea.getText().length());
            }
        } catch (Exception e) {
            System.err.println("处理玩家加入房间消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新玩家数量显示
     */
    private void updatePlayerCount() {
        int currentCount = playerListView.getItems().size();
        playerCountLabel.setText("玩家: " + currentCount + "/" + roomDetail.getMaxPlayers());
    }

    @Override
    public void onHide() {
        // 移除消息监听
        gameConnectionManager.removeListener(GameMsgType.PLAYER_JOIN_ROOM, joinRoomCallback);
        gameConnectionManager.removeListener(GameMsgType.PLAYER_LEAVE_ROOM, leaveRoomCallback);
        
        // 清理资源
        roomDetail = null;
        playerListView.getItems().clear();
        chatArea.clear();
    }

    private void initializeUI() {
        if (roomDetail == null) {
            MessageUtil.showError("房间数据加载失败，请重试");
            ViewManager.getInstance().show(ViewEnum.LOBBY);
            return;
        }

        // 使用传递的房间详情数据初始化UI
        roomNameLabel.setText("房间名称: " + roomDetail.getName());
        roomStatusLabel.setText("状态: " + (roomDetail.getStatus() == GameRoomProto.RoomStatus.WAITING ? "等待中" : "游戏中"));
        playerCountLabel.setText("玩家: " + roomDetail.getPlayersCount() + "/" + roomDetail.getMaxPlayers());

        // 清空玩家列表
        playerListView.getItems().clear();
        
        // 从房间详情中获取玩家列表
        Long creatorId = roomDetail.getCreatorId();
        
        for (GameRoomProto.GameRoomPlayerData playerData : roomDetail.getPlayersList()) {
            boolean isCreator = playerData.getPlayerId() == creatorId;
            PlayerItem playerItem = new PlayerItem(playerData.getPlayerId(), playerData.getNickName(), isCreator);
            playerListView.getItems().add(playerItem);
        }

        // 添加欢迎消息
        chatArea.clear();
        chatArea.appendText("系统消息: 欢迎来到房间 " + roomDetail.getName() + "!\n");

        // 初始化按钮事件和显示控制
        initStartGameButton();
        updateStartButtonVisibility();
    }

    /**
     * 更新开始按钮的可见性（只对房主显示）
     */
    private void updateStartButtonVisibility() {
        Long currentPlayerId = UserInfoManager.getInstance().getPlayerId();
        boolean isCreator = currentPlayerId != null && currentPlayerId.equals(roomDetail.getCreatorId());
        startGameButton.setVisible(isCreator);
        startGameButton.setManaged(isCreator);
    }

    private void initStartGameButton() {
        startGameButton.setAction(() -> {
            // 开始游戏逻辑（示例）
            return CompletableFuture.completedFuture(null);
        });
        startGameButton.setOnSuccess(obj -> {
            // 开始游戏成功
        });
        startGameButton.setOnError(ex -> {
            // 开始游戏失败
        });
    }

    @FXML
    protected void onSendClick(ActionEvent event) {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            chatArea.appendText("我: " + message + "\n");
            messageField.clear();
            // 自动滚动到底部
            chatArea.positionCaret(chatArea.getText().length());
        }
    }

    @FXML
    protected void onLeaveRoomClick(ActionEvent event) {
        // 发个消息
        gameConnectionManager.send(GameMsgType.LEAVE_ROOM, GameRoomProto.LeaveGameRoomRequest.newBuilder()
                .setRoomId(roomDetail.getId()).setPlayerId(UserInfoManager.getInstance().getPlayerId()).build());
        // 离开房间，返回大厅
        ViewManager.getInstance().show(ViewEnum.LOBBY);
    }
}

