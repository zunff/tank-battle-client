package com.zunf.tankbattleclient.controller;

import com.google.protobuf.MessageLite;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.manager.GameConnectionManager;
import com.zunf.tankbattleclient.manager.UserInfoManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.model.PlayerItem;
import com.zunf.tankbattleclient.model.bo.ResponseBo;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;
import com.zunf.tankbattleclient.ui.AsyncButton;
import com.zunf.tankbattleclient.util.MessageUtil;
import com.zunf.tankbattleclient.manager.SoundManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.function.Supplier;

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
    private final Consumer<MessageLite> gameStartedCallback = this::onGameStarted;

    // ========== 生命周期方法 ==========

    @Override
    public void onShow(Object data) {
        SoundManager.getInstance().playBackgroundMusic("back_room.mp3", 0.8);

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
        SoundManager.getInstance().stopBackgroundMusic();

        unregisterMessageListeners();
        clearResources();
    }

    // ========== 消息监听回调方法 ==========

    private void onPlayerLeaveRoom(MessageLite messageLite) {
        if (!validateRoomDetail()) {
            return;
        }
        
        handlePlayerInfoMessage(messageLite, "处理玩家离开房间消息失败", (playerInfo) -> {
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
        
        handlePlayerInfoMessage(messageLite, "处理玩家加入房间消息失败", (playerInfo) -> {
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

        handlePlayerInfoMessage(messageLite, "处理玩家准备消息失败", (playerInfo) -> {
            Long playerId = playerInfo.getPlayerId();
            String nickName = playerInfo.getNickName();

            // 更新该玩家的准备状态
            updatePlayerReadyStatus(playerId, true);
            appendSystemMessage(nickName + " 已准备");
        });
    }

    private void onGameStarted(MessageLite messageLite) {
        if (!validateRoomDetail()) {
            return;
        }
        GameRoomProto.StartNotice startNotice = (GameRoomProto.StartNotice) messageLite;
        // 发送一个 Ack 表示已经收到
        gameConnectionManager.sendAndListenFuture(GameMsgType.LOADED_ACK, GameRoomProto.LoadedAck.newBuilder()
                .setRoomId(startNotice.getRoomId())
                .setPlayerId(getCurrentPlayerId())
                .build())
                .whenComplete((resp, throwable) -> {
                    javafx.application.Platform.runLater(() -> {
                        if (throwable != null) {
                            MessageUtil.showError("游戏开始失败，请重试");
                            ViewManager.getInstance().show(ViewEnum.LOBBY);
                        } else {
                            // 开始 5 秒倒计时
                            startCountdown(startNotice);
                        }
                    });
                });
    }

    /**
     * 开始倒计时，倒计时结束后进入游戏界面
     */
    private void startCountdown(GameRoomProto.StartNotice startNotice) {
        // 创建倒计时标签
        Label countdownLabel = new Label("5");
        countdownLabel.setStyle("-fx-font-size: 120px; -fx-text-fill: #FFD700; -fx-font-weight: bold;");
        
        // 创建遮罩层，使用 StackPane 确保内容居中
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        overlay.setAlignment(Pos.CENTER); // 确保内容居中
        overlay.getChildren().add(countdownLabel);
        
        // 获取场景和根节点
        javafx.scene.Scene scene = roomNameLabel.getScene();
        if (scene == null) {
            return;
        }
        
        javafx.scene.Parent root = scene.getRoot();
        
        if (root instanceof javafx.scene.layout.BorderPane borderPane) {
            // 如果根节点是 BorderPane，需要创建一个包装层
            // 创建一个新的 StackPane 作为包装，包含原 BorderPane 和 overlay
            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(borderPane);
            wrapper.getChildren().add(overlay);
            
            // 绑定 overlay 大小到 wrapper
            overlay.prefWidthProperty().bind(wrapper.widthProperty());
            overlay.prefHeightProperty().bind(wrapper.heightProperty());
            overlay.setMaxWidth(Double.MAX_VALUE);
            overlay.setMaxHeight(Double.MAX_VALUE);
            
            // 替换场景的根节点
            scene.setRoot(wrapper);
        } else if (root instanceof javafx.scene.layout.Pane pane) {
            // 如果是 Pane，直接添加 overlay
            overlay.prefWidthProperty().bind(scene.widthProperty());
            overlay.prefHeightProperty().bind(scene.heightProperty());
            overlay.setMaxWidth(Double.MAX_VALUE);
            overlay.setMaxHeight(Double.MAX_VALUE);
            pane.getChildren().add(overlay);
        } else {
            // 对于其他类型，也创建一个包装层
            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(root);
            wrapper.getChildren().add(overlay);
            
            overlay.prefWidthProperty().bind(wrapper.widthProperty());
            overlay.prefHeightProperty().bind(wrapper.heightProperty());
            overlay.setMaxWidth(Double.MAX_VALUE);
            overlay.setMaxHeight(Double.MAX_VALUE);
            
            scene.setRoot(wrapper);
        }
        
        // 确保 overlay 在最上层
        overlay.toFront();

        // 倒计时逻辑
        final int[] countdown = { 5 };
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdown[0]--;
            if (countdown[0] > 0) {
                countdownLabel.setText(String.valueOf(countdown[0]));
            } else {
                countdownLabel.setText("开始！");
                // 倒计时结束，延迟一小段时间后切换到游戏界面
                Timeline delayTimeline = new Timeline(new KeyFrame(Duration.millis(500), ev -> {
                    ViewManager.getInstance().show(ViewEnum.GAME, startNotice);
                }));
                delayTimeline.setCycleCount(1);
                delayTimeline.play();
            }
        }));
        timeline.setCycleCount(6); // 5秒 + 1次显示"开始！"
        timeline.play();
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
        startGameButton.setAction(() -> gameConnectionManager.sendAndListenFuture(GameMsgType.START_GAME,
                GameRoomProto.StartRequest.newBuilder()
                        .setRoomId(roomDetail.getId())
                        .setPlayerId(getCurrentPlayerId())
                        .build()));
        startGameButton.setOnSuccess(obj -> {
            // 开始游戏成功
        });
        startGameButton.setOnError(ex -> {
            MessageUtil.showError("开始游戏失败: " + ex.getMessage());
        });
    }

    private void initReadyButton() {
        readyButton.setAction(() -> createRoomAction(() -> gameConnectionManager.sendAndListenFuture(
                GameMsgType.READY,
                GameRoomProto.ReadyRequest.newBuilder()
                        .setRoomId(roomDetail.getId())
                        .setPlayerId(getCurrentPlayerId())
                        .build())));

        readyButton.setOnSuccess(responseBo -> handleReadySuccess(responseBo.getResponse()));
        readyButton.setOnError(ex -> MessageUtil.showError("准备失败: " + ex.getMessage()));
    }

    private void initLeaveRoomButton() {
        leaveRoomButton.setAction(() -> createRoomAction(() -> gameConnectionManager.sendAndListenFuture(
                GameMsgType.LEAVE_ROOM,
                GameRoomProto.LeaveRequest.newBuilder()
                        .setRoomId(roomDetail.getId())
                        .setPlayerId(getCurrentPlayerId())
                        .build())));

        leaveRoomButton.setOnSuccess(responseBo -> ViewManager.getInstance().show(ViewEnum.LOBBY));
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
        gameConnectionManager.listenMessage(GameMsgType.GAME_STARTED, gameStartedCallback);
    }

    private void unregisterMessageListeners() {
        gameConnectionManager.removeListener(GameMsgType.PLAYER_JOIN_ROOM, joinRoomCallback);
        gameConnectionManager.removeListener(GameMsgType.PLAYER_LEAVE_ROOM, leaveRoomCallback);
        gameConnectionManager.removeListener(GameMsgType.PLAYER_READY, readyCallback);
        gameConnectionManager.removeListener(GameMsgType.GAME_STARTED, gameStartedCallback);
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

    private CompletableFuture<ResponseBo> createRoomAction(Supplier<CompletableFuture<ResponseBo>> action) {
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

    private void handlePlayerInfoMessage(MessageLite messageLite, String errorMessage,
            Consumer<GameRoomProto.PlayerInfo> handler) {
        try {
            GameRoomProto.PlayerInfo playerInfo = (GameRoomProto.PlayerInfo) messageLite;
            handler.accept(playerInfo);
        } catch (Exception e) {
            System.err.println(errorMessage + ": " + e.getMessage());
        }
    }

    private void handleReadySuccess(CommonProto.BaseResponse resp) {
        Long currentPlayerId = getCurrentPlayerId();
        if (currentPlayerId != null) {
            updatePlayerReadyStatus(currentPlayerId, true);
            readyButton.setPermanentlyDisabled("已准备");
            leaveRoomButton.setPermanentlyDisabled("离开房间");
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
