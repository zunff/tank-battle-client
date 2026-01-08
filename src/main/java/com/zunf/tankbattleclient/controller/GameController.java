package com.zunf.tankbattleclient.controller;

import com.google.protobuf.ByteString;
import com.zunf.tankbattleclient.TankBattleApplication;
import com.zunf.tankbattleclient.constant.GameConstants;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.enums.Direction;
import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.manager.GameConnectionManager;
import com.zunf.tankbattleclient.manager.UserInfoManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.model.bo.BulletState;
import com.zunf.tankbattleclient.model.bo.TankState;
import com.zunf.tankbattleclient.protobuf.CommonProto;
import com.zunf.tankbattleclient.protobuf.game.match.MatchProto;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;
import com.zunf.tankbattleclient.util.AnimationHandler;
import com.zunf.tankbattleclient.util.CanvasScaler;
import com.zunf.tankbattleclient.util.GameRenderer;
import com.zunf.tankbattleclient.util.KeyRepeatHandler;
import com.zunf.tankbattleclient.util.MapDataProcessor;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 游戏界面控制器
 * 负责协调游戏各个模块，处理用户交互和游戏状态更新
 */
public class GameController extends ViewLifecycle {

    @FXML
    private BorderPane gameContainer;

    @FXML
    private Canvas gameCanvas;

    @FXML
    private VBox menuBox;

    @FXML
    private Label leaveGameLabel;

    // 游戏数据
    private GameRoomProto.StartNotice gameData;
    private long matchId;
    private boolean isFirstTick = true;

    // 游戏状态
    private Map<Long, TankState> tanks = new HashMap<>(); // playerId -> TankState
    private Map<String, BulletState> bullets = new HashMap<>(); // bulletId -> BulletState
    private byte[][] previousMapData; // 上一帧的地图数据
    private Map<String, Long> destroyedBrickAnimations = new HashMap<>(); // 正在播放摧毁动画的砖块位置 -> 动画开始时间

    // UI组件
    private CanvasScaler canvasScaler;
    private AnimationTimer animationTimer;
    private Consumer<com.google.protobuf.MessageLite> tickListener;

    // 按键处理
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private KeyRepeatHandler keyRepeatHandler;
    private javafx.event.EventHandler<KeyEvent> escKeyHandler;
    private javafx.event.EventHandler<KeyEvent> gameKeyHandler;
    private javafx.event.EventHandler<KeyEvent> gameKeyReleasedHandler;

    // 窗口宽高比控制
    private boolean adjustingAspectRatio = false;
    private javafx.beans.value.ChangeListener<Number> widthListener;
    private javafx.beans.value.ChangeListener<Number> heightListener;

    @Override
    public void onShow(Object data) {
        if (data instanceof GameRoomProto.StartNotice) {
            this.gameData = (GameRoomProto.StartNotice) data;
            this.matchId = gameData.getMatchId();

            loadStylesheet();
            setupWindowAspectRatio();
            initializeCanvas();
            initializePreviousMapData();
            renderGame();
            initEscKeyListener();
            initGameKeyListener();
            registerTickListener();
            startAnimationLoop();
        }
    }

    @Override
    public void onHide() {
        cleanup();
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        // 清理Canvas缩放器
        if (canvasScaler != null) {
            canvasScaler.cleanup();
            canvasScaler = null;
        }

        // 移除窗口宽高比监听器
        removeWindowAspectRatioListeners();

        // 移除ESC键监听
        removeEscKeyListener();

        // 移除游戏按键监听
        removeGameKeyListener();

        // 移除Tick消息监听
        unregisterTickListener();

        // 停止动画循环
        stopAnimationLoop();

        // 清理状态
        tanks.clear();
        bullets.clear();
        destroyedBrickAnimations.clear();
        previousMapData = null;
        gameData = null;
        isFirstTick = true;
    }

    /**
     * 移除窗口宽高比监听器
     */
    private void removeWindowAspectRatioListeners() {
        javafx.stage.Stage stage = ViewManager.getInstance().getStage();
        if (stage != null) {
            if (widthListener != null) {
                stage.widthProperty().removeListener(widthListener);
                widthListener = null;
            }
            if (heightListener != null) {
                stage.heightProperty().removeListener(heightListener);
                heightListener = null;
            }
        }
    }

    /**
     * 初始化Canvas
     */
    private void initializeCanvas() {
        if (gameContainer == null || gameCanvas == null) {
            return;
        }

        canvasScaler = new CanvasScaler(gameCanvas, gameContainer);
        canvasScaler.initialize();

        // 监听容器大小变化，触发重绘
        gameContainer.widthProperty().addListener((obs, oldVal, newVal) -> renderGame());
        gameContainer.heightProperty().addListener((obs, oldVal, newVal) -> renderGame());
    }

    /**
     * 设置窗口保持1:1宽高比
     */
    private void setupWindowAspectRatio() {
        javafx.stage.Stage stage = ViewManager.getInstance().getStage();
        if (stage == null) {
            return;
        }

        // 监听窗口宽度变化，调整高度保持1:1比例
        widthListener = (obs, oldWidth, newWidth) -> {
            if (adjustingAspectRatio || newWidth.doubleValue() <= 0) {
                return;
            }
            adjustingAspectRatio = true;
            stage.setHeight(newWidth.doubleValue());
            adjustingAspectRatio = false;
        };
        stage.widthProperty().addListener(widthListener);

        // 监听窗口高度变化，调整宽度保持1:1比例
        heightListener = (obs, oldHeight, newHeight) -> {
            if (adjustingAspectRatio || newHeight.doubleValue() <= 0) {
                return;
            }
            adjustingAspectRatio = true;
            stage.setWidth(newHeight.doubleValue());
            adjustingAspectRatio = false;
        };
        stage.heightProperty().addListener(heightListener);

        // 设置初始大小为1:1（如果当前不是1:1）
        double currentWidth = stage.getWidth();
        double currentHeight = stage.getHeight();
        if (currentWidth > 0 && currentHeight > 0 && Math.abs(currentWidth - currentHeight) > 1) {
            double size = Math.max(currentWidth, currentHeight);
            adjustingAspectRatio = true;
            stage.setWidth(size);
            stage.setHeight(size);
            adjustingAspectRatio = false;
        }
    }

    /**
     * 初始化previousMapData
     */
    private void initializePreviousMapData() {
        previousMapData = MapDataProcessor.initializePreviousMapData(gameData);
    }

    /**
     * 加载CSS样式表
     */
    private void loadStylesheet() {
        if (gameContainer != null) {
            Scene scene = gameContainer.getScene();
            if (scene != null) {
                scene.getStylesheets().add(
                        TankBattleApplication.class.getResource("css/game.css").toExternalForm());
            } else {
                gameContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        newScene.getStylesheets().add(
                                TankBattleApplication.class.getResource("css/game.css").toExternalForm());
                    }
                });
            }
        }
    }

    /**
     * 切换菜单框的显示/隐藏状态
     */
    private void toggleLeaveGameButton() {
        if (menuBox != null) {
            boolean isVisible = menuBox.isVisible();
            menuBox.setVisible(!isVisible);
            menuBox.setManaged(!isVisible);
        }
    }

    /**
     * 离开游戏点击事件
     */
    @FXML
    private void onLeaveGameClick(MouseEvent event) {
        Long playerId = UserInfoManager.getInstance().getPlayerId();
        if (playerId == null || matchId == 0) {
            // 如果没有有效的playerId或matchId，直接返回大厅
            ViewManager.getInstance().show(ViewEnum.LOBBY);
            return;
        }

        // 构建离开匹配请求
        MatchProto.LeaveMatchReq request = MatchProto.LeaveMatchReq.newBuilder()
                .setPlayerId(playerId)
                .setMatchId(matchId)
                .build();

        // 发送请求并等待响应
        GameConnectionManager.getInstance()
                .sendAndListenFuture(GameMsgType.LEAVE_MATCH, request, 5000)
                .thenAccept(responseBo -> {
                    // 在JavaFX应用线程中执行UI操作
                    javafx.application.Platform.runLater(() -> {
                        CommonProto.BaseResponse baseResponse = responseBo.getResponse();
                        if (baseResponse != null && baseResponse.getCode() == 0) {
                            // 服务器正常响应，返回大厅
                            ViewManager.getInstance().show(ViewEnum.LOBBY);
                        } else {
                            // 响应失败，显示错误信息（可选）
                            String errorMsg = baseResponse != null ? baseResponse.getMessage() : "离开游戏失败";
                            System.err.println("离开游戏失败: " + errorMsg);
                            // 即使失败也返回大厅（可选，根据业务需求决定）
                            ViewManager.getInstance().show(ViewEnum.LOBBY);
                        }
                    });
                })
                .exceptionally(throwable -> {
                    // 处理异常（超时或其他错误）
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("离开游戏请求异常: " + throwable.getMessage());
                        // 即使异常也返回大厅（可选，根据业务需求决定）
                        ViewManager.getInstance().show(ViewEnum.LOBBY);
                    });
                    return null;
                });
    }

    /**
     * 初始化ESC键监听
     */
    private void initEscKeyListener() {
        if (gameContainer == null) {
            return;
        }

        Scene scene = gameContainer.getScene();
        if (scene != null) {
            setupEscKeyListener(scene);
        } else {
            gameContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupEscKeyListener(newScene);
                }
            });
        }
    }

    /**
     * 设置ESC键监听器
     */
    private void setupEscKeyListener(Scene scene) {
        if (escKeyHandler == null) {
            escKeyHandler = event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    toggleLeaveGameButton();
                    event.consume();
                }
            };
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, escKeyHandler);
    }

    /**
     * 移除ESC键监听
     */
    private void removeEscKeyListener() {
        if (gameContainer != null && escKeyHandler != null) {
            Scene scene = gameContainer.getScene();
            if (scene != null) {
                scene.removeEventFilter(KeyEvent.KEY_PRESSED, escKeyHandler);
            }
        }
    }

    /**
     * 注册Tick消息监听
     */
    private void registerTickListener() {
        GameConnectionManager connectionManager = GameConnectionManager.getInstance();
        tickListener = (message) -> {
            if (message instanceof MatchProto.Tick) {
                MatchProto.Tick tick = (MatchProto.Tick) message;
                handleTick(tick);
            }
        };
        connectionManager.listenMessage(GameMsgType.GAME_TICK, tickListener);
    }

    /**
     * 取消注册Tick消息监听
     */
    private void unregisterTickListener() {
        if (tickListener != null) {
            GameConnectionManager connectionManager = GameConnectionManager.getInstance();
            connectionManager.removeListener(GameMsgType.GAME_TICK, tickListener);
            tickListener = null;
        }
    }

    /**
     * 处理Tick消息
     */
    private void handleTick(MatchProto.Tick tick) {
        // 更新坦克状态
        updateTanks(tick);

        // 更新子弹状态
        updateBullets(tick);

        // 更新地图数据
        if (tick.getMapDataCount() > 0) {
            updateMapData(tick.getMapDataList());
        } else if (isFirstTick && gameData != null && gameData.getMapDataCount() > 0) {
            initializePreviousMapData();
        }

        isFirstTick = false;
    }

    /**
     * 更新坦克状态
     */
    private void updateTanks(MatchProto.Tick tick) {
        for (MatchProto.Tank tank : tick.getTanksList()) {
            long playerId = tank.getPlayerId();
            double x = tank.getX();
            double y = tank.getY();
            Direction direction = Direction.values()[tank.getDirection()];
            int life = tank.getLife();

            TankState state = tanks.computeIfAbsent(playerId, k -> new TankState());

            // 检测方向变化（向左或向右转向）
            Direction oldDirection = state.getDirection();
            boolean directionChangedToLeftOrRight = !isFirstTick
                    && oldDirection != null
                    && direction != oldDirection
                    && (direction == Direction.LEFT || direction == Direction.RIGHT);

            // 检测血量变化，触发受击动画
            boolean isHitThisTick = false;
            if (!isFirstTick && state.getLife() > life) {
                isHitThisTick = true;
                state.setHit(true);
                state.setHitAnimationStartTime(System.currentTimeMillis());
                state.setPreviousLife(state.getLife());
            }

            // 如果满足显示血条的条件（受击或向左/向右转向），显示血条
            if (isHitThisTick || directionChangedToLeftOrRight) {
                state.setShowHealthBar(true);
                state.setHealthBarShowStartTime(System.currentTimeMillis());
            }

            // 更新血量
            state.setLife(life);
            state.setMaxLife(100);

            // 初始化previousLife用于动画
            if (isFirstTick) {
                state.setPreviousLife(life);
            }

            // 更新位置
            if (isFirstTick) {
                state.setCurrentX(x);
                state.setCurrentY(y);
                state.setTargetX(x);
                state.setTargetY(y);
            } else {
                if (state.getCurrentX() != x || state.getCurrentY() != y) {
                    state.setTargetX(x);
                    state.setTargetY(y);
                    state.setAnimating(true);
                }
            }
            state.setDirection(direction);
        }

        // 移除不存在的坦克
        tanks.entrySet()
                .removeIf(entry -> tick.getTanksList().stream().noneMatch(t -> t.getPlayerId() == entry.getKey()));
    }

    /**
     * 更新子弹状态
     */
    private void updateBullets(MatchProto.Tick tick) {
        Map<String, BulletState> newBullets = new HashMap<>();
        for (MatchProto.Bullet bullet : tick.getBulletsList()) {
            String bulletId = bullet.getPlayerId() + "_" + bullet.getBulletId();
            double x = bullet.getX();
            double y = bullet.getY();
            Direction direction = Direction.values()[bullet.getDirection()];

            BulletState state = bullets.get(bulletId);
            if (state == null) {
                // 新增子弹，直接创建并设置位置，不启动动画
                state = new BulletState();
                state.setCurrentX(x);
                state.setCurrentY(y);
                state.setTargetX(x);
                state.setTargetY(y);
                state.setAnimating(false);
            } else if (isFirstTick) {
                // 第一个 tick直接设置位置
                state.setCurrentX(x);
                state.setCurrentY(y);
                state.setTargetX(x);
                state.setTargetY(y);
                state.setAnimating(false);
            } else {
                // 已存在的子弹，检查位置是否变化
                if (state.getCurrentX() != x || state.getCurrentY() != y) {
                    state.setTargetX(x);
                    state.setTargetY(y);
                    state.setAnimating(true);
                } else {
                    state.setAnimating(false);
                }
            }
            state.setDirection(direction);
            newBullets.put(bulletId, state);
        }
        bullets = newBullets;
    }

    /**
     * 更新地图数据
     */
    private void updateMapData(java.util.List<ByteString> mapDataList) {
        previousMapData = MapDataProcessor.updateMapData(
                mapDataList, previousMapData, destroyedBrickAnimations, isFirstTick);

        // 更新gameData中的地图数据
        if (gameData != null) {
            gameData = MapDataProcessor.updateGameDataMap(gameData, mapDataList);
        }
    }

    /**
     * 启动动画循环
     */
    private void startAnimationLoop() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAnimations();
                renderGame();
            }
        };
        animationTimer.start();
    }

    /**
     * 停止动画循环
     */
    private void stopAnimationLoop() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    /**
     * 更新动画状态
     */
    private void updateAnimations() {
        AnimationHandler.updateTankAnimations(tanks.values());
        AnimationHandler.updateBulletAnimations(bullets.values());
        AnimationHandler.cleanupBrickAnimations(destroyedBrickAnimations);
    }

    /**
     * 渲染游戏（包括地图、坦克、子弹）
     */
    private void renderGame() {
        if (gameCanvas == null) {
            return;
        }

        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, GameConstants.CANVAS_SIZE, GameConstants.CANVAS_SIZE);

        // 渲染地图
        GameRenderer.renderMapBackground(gc, gameData, destroyedBrickAnimations);

        // 渲染坦克
        Long myPlayerId = UserInfoManager.getInstance().getPlayerId();
        for (Map.Entry<Long, TankState> entry : tanks.entrySet()) {
            long playerId = entry.getKey();
            TankState state = entry.getValue();
            boolean isMyTank = myPlayerId != null && playerId == myPlayerId;
            GameRenderer.renderTank(gc, state.getCurrentX(), state.getCurrentY(),
                    state.getDirection(), state, isMyTank);
        }

        // 渲染子弹
        for (BulletState state : bullets.values()) {
            GameRenderer.renderBullet(gc, state.getCurrentX(), state.getCurrentY(), state.getDirection());
        }
    }

    /**
     * 初始化游戏按键监听（WASD移动，空格射击）
     */
    private void initGameKeyListener() {
        if (gameContainer == null) {
            return;
        }

        Scene scene = gameContainer.getScene();
        if (scene != null) {
            setupGameKeyListener(scene);
        } else {
            gameContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupGameKeyListener(newScene);
                }
            });
        }
    }

    /**
     * 设置游戏按键监听器
     */
    private void setupGameKeyListener(Scene scene) {
        // 初始化按键重复处理器
        keyRepeatHandler = new KeyRepeatHandler(pressedKeys, this::handleTankMove);

        if (gameKeyHandler == null) {
            gameKeyHandler = event -> {
                KeyCode code = event.getCode();
                if (code == KeyCode.W || code == KeyCode.A || code == KeyCode.S || code == KeyCode.D) {
                    if (pressedKeys.add(code)) {
                        // 新按下的键，立即发送消息
                        handleTankMove(code);
                        // 启动重复定时器
                        if (pressedKeys.size() == 1) {
                            keyRepeatHandler.start();
                        }
                    }
                    event.consume();
                } else if (code == KeyCode.SPACE) {
                    handleTankShoot();
                    event.consume();
                }
            };

            gameKeyReleasedHandler = event -> {
                KeyCode code = event.getCode();
                if (code == KeyCode.W || code == KeyCode.A || code == KeyCode.S || code == KeyCode.D) {
                    pressedKeys.remove(code);
                    if (pressedKeys.isEmpty()) {
                        keyRepeatHandler.stop();
                    }
                    event.consume();
                }
            };
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, gameKeyHandler);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, gameKeyReleasedHandler);
    }

    /**
     * 移除游戏按键监听
     */
    private void removeGameKeyListener() {
        if (keyRepeatHandler != null) {
            keyRepeatHandler.stop();
            keyRepeatHandler = null;
        }
        pressedKeys.clear();

        if (gameContainer != null && gameKeyHandler != null) {
            Scene scene = gameContainer.getScene();
            if (scene != null) {
                scene.removeEventFilter(KeyEvent.KEY_PRESSED, gameKeyHandler);
                if (gameKeyReleasedHandler != null) {
                    scene.removeEventFilter(KeyEvent.KEY_RELEASED, gameKeyReleasedHandler);
                }
            }
        }
    }

    /**
     * 处理坦克移动
     */
    private void handleTankMove(KeyCode keyCode) {
        Direction direction = switch (keyCode) {
            case W -> Direction.UP;
            case S -> Direction.DOWN;
            case A -> Direction.LEFT;
            case D -> Direction.RIGHT;
            default -> null;
        };

        if (direction == null) {
            return;
        }

        Long playerId = UserInfoManager.getInstance().getPlayerId();
        if (playerId == null || matchId == 0) {
            return;
        }

        MatchProto.OpRequest request = MatchProto.OpRequest.newBuilder()
                .setPlayerId(playerId)
                .setMatchId(matchId)
                .setOpParams(MatchProto.OpParams.newBuilder()
                        .setTankDirection(direction.ordinal())
                        .build())
                .build();

        GameConnectionManager.getInstance().send(GameMsgType.TANK_MOVE, request);
    }

    /**
     * 处理坦克射击
     */
    private void handleTankShoot() {
        Long playerId = UserInfoManager.getInstance().getPlayerId();
        if (playerId == null || matchId == 0) {
            return;
        }

        MatchProto.OpRequest request = MatchProto.OpRequest.newBuilder()
                .setPlayerId(playerId)
                .setMatchId(matchId)
                .build();

        GameConnectionManager.getInstance().send(GameMsgType.TANK_SHOOT, request);
    }
}
