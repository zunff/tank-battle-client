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
import java.util.Optional;

public class RoomController extends ViewLifecycle {

    // ========== FXML 组件 ==========
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
    private AsyncButton leaveRoomButton;

    // ========== 业务数据 ==========
    private GameRoomProto.GameRoomDetail roomDetail;
    private final GameConnectionManager gameConnectionManager = GameConnectionManager.getInstance();

    // 保存监听回调引用，用于移除监听
    private final Consumer<MessageLite> joinRoomCallback = this::onPlayerJoinRoom;
    private final Consumer<MessageLite> leaveRoomCallback = this::onPlayerLeaveRoom;
    private final Consumer<MessageLite> readyCallback = this::onPlayerReady;

    // ========== 生命周期方法 ==========

    @Override
    public void onShow(Object data) {
        if (data instanceof GameRoomProto.GameRoomDetail) {
            this.roomDetail = (GameRoomProto.GameRoomDetail) data;
            initializeUI();
            registerMessageListeners();
        } else {
            MessageUtil.showError("房间数据加载失败，请重试");
            ViewManager.getInstance().show(ViewEnum.LOBBY);
        }
    }

    @Override
    public void onClose() {
        sendLeaveRoomMessage();
        onHide();
    }

    @Override
    public void onHide() {
        unregisterMessageListeners();
        clearResources();
    }

    // ========== 消息监听回调方法 ==========

    private void onPlayerLeaveRoom(MessageLite messageLite) {
        if (!validateRoomDetail()) {
            return;
        }

        handleMessage(messageLite, "处理玩家离开房间消息失败", (playerInfo) -> {
            Long playerId = playerInfo.getPlayerId();
            Long creatorId = roomDetail.getCreatorId();

            // 检查离开的玩家是否是房主
            if (playerId.equals(creatorId)) {
                MessageUtil.showWarning("房主已离开房间，房间已解散");
                ViewManager.getInstance().show(ViewEnum.LOBBY);
                return;
            }

            // 从玩家列表中移除该玩家
            removePlayerFromList(playerId);
            updatePlayerCount();
            appendSystemMessage(playerInfo.getNickName() + " 离开了房间");
        });
    }

    private void onPlayerJoinRoom(MessageLite messageLite) {
        if (!validateRoomDetail()) {
            return;
        }

        handleMessage(messageLite, "处理玩家加入房间消息失败", (playerInfo) -> {
            Long playerId = playerInfo.getPlayerId();
            String nickName = playerInfo.getNickName();
            Long creatorId = roomDetail.getCreatorId();

            // 检查玩家是否已存在（避免重复添加）
            if (!isPlayerInList(playerId)) {
                boolean isCreator = playerId.equals(creatorId);
                addPlayerToList(playerId, nickName, isCreator);
                updatePlayerCount();
                appendSystemMessage(nickName + " 加入了房间");
            }
        });
    }

    private void onPlayerReady(MessageLite messageLite) {
        if (!validateRoomDetail()) {
            return;
        }

        handleMessage(messageLite, "处理玩家准备消息失败", (playerInfo) -> {
            Long playerId = playerInfo.getPlayerId();
            String nickName = playerInfo.getNickName();

            // 更新该玩家的准备状态
            updatePlayerReadyStatus(playerId, true);
            appendSystemMessage(nickName + " 已准备");
        });
    }

    // ========== UI 初始化方法 ==========

    private void initializeUI() {
        if (!validateRoomDetail()) {
            MessageUtil.showError("房间数据加载失败，请重试");
            ViewManager.getInstance().show(ViewEnum.LOBBY);
            return;
        }

        updateRoomInfo();
        loadPlayerList();
        appendSystemMessage("欢迎来到房间 " + roomDetail.getName() + "!");
        initAllButtons();
    }

    private void updateRoomInfo() {
        roomNameLabel.setText("房间名称: " + roomDetail.getName());
        roomStatusLabel.setText("状态: " + getRoomStatusText());
        playerCountLabel.setText("玩家: " + roomDetail.getPlayersCount() + "/" + roomDetail.getMaxPlayers());
    }

    private String getRoomStatusText() {
        return roomDetail.getStatus() == GameRoomProto.RoomStatus.WAITING ? "等待中" : "游戏中";
    }

    private void loadPlayerList() {
        playerListView.getItems().clear();
        Long creatorId = roomDetail.getCreatorId();

        for (GameRoomProto.PlayerInfo playerData : roomDetail.getPlayersList()) {
            boolean isCreator = playerData.getPlayerId() == creatorId;
            boolean isReady = playerData.getStatus() == GameRoomProto.UserStatus.READY;
            PlayerItem playerItem = new PlayerItem(playerData.getPlayerId(), playerData.getNickName(), isCreator);
            playerItem.setReady(isReady);
            playerListView.getItems().add(playerItem);
        }
    }

    private void updateButtonVisibility() {
        Long currentPlayerId = UserInfoManager.getInstance().getPlayerId();
        boolean isCreator = currentPlayerId != null && currentPlayerId.equals(roomDetail.getCreatorId());

        startGameButton.setVisible(isCreator);
        startGameButton.setManaged(isCreator);
        readyButton.setVisible(!isCreator);
        readyButton.setManaged(!isCreator);
    }

    private void updatePlayerCount() {
        int currentCount = playerListView.getItems().size();
        playerCountLabel.setText("玩家: " + currentCount + "/" + roomDetail.getMaxPlayers());
    }

    // ========== 按钮初始化方法 ==========

    private void initAllButtons() {
        initStartGameButton();
        initReadyButton();
        initLeaveRoomButton();
        updateButtonVisibility();
    }

    private void initStartGameButton() {
        startGameButton.setAction(() -> CompletableFuture.completedFuture(null));
        startGameButton.setOnSuccess(obj -> {
            // 开始游戏成功
        });
        startGameButton.setOnError(ex -> {
            // 开始游戏失败
        });
    }

    private void initReadyButton() {
        readyButton.setAction(() -> createRoomAction(() ->
                gameConnectionManager.sendAndListenFuture(
                        GameMsgType.READY,
                        GameRoomProto.ReadyRequest.newBuilder()
                                .setRoomId(roomDetail.getId())
                                .setPlayerId(getCurrentPlayerId())
                                .build()
                )
        ));

        readyButton.setOnSuccess(obj -> handleReadySuccess((CommonProto.BaseResponse) obj));
        readyButton.setOnError(ex -> MessageUtil.showError("准备失败: " + ex.getMessage()));
    }

    private void initLeaveRoomButton() {
        leaveRoomButton.setAction(() -> createRoomAction(() ->
                gameConnectionManager.sendAndListenFuture(
                        GameMsgType.LEAVE_ROOM,
                        GameRoomProto.LeaveRequest.newBuilder()
                                .setRoomId(roomDetail.getId())
                                .setPlayerId(getCurrentPlayerId())
                                .build()
                )
        ));

        leaveRoomButton.setOnSuccess(obj -> handleLeaveRoomSuccess((CommonProto.BaseResponse) obj));
        leaveRoomButton.setOnError(ex -> MessageUtil.showError("离开房间失败: " + ex.getMessage()));
    }

    // ========== 其他方法 ==========

    @FXML
    protected void onSendClick(ActionEvent event) {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            chatArea.appendText("我: " + message + "\n");
            messageField.clear();
            scrollChatToBottom();
        }
    }

    /**
     * 发送离开房间消息（用于窗口关闭时）
     * 窗口关闭时使用异步发送，不等待响应，避免阻塞关闭
     */
    private void sendLeaveRoomMessage() {
        if (roomDetail != null && getCurrentPlayerId() != null) {
            gameConnectionManager.send(GameMsgType.LEAVE_ROOM, GameRoomProto.LeaveRequest.newBuilder()
                    .setRoomId(roomDetail.getId())
                    .setPlayerId(getCurrentPlayerId())
                    .build());
            System.out.println("发送离开房间消息: RoomId=" + roomDetail.getId() + ", PlayerId=" + getCurrentPlayerId());
        }
    }

    // ========== 辅助方法 ==========

    private void registerMessageListeners() {
        gameConnectionManager.listenMessage(GameMsgType.PLAYER_JOIN_ROOM, joinRoomCallback);
        gameConnectionManager.listenMessage(GameMsgType.PLAYER_LEAVE_ROOM, leaveRoomCallback);
        gameConnectionManager.listenMessage(GameMsgType.PLAYER_READY, readyCallback);
    }

    private void unregisterMessageListeners() {
        gameConnectionManager.removeListener(GameMsgType.PLAYER_JOIN_ROOM, joinRoomCallback);
        gameConnectionManager.removeListener(GameMsgType.PLAYER_LEAVE_ROOM, leaveRoomCallback);
        gameConnectionManager.removeListener(GameMsgType.PLAYER_READY, readyCallback);
    }

    private void clearResources() {
        roomDetail = null;
        playerListView.getItems().clear();
        chatArea.clear();
    }

    private boolean validateRoomDetail() {
        return roomDetail != null;
    }

    private Long getCurrentPlayerId() {
        return UserInfoManager.getInstance().getPlayerId();
    }

    private CompletableFuture<?> createRoomAction(java.util.function.Supplier<CompletableFuture<?>> action) {
        if (!validateRoomDetail()) {
            MessageUtil.showError("房间数据异常");
            return CompletableFuture.completedFuture(null);
        }

        Long playerId = getCurrentPlayerId();
        if (playerId == null) {
            MessageUtil.showError("用户信息异常");
            return CompletableFuture.completedFuture(null);
        }

        return action.get();
    }

    private void handleMessage(MessageLite messageLite, String errorMessage,
                               Consumer<GameRoomProto.PlayerInfo> handler) {
        try {
            GameRoomProto.PlayerInfo playerInfo = (GameRoomProto.PlayerInfo) messageLite;
            handler.accept(playerInfo);
        } catch (Exception e) {
            System.err.println(errorMessage + ": " + e.getMessage());
        }
    }

    private void handleReadySuccess(CommonProto.BaseResponse resp) {
        if (resp != null && resp.getCode() == 0) {
            Long currentPlayerId = getCurrentPlayerId();
            if (currentPlayerId != null) {
                updatePlayerReadyStatus(currentPlayerId, true);
                readyButton.setPermanentlyDisabled("已准备");
                leaveRoomButton.setPermanentlyDisabled("离开房间");
            }
        } else {
            MessageUtil.showError("准备失败: " + (resp == null ? "未知错误" : resp.getMessage()));
        }
    }

    private void handleLeaveRoomSuccess(CommonProto.BaseResponse resp) {
        if (resp != null && resp.getCode() == 0) {
            ViewManager.getInstance().show(ViewEnum.LOBBY);
        } else {
            MessageUtil.showError("离开房间失败: " + (resp == null ? "未知错误" : resp.getMessage()));
        }
    }

    private boolean isPlayerInList(Long playerId) {
        return playerListView.getItems().stream()
                .anyMatch(item -> item.getPlayerId().equals(playerId));
    }

    private void addPlayerToList(Long playerId, String nickName, boolean isCreator) {
        PlayerItem playerItem = new PlayerItem(playerId, nickName, isCreator);
        playerListView.getItems().add(playerItem);
    }

    private void removePlayerFromList(Long playerId) {
        playerListView.getItems().removeIf(item -> item.getPlayerId().equals(playerId));
    }

    private void updatePlayerReadyStatus(Long playerId, boolean ready) {
        findPlayerInList(playerId).ifPresent(item -> {
            item.setReady(ready);
            playerListView.refresh();
        });
    }

    private Optional<PlayerItem> findPlayerInList(Long playerId) {
        return playerListView.getItems().stream()
                .filter(item -> item.getPlayerId().equals(playerId))
                .findFirst();
    }

    private void appendSystemMessage(String message) {
        chatArea.appendText("系统消息: " + message + "\n");
        scrollChatToBottom();
    }

    private void scrollChatToBottom() {
        chatArea.positionCaret(chatArea.getText().length());
    }
}
