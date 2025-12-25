package com.zunf.tankbattleclient.controller;

import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.manager.GameConnectionManager;
import com.zunf.tankbattleclient.manager.UserInfoManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.model.PlayerItem;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;
import com.zunf.tankbattleclient.ui.AsyncButton;
import com.zunf.tankbattleclient.util.MessageUtil;
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
    private AsyncButton readyButton;

    @FXML
    private Button leaveRoomButton;

    private GameRoomProto.GameRoomDetail roomDetail;

    private final GameConnectionManager gameConnectionManager = GameConnectionManager.getInstance();
    
    // 保存监听回调引用，用于移除监听
    private final Consumer<MessageLite> joinRoomCallback = this::onPlayerJoinRoom;
    private final Consumer<MessageLite> leaveRoomCallback = this::onPlayerLeaveRoom;
    private final Consumer<MessageLite> readyCallback = this::onPlayerReady;

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
        // 监听加入房间、离开房间和准备消息
        gameConnectionManager.listenMessage(GameMsgType.PLAYER_JOIN_ROOM, joinRoomCallback);
        gameConnectionManager.listenMessage(GameMsgType.PLAYER_LEAVE_ROOM, leaveRoomCallback);
        gameConnectionManager.listenMessage(GameMsgType.PLAYER_READY, readyCallback);
    }

    @Override
    public void onClose() {
        // 如果用户在房间里，先发送离开房间消息
        sendLeaveRoomMessage();
        // 然后执行正常的隐藏逻辑
        onHide();
    }

    @Override
    public void onHide() {
        // 移除消息监听
        gameConnectionManager.removeListener(GameMsgType.PLAYER_JOIN_ROOM, joinRoomCallback);
        gameConnectionManager.removeListener(GameMsgType.PLAYER_LEAVE_ROOM, leaveRoomCallback);
        gameConnectionManager.removeListener(GameMsgType.PLAYER_READY, readyCallback);

        // 清理资源
        roomDetail = null;
        playerListView.getItems().clear();
        chatArea.clear();
    }


    private void onPlayerLeaveRoom(MessageLite messageLite) {
        if (roomDetail == null) {
            return;
        }
        
        try {
            GameRoomProto.PlayerInfo roomPlayer = (GameRoomProto.PlayerInfo) messageLite;
            Long playerId = roomPlayer.getPlayerId();
            Long creatorId = roomDetail.getCreatorId();

            // 检查离开的玩家是否是房主
            if (playerId.equals(creatorId)) {
                // 房主离开，显示消息并返回大厅
                MessageUtil.showWarning("房主已离开房间，房间已解散");
                ViewManager.getInstance().show(ViewEnum.LOBBY);
                return;
            }

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
            GameRoomProto.PlayerInfo roomPlayer = (GameRoomProto.PlayerInfo) messageLite;
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

    private void onPlayerReady(MessageLite messageLite) {
        if (roomDetail == null) {
            return;
        }
        
        try {
            GameRoomProto.PlayerInfo roomPlayer = (GameRoomProto.PlayerInfo) messageLite;
            Long playerId = roomPlayer.getPlayerId();
            String nickName = roomPlayer.getNickName();

            // 更新该玩家的准备状态
            playerListView.getItems().stream()
                    .filter(item -> item.getPlayerId().equals(playerId))
                    .findFirst()
                    .ifPresent(item -> {
                        item.setReady(true);
                        // 刷新 ListView 显示
                        playerListView.refresh();
                    });

            // 在聊天区域显示准备消息
            chatArea.appendText("系统消息: " + nickName + " 已准备\n");
            chatArea.positionCaret(chatArea.getText().length());
        } catch (Exception e) {
            System.err.println("处理玩家准备消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新玩家数量显示
     */
    private void updatePlayerCount() {
        int currentCount = playerListView.getItems().size();
        playerCountLabel.setText("玩家: " + currentCount + "/" + roomDetail.getMaxPlayers());
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
        
        for (GameRoomProto.PlayerInfo playerData : roomDetail.getPlayersList()) {
            boolean isCreator = playerData.getPlayerId() == creatorId;
            boolean isReady = playerData.getStatus() == GameRoomProto.UserStatus.READY;
            PlayerItem playerItem = new PlayerItem(playerData.getPlayerId(), playerData.getNickName(), isCreator);
            playerItem.setReady(isReady);
            playerListView.getItems().add(playerItem);
        }

        // 添加欢迎消息
        chatArea.clear();
        chatArea.appendText("系统消息: 欢迎来到房间 " + roomDetail.getName() + "!\n");

        // 初始化按钮事件和显示控制
        initStartGameButton();
        initReadyButton();
        updateButtonVisibility();
    }

    /**
     * 更新按钮的可见性
     * 开始游戏按钮只对房主显示
     * 准备按钮对非房主显示
     */
    private void updateButtonVisibility() {
        Long currentPlayerId = UserInfoManager.getInstance().getPlayerId();
        boolean isCreator = currentPlayerId != null && currentPlayerId.equals(roomDetail.getCreatorId());
        startGameButton.setVisible(isCreator);
        startGameButton.setManaged(isCreator);
        // 房主不需要准备，所以准备按钮对非房主显示
        readyButton.setVisible(!isCreator);
        readyButton.setManaged(!isCreator);
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

    private void initReadyButton() {
        readyButton.setAction(() -> {
            if (roomDetail == null) {
                MessageUtil.showError("房间数据异常");
                return CompletableFuture.completedFuture(null);
            }
            
            Long playerId = UserInfoManager.getInstance().getPlayerId();
            if (playerId == null) {
                MessageUtil.showError("用户信息异常");
                return CompletableFuture.completedFuture(null);
            }
            
            return gameConnectionManager.sendAndListenFuture(
                GameMsgType.READY,
                GameRoomProto.ReadyRequest.newBuilder()
                    .setRoomId(roomDetail.getId())
                    .setPlayerId(playerId)
                    .build()
            );
        });
        
        readyButton.setOnSuccess(obj -> {
            CommonProto.BaseResponse resp = (CommonProto.BaseResponse) obj;
            if (resp != null && resp.getCode() == 0) {
                // 准备成功，更新自己的显示状态
                Long currentPlayerId = UserInfoManager.getInstance().getPlayerId();
                if (currentPlayerId != null) {
                    playerListView.getItems().stream()
                            .filter(item -> item.getPlayerId().equals(currentPlayerId))
                            .findFirst()
                            .ifPresent(item -> {
                                item.setReady(true);
                                // 刷新 ListView 显示
                                playerListView.refresh();
                            });
                    // 解绑 disable 属性，然后手动禁用并显示"已准备"
                    readyButton.disableProperty().unbind();
                    readyButton.setText("已准备");
                    readyButton.setDisable(true);
                    // 移除按钮的点击事件，防止再次点击
                    readyButton.setOnAction(null);
                }
            } else {
                MessageUtil.showError("准备失败: " + (resp == null ? "未知错误" : resp.getMessage()));
            }
        });
        
        readyButton.setOnError(ex -> {
            MessageUtil.showError("准备失败: " + ex.getMessage());
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
        sendLeaveRoomMessage();
        // 离开房间，返回大厅
        ViewManager.getInstance().show(ViewEnum.LOBBY);
    }

    /**
     * 发送离开房间消息
     */
    private void sendLeaveRoomMessage() {
        if (roomDetail != null) {
            gameConnectionManager.send(GameMsgType.LEAVE_ROOM, GameRoomProto.LeaveRequest.newBuilder()
                    .setRoomId(roomDetail.getId())
                    .setPlayerId(UserInfoManager.getInstance().getPlayerId())
                    .build());
        }
    }
}

