package com.zunf.tankbattleclient.controller;

import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.exception.BusinessException;
import com.zunf.tankbattleclient.exception.ErrorCode;
import com.zunf.tankbattleclient.manager.GameConnectionManager;
import com.zunf.tankbattleclient.manager.UserInfoManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.model.bo.CreateRoomBo;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;
import com.zunf.tankbattleclient.ui.AsyncButton;
import com.zunf.tankbattleclient.util.MessageUtil;
import com.zunf.tankbattleclient.util.ProtoBufUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LobbyController extends ViewLifecycle {

    @FXML
    private Label welcomeLabel;

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField messageField;

    @FXML
    private ListView<GameRoomProto.GameRoomData> roomListView;

    @FXML
    private Button sendButton;

    @FXML
    private AsyncButton createRoomButton;

    @FXML
    private AsyncButton joinRoomButton;

    @FXML
    private Button logoutButton;

    private GameConnectionManager gameConnectionManager = GameConnectionManager.getInstance();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> roomRefreshTask;

    private CreateRoomBo createRoomBo = null;

    @Override
    public void onShow(Object data) {
        // 从缓存中获取用户名
        String nickname = UserInfoManager.getInstance().getNickname();
        if (nickname != null && !nickname.isEmpty()) {
            welcomeLabel.setText("欢迎, " + nickname);
        }
        // 初始化按钮实践
        initCreateRoomButton();
        initJoinRoomButton();


        // 向服务端获取初始化数据
        initializeChat();
        queryRooms();

        // 创建定时任务调度器
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // 每5秒执行一次
        roomRefreshTask = scheduler.scheduleAtFixedRate(
                this::queryRooms,
                5,
                5,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void onHide() {
        // 停止定时任务
        if (roomRefreshTask != null && !roomRefreshTask.isCancelled()) {
            roomRefreshTask.cancel(false);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void initCreateRoomButton() {
        createRoomButton.setAction(() -> {
            // 创建房间弹窗
            Dialog<CreateRoomBo> dialog = new Dialog<>();
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
                    return new CreateRoomBo(roomNameField.getText().trim(), maxPlayersComboBox.getValue());
                }
                return null;
            });

            // 显示弹窗并等待用户响应
            Optional<CreateRoomBo> result = dialog.showAndWait();

            // 处理用户选择
            return result.map(data -> {
                // 这里可以添加实际的创建房间逻辑
                System.out.println("创建房间: " + data.roomName() + ", 最大人数: " + data.maxPlayers());
                return gameConnectionManager.sendAndListenFuture(GameMsgType.CREATE_ROOM, GameRoomProto.CreateRequest.newBuilder()
                        .setName(data.roomName())
                        .setMaxPlayers(data.maxPlayers())
                        .setPlayerId(UserInfoManager.getInstance().getPlayerId())
                        .build())
                        .thenApply(resp -> {
                            // 保存房间创建数据，用于后续组装 GameRoomData
                            createRoomBo = result.get();
                            return resp;
                        });
            }).orElseGet(() -> CompletableFuture.completedFuture(null));
        });
        createRoomButton.setOnSuccess(bo -> {
            CommonProto.BaseResponse resp = bo.getResponse();
            GameRoomProto.CreateResponse r = (GameRoomProto.CreateResponse) bo.getPayload();
            if (resp == null || resp.getCode() != ErrorCode.OK.getCode()) {
                MessageUtil.showError("创建房间失败，请检查房间名称或网络连接");
                return;
            }
            long roomId = r.getRoomId();
            String roomName = createRoomBo.roomName();
            int maxPlayers = createRoomBo.maxPlayers();
            
            // 获取创建者信息
            Long creatorId = UserInfoManager.getInstance().getPlayerId();
            String nickname = UserInfoManager.getInstance().getNickname();
            
            // 组装 GameRoomDetail 对象
            GameRoomProto.GameRoomDetail.Builder detailBuilder = GameRoomProto.GameRoomDetail.newBuilder()
                    .setId(roomId)
                    .setName(roomName)
                    .setMaxPlayers(maxPlayers)
                    .setCreatorId(creatorId)
                    .setStatus(GameRoomProto.RoomStatus.WAITING); // 初始状态为等待中
            
            // 添加创建者到玩家列表
            if (nickname != null && !nickname.isEmpty()) {
                GameRoomProto.PlayerInfo playerData = GameRoomProto.PlayerInfo.newBuilder()
                        .setPlayerId(creatorId)
                        .setNickName(nickname)
                        .build();
                detailBuilder.addPlayers(playerData);
            }
            
            GameRoomProto.GameRoomDetail roomDetail = detailBuilder.build();
            
            // 跳转进入房间页面，传递房间详情
            ViewManager.getInstance().show(ViewEnum.ROOM, roomDetail);
        });
        createRoomButton.setOnError(ex -> {
            // 创建房间失败
            MessageUtil.showError("创建房间失败，请检查网络连接或稍后再试");
        });
    }
    private void initJoinRoomButton() {
        joinRoomButton.setAction(() -> {
            // 获取选中的房间
            GameRoomProto.GameRoomData selectedRoom = roomListView.getSelectionModel().getSelectedItem();
            if (selectedRoom == null) {
                MessageUtil.showWarning("请先选择一个房间");
                return CompletableFuture.completedFuture(null);
            }
            
            long roomId = selectedRoom.getId();
            return gameConnectionManager.sendAndListenFuture(GameMsgType.JOIN_ROOM,
                    GameRoomProto.JoinRequest.newBuilder()
                            .setRoomId(roomId)
                            .setPlayerId(UserInfoManager.getInstance().getPlayerId())
                            .build());
        });
        joinRoomButton.setOnSuccess(responseBo -> {
            GameRoomProto.GameRoomDetail r = (GameRoomProto.GameRoomDetail) responseBo.getPayload();
            ViewManager.getInstance().show(ViewEnum.ROOM, r);
        });
        joinRoomButton.setOnError(ex -> {
            // 加入房间失败
            MessageUtil.showError("加入房间失败，请检查房间ID或网络连接");
        });
    }

    private void initializeChat() {
        // 添加一些假的聊天记录
        chatArea.appendText("系统消息: 欢迎来到大厅!\n");
        chatArea.appendText("管理员: 请遵守聊天规则，文明发言。\n");
        chatArea.appendText("玩家1: 有人一起开黑吗？\n");
        chatArea.appendText("玩家2: 我在! 我在!\n");
    }

    private void queryRooms() {
        gameConnectionManager.sendAndListenFuture(GameMsgType.PAGE_ROOM,
                GameRoomProto.PageRequest.newBuilder().setPageNum(1).setPageSize(10).build()
        ).thenAccept(responseBo -> {
            CommonProto.BaseResponse resp = responseBo.getResponse();
            if (resp.getCode() != ErrorCode.OK.getCode()) {
                throw new BusinessException(ErrorCode.of(resp.getCode()));
            }
            GameRoomProto.PageResponse r = ProtoBufUtil.parseRespBody(resp, GameRoomProto.PageResponse.parser());
            List<GameRoomProto.GameRoomData> dataList = r.getDataList();
            Platform.runLater(() -> {
                // 清空现有列表
                roomListView.getItems().clear();
                // 添加新数据（直接存储 GameRoomData 对象）
                roomListView.getItems().addAll(dataList);
                
                // 设置单元格工厂，自定义显示格式
                roomListView.setCellFactory(param -> new ListCell<GameRoomProto.GameRoomData>() {
                    @Override
                    protected void updateItem(GameRoomProto.GameRoomData item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName() + " - " + item.getStatus().name() + " (" + item.getNowPlayers() + "/" + item.getMaxPlayers() + ")");
                        }
                    }
                });
            });
        });
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
        ViewManager.getInstance().show(ViewEnum.LOGIN);
    }
}