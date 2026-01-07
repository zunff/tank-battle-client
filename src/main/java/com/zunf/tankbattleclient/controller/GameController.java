package com.zunf.tankbattleclient.controller;

import com.google.protobuf.ByteString;
import com.zunf.tankbattleclient.TankBattleApplication;
import com.zunf.tankbattleclient.constant.GameConstants;
import com.zunf.tankbattleclient.enums.GameMsgType;
import com.zunf.tankbattleclient.enums.MapIndex;
import com.zunf.tankbattleclient.enums.Direction;
import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.manager.GameConnectionManager;
import com.zunf.tankbattleclient.manager.UserInfoManager;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.model.bo.AnimationXyState;
import com.zunf.tankbattleclient.model.bo.BulletState;
import com.zunf.tankbattleclient.model.bo.TankState;
import com.zunf.tankbattleclient.protobuf.game.match.MatchProto;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * 游戏界面控制器
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

    private GameRoomProto.StartNotice gameData;
    private ChangeListener<Number> canvasSizeListener;
    private javafx.event.EventHandler<KeyEvent> escKeyHandler;
    private javafx.event.EventHandler<KeyEvent> gameKeyHandler;
    private javafx.event.EventHandler<KeyEvent> gameKeyReleasedHandler;
    private boolean adjustingAspectRatio = false; // 防止循环调整的标志
    private Consumer<com.google.protobuf.MessageLite> tickListener; // Tick消息监听器

    // 按键状态跟踪（用于长按检测）
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private javafx.animation.Timeline keyRepeatTimeline;

    // 游戏状态
    private long matchId;
    private Map<Long, TankState> tanks = new HashMap<>(); // playerId -> TankState
    private Map<String, BulletState> bullets = new HashMap<>(); // bulletId -> BulletState
    private boolean isFirstTick = true;
    private AnimationTimer animationTimer;

    // 地图状态（用于检测砖块摧毁）
    private byte[][] previousMapData = null; // 上一帧的地图数据
    private Map<String, Long> destroyedBrickAnimations = new HashMap<>(); // 正在播放摧毁动画的砖块位置 -> 动画开始时间

    // 地图常量
    private static final int MAP_SIZE = 32; // 地图大小 32x32
    private static final int CELL_SIZE = 32; // 每个格子固定32px
    private static final int CANVAS_SIZE = MAP_SIZE * CELL_SIZE; // Canvas大小 1024x1024

    @Override
    public void onShow(Object data) {
        if (data instanceof GameRoomProto.StartNotice) {
            this.gameData = (GameRoomProto.StartNotice) data;
            this.matchId = gameData.getMatchId();
            // 加载CSS样式
            loadStylesheet();
            // 设置窗口保持1:1宽高比
            setupWindowAspectRatio();
            // 绑定Canvas大小到容器
            bindCanvasSize();
            // 初始化previousMapData
            initializePreviousMapData();
            renderMap();
            // 初始化ESC键监听
            initEscKeyListener();
            // 初始化游戏按键监听
            initGameKeyListener();
            // 注册Tick消息监听
            registerTickListener();
            // 启动动画循环
            startAnimationLoop();
        }
    }

    @Override
    public void onHide() {
        // 移除监听器
        if (canvasSizeListener != null && gameContainer != null) {
            gameContainer.widthProperty().removeListener(canvasSizeListener);
            gameContainer.heightProperty().removeListener(canvasSizeListener);
        }
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
     * 设置窗口保持1:1宽高比
     */
    private void setupWindowAspectRatio() {
        javafx.stage.Stage stage = ViewManager.getInstance().getStage();
        if (stage == null) {
            return;
        }

        // 监听窗口宽度变化，调整高度保持1:1比例
        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (adjustingAspectRatio || newWidth.doubleValue() <= 0) {
                return;
            }
            adjustingAspectRatio = true;
            stage.setHeight(newWidth.doubleValue());
            adjustingAspectRatio = false;
        });

        // 监听窗口高度变化，调整宽度保持1:1比例
        stage.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            if (adjustingAspectRatio || newHeight.doubleValue() <= 0) {
                return;
            }
            adjustingAspectRatio = true;
            stage.setWidth(newHeight.doubleValue());
            adjustingAspectRatio = false;
        });

        // 设置初始大小为1:1（如果当前不是1:1）
        double currentWidth = stage.getWidth();
        double currentHeight = stage.getHeight();
        if (currentWidth > 0 && currentHeight > 0 && Math.abs(currentWidth - currentHeight) > 1) {
            // 使用较大的值作为基准，保持1:1
            double size = Math.max(currentWidth, currentHeight);
            adjustingAspectRatio = true;
            stage.setWidth(size);
            stage.setHeight(size);
            adjustingAspectRatio = false;
        }
    }

    /**
     * 绑定Canvas大小到容器
     */
    private void bindCanvasSize() {
        if (gameContainer == null || gameCanvas == null) {
            return;
        }

        // Canvas固定逻辑大小为1024x1024（32x32格子，每个32px）
        gameCanvas.setWidth(CANVAS_SIZE);
        gameCanvas.setHeight(CANVAS_SIZE);

        // 使用Scale变换来缩放Canvas以适应容器，保持1:1宽高比
        Scale scale = new Scale();
        gameCanvas.getTransforms().add(scale);

        // 监听容器大小变化，调整缩放比例
        canvasSizeListener = (obs, oldVal, newVal) -> {
            updateCanvasScale(scale);
            renderMap();
        };
        gameContainer.widthProperty().addListener(canvasSizeListener);
        gameContainer.heightProperty().addListener(canvasSizeListener);

        // 初始设置缩放
        updateCanvasScale(scale);
    }

    /**
     * 更新Canvas的缩放比例，使其适应容器并保持1:1宽高比
     */
    private void updateCanvasScale(Scale scale) {
        if (gameContainer == null || gameCanvas == null) {
            return;
        }

        double containerWidth = gameContainer.getWidth();
        double containerHeight = gameContainer.getHeight();

        if (containerWidth <= 0 || containerHeight <= 0) {
            return;
        }

        // 计算缩放比例，保持1:1宽高比，适应容器
        double scaleX = containerWidth / CANVAS_SIZE;
        double scaleY = containerHeight / CANVAS_SIZE;
        double scaleValue = Math.min(scaleX, scaleY); // 取较小的值，确保完全显示

        scale.setX(scaleValue);
        scale.setY(scaleValue);
        scale.setPivotX(0);
        scale.setPivotY(0);

        // 调整Canvas在StackPane中的位置，使其居中
        StackPane parent = (StackPane) gameCanvas.getParent();
        if (parent != null) {
            // Canvas缩放后的实际大小
            double scaledWidth = CANVAS_SIZE * scaleValue;
            double scaledHeight = CANVAS_SIZE * scaleValue;

            // 计算偏移量使Canvas居中
            double offsetX = (containerWidth - scaledWidth) / 2;
            double offsetY = (containerHeight - scaledHeight) / 2;

            gameCanvas.setLayoutX(offsetX);
            gameCanvas.setLayoutY(offsetY);
        }
    }

    /**
     * 渲染地图（初始渲染，后续由动画循环处理）
     */
    private void renderMap() {
        if (gameCanvas == null) {
            return;
        }
        // 初始渲染，后续由动画循环的renderGame()处理
        renderGame();
    }

    /**
     * 根据单元格类型获取颜色
     */
    private Color getCellColor(MapIndex cellType) {
        return switch (cellType) {
            case MapIndex.EMPTY -> Color.LIGHTGRAY; // 空地 - 浅灰色
            case MapIndex.WALL -> Color.DARKGRAY; // 不可破坏墙 - 深灰色
            case MapIndex.BRICK -> Color.SADDLEBROWN; // 可破坏砖块 - 棕色
            case MapIndex.SPAWN -> Color.LIGHTGREEN; // 出生点 - 浅绿色
            case MapIndex.DESTROYED_WALL -> Color.LIGHTGRAY;
            default -> Color.WHITE; // 未知类型 - 白色
        };
    }

    /**
     * 初始化ESC键监听，用于显示/隐藏离开游戏按钮
     */
    private void initEscKeyListener() {
        if (gameContainer == null) {
            return;
        }

        Scene scene = gameContainer.getScene();
        if (scene == null) {
            // 如果场景还未初始化，等待场景初始化后再添加监听
            gameContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupEscKeyListener(newScene);
                }
            });
        } else {
            setupEscKeyListener(scene);
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
                    event.consume(); // 阻止事件继续传播
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
     * 加载CSS样式表
     */
    private void loadStylesheet() {
        if (gameContainer != null) {
            Scene scene = gameContainer.getScene();
            if (scene != null) {
                scene.getStylesheets().add(
                        TankBattleApplication.class.getResource("css/game.css").toExternalForm());
            } else {
                // 如果场景还未初始化，等待场景初始化后再加载样式
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
        // 直接跳转到大厅，不发送消息给服务端
        ViewManager.getInstance().show(ViewEnum.LOBBY);
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
        for (MatchProto.Tank tank : tick.getTanksList()) {
            long playerId = tank.getPlayerId();
            double x = tank.getX();
            double y = tank.getY();
            Direction direction = Direction.values()[tank.getDirection()];
            int life = tank.getLife();

            TankState state = tanks.computeIfAbsent(playerId, k -> new TankState());

            // 检测方向变化（向左或向右转向）
            Direction oldDirection = state.getDirection();
            boolean directionChangedToLeftOrRight = false;
            if (!isFirstTick && oldDirection != null && direction != oldDirection) {
                // 方向变化，且新方向是向左或向右
                if (direction == Direction.LEFT || direction == Direction.RIGHT) {
                    directionChangedToLeftOrRight = true;
                }
            }

            // 检测血量变化，触发受击动画
            boolean isHitThisTick = false;
            if (!isFirstTick && state.getLife() > life) {
                isHitThisTick = true;
                state.setHit(true);
                state.setHitAnimationStartTime(System.currentTimeMillis());
                state.setPreviousLife(state.getLife());
            }

            // 如果满足显示血条的条件（受击或向左/向右转向），显示血条3秒
            if (isHitThisTick || directionChangedToLeftOrRight) {
                state.setShowHealthBar(true);
                state.setHealthBarShowStartTime(System.currentTimeMillis());
            }

            // 更新血量
            int oldLife = state.getLife();
            state.setLife(life);
            state.setMaxLife(100); // 最大血量为100

            // 如果血量变化，初始化previousLife用于动画
            if (isFirstTick) {
                state.setPreviousLife(life);
            } else if (oldLife != life && state.getPreviousLife() == oldLife) {
                // 血量变化时，如果previousLife还是旧值，保持当前值让动画平滑过渡
                // 动画会在updateAnimations中处理
            }

            if (isFirstTick) {
                // 第一个 tick直接设置位置和血量
                state.setCurrentX(x);
                state.setCurrentY(y);
                state.setTargetX(x);
                state.setTargetY(y);
                state.setPreviousLife(life);
            } else {
                // 后续 tick检查是否需要动画
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

        // 更新子弹状态
        Map<String, BulletState> newBullets = new HashMap<>();
        for (MatchProto.Bullet bullet : tick.getBulletsList()) {
            String bulletId = bullet.getPlayerId() + "_" + bullet.getBulletId();
            double x = bullet.getX();
            double y = bullet.getY();
            Direction direction = Direction.values()[bullet.getDirection()];

            // 获取已存在的子弹状态，如果不存在则是新增的
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
                    // 位置变化，启动动画
                    state.setTargetX(x);
                    state.setTargetY(y);
                    state.setAnimating(true);
                } else {
                    // 位置没变化，确保动画状态为false
                    state.setAnimating(false);
                }
            }
            state.setDirection(direction);
            newBullets.put(bulletId, state);
        }
        bullets = newBullets; // 更新子弹列表

        // 更新地图数据（如果有）
        if (tick.getMapDataCount() > 0) {
            updateMapData(tick.getMapDataList());
        } else if (isFirstTick && gameData != null && gameData.getMapDataCount() > 0) {
            // 第一个tick如果没有地图数据，使用初始地图数据
            initializePreviousMapData();
        }

        isFirstTick = false;
    }

    /**
     * 初始化previousMapData（从gameData）
     */
    private void initializePreviousMapData() {
        if (gameData == null || gameData.getMapDataCount() == 0) {
            return;
        }

        int mapHeight = gameData.getMapDataCount();
        int mapWidth = gameData.getMapData(0).size();
        previousMapData = new byte[mapHeight][mapWidth];

        for (int row = 0; row < mapHeight && row < MAP_SIZE; row++) {
            ByteString rowData = gameData.getMapData(row);
            byte[] rowBytes = rowData.toByteArray();
            for (int col = 0; col < rowBytes.length && col < mapWidth && col < MAP_SIZE; col++) {
                previousMapData[row][col] = rowBytes[col];
            }
        }
    }

    /**
     * 更新地图数据，检测新的已摧毁砖块
     */
    private void updateMapData(java.util.List<ByteString> mapDataList) {
        if (mapDataList == null || mapDataList.isEmpty()) {
            return;
        }

        int mapHeight = mapDataList.size();
        if (mapHeight == 0) {
            return;
        }

        int mapWidth = mapDataList.get(0).size();
        byte[][] currentMapData = new byte[mapHeight][mapWidth];

        // 解析当前地图数据
        for (int row = 0; row < mapHeight && row < MAP_SIZE; row++) {
            ByteString rowData = mapDataList.get(row);
            byte[] rowBytes = rowData.toByteArray();
            for (int col = 0; col < rowBytes.length && col < mapWidth && col < MAP_SIZE; col++) {
                currentMapData[row][col] = rowBytes[col];
            }
        }

        // 检测新的已摧毁砖块
        if (previousMapData != null && !isFirstTick) {
            for (int row = 0; row < mapHeight && row < MAP_SIZE; row++) {
                for (int col = 0; col < mapWidth && col < MAP_SIZE; col++) {
                    byte previousType = previousMapData[row][col];
                    byte currentType = currentMapData[row][col];

                    // 如果之前是可破坏墙(BRICK)，现在是已破坏墙(DESTROYED_WALL)或空地(EMPTY)，触发摧毁动画
                    if (previousType == MapIndex.BRICK.getCode() &&
                            (currentType == MapIndex.DESTROYED_WALL.getCode() ||
                                    currentType == MapIndex.EMPTY.getCode())) {
                        String key = row + "_" + col;
                        destroyedBrickAnimations.put(key, System.currentTimeMillis());
                    }
                }
            }
        }

        // 更新gameData中的地图数据
        if (gameData != null) {
            // 创建新的StartNotice.Builder来更新地图数据
            GameRoomProto.StartNotice.Builder builder = gameData.toBuilder();
            builder.clearMapData();
            for (ByteString rowData : mapDataList) {
                builder.addMapData(rowData);
            }
            gameData = builder.build();
        }

        // 保存当前地图数据作为下一帧的previousMapData
        previousMapData = currentMapData;
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
        // 更新坦克动画
        for (TankState state : tanks.values()) {
            if (state.isAnimating()) {
                handlerAnimationState(state, GameConstants.TANK_ANIMATION_SPEED);
            }

            // 更新受击动画
            if (state.isHit()) {
                long elapsed = System.currentTimeMillis() - state.getHitAnimationStartTime();
                if (elapsed > 300) { // 受击动画持续300ms
                    state.setHit(false);
                }
            }

            // 更新血条显示状态（3秒后隐藏）
            if (state.isShowHealthBar()) {
                long elapsed = System.currentTimeMillis() - state.getHealthBarShowStartTime();
                if (elapsed > 500) { // 血条显示时间
                    state.setShowHealthBar(false);
                }
            }

            // 更新血条动画（平滑变化）
            double currentDisplayLife = state.getPreviousLife();
            double targetLife = state.getLife();
            double diff = targetLife - currentDisplayLife;

            if (Math.abs(diff) > 0.1) {
                // 平滑变化（增加或减少）
                state.setPreviousLife(currentDisplayLife + diff * 0.1);
            } else {
                // 接近目标值，直接设置
                state.setPreviousLife(targetLife);
            }
        }

        // 更新子弹动画
        for (BulletState state : bullets.values()) {
            if (state.isAnimating()) {
                handlerAnimationState(state, GameConstants.BULLET_ANIMATION_SPEED);
            }
        }

        // 更新砖块摧毁动画（移除过期的动画）
        long currentTime = System.currentTimeMillis();
        destroyedBrickAnimations.entrySet().removeIf(entry -> currentTime - entry.getValue() > 500 // 动画持续500ms
        );
    }

    private void handlerAnimationState(AnimationXyState state, double speed) {
        double dx = state.getTargetX() - state.getCurrentX();
        double dy = state.getTargetY() - state.getCurrentY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < speed) {
            // 到达目标位置
            state.setCurrentX(state.getTargetX());
            state.setCurrentY(state.getTargetY());
            state.setAnimating(false);
        } else {
            // 继续移动
            double moveX = (dx / distance) * speed;
            double moveY = (dy / distance) * speed;
            state.setCurrentX(state.getCurrentX() + moveX);
            state.setCurrentY(state.getCurrentY() + moveY);
        }
    }

    /**
     * 渲染游戏（包括地图、坦克、子弹）
     */
    private void renderGame() {
        if (gameCanvas == null) {
            return;
        }

        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        // 渲染地图
        renderMapBackground(gc);

        // 渲染坦克
        Long myPlayerId = UserInfoManager.getInstance().getPlayerId();
        for (Map.Entry<Long, TankState> entry : tanks.entrySet()) {
            long playerId = entry.getKey();
            TankState state = entry.getValue();
            boolean isMyTank = myPlayerId != null && playerId == myPlayerId;
            renderTankAtPosition(gc, state.getCurrentX(), state.getCurrentY(), state.getDirection(), state, isMyTank);
        }

        // 渲染子弹
        for (BulletState state : bullets.values()) {
            renderBulletAtPosition(gc, state.getCurrentX(), state.getCurrentY(), state.getDirection());
        }
    }

    /**
     * 渲染地图背景
     */
    private void renderMapBackground(GraphicsContext gc) {
        if (gameData == null || gameData.getMapDataCount() == 0) {
            return;
        }

        int mapHeight = gameData.getMapDataCount();
        int mapWidth = gameData.getMapData(0).size();

        for (int row = 0; row < mapHeight && row < MAP_SIZE; row++) {
            ByteString rowData = gameData.getMapData(row);
            byte[] rowBytes = rowData.toByteArray();

            for (int col = 0; col < rowBytes.length && col < mapWidth && col < MAP_SIZE; col++) {
                byte cellType = rowBytes[col];
                double x = col * CELL_SIZE;
                double y = row * CELL_SIZE;

                String key = row + "_" + col;
                boolean isDestroying = destroyedBrickAnimations.containsKey(key);

                // 如果正在播放摧毁动画，显示动画效果
                if (isDestroying) {
                    long elapsed = System.currentTimeMillis() - destroyedBrickAnimations.get(key);
                    double progress = Math.min(elapsed / 500.0, 1.0); // 0-1的进度

                    // 摧毁动画：闪烁和缩放效果
                    double alpha = 1.0 - progress;
                    double scale = 1.0 - progress * 0.5; // 缩小到50%

                    // 绘制原始砖块（带透明度）
                    Color brickColor = getCellColor(MapIndex.BRICK);
                    gc.setGlobalAlpha(alpha);
                    gc.setFill(brickColor);
                    double offsetX = (CELL_SIZE - CELL_SIZE * scale) / 2;
                    double offsetY = (CELL_SIZE - CELL_SIZE * scale) / 2;
                    gc.fillRect(x + offsetX, y + offsetY, CELL_SIZE * scale, CELL_SIZE * scale);
                    gc.setGlobalAlpha(1.0);

                    // 如果动画完成，显示为空地
                    if (progress >= 1.0) {
                        cellType = MapIndex.EMPTY.getCode();
                    }
                }

                Color cellColor = getCellColor(MapIndex.of(cellType));
                gc.setFill(cellColor);
                gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                // 只对不可破坏墙和可破坏墙绘制边框，已摧毁墙、空地和正在摧毁的砖块不绘制边框
                byte finalCellType = cellType;
                if (!isDestroying
                        && finalCellType != MapIndex.DESTROYED_WALL.getCode()
                        && finalCellType != MapIndex.EMPTY.getCode()
                        && (finalCellType == MapIndex.WALL.getCode() || finalCellType == MapIndex.BRICK.getCode())) {
                    gc.setStroke(Color.GRAY);
                    gc.setLineWidth(0.5);
                    gc.strokeRect(x, y, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    /**
     * 在指定位置渲染坦克
     */
    private void renderTankAtPosition(GraphicsContext gc, double x, double y, Direction direction, TankState state,
            boolean isMyTank) {
        // 受击动画效果
        boolean isHit = state != null && state.isHit();
        if (isHit) {
            // 受击时闪烁效果
            long elapsed = System.currentTimeMillis() - state.getHitAnimationStartTime();
            double alpha = 0.5 + 0.5 * Math.sin(elapsed / 50.0); // 闪烁效果
            gc.setGlobalAlpha(alpha);
        }

        // 坦克大小
        double baseSize = CELL_SIZE * 0.8;
        double longSide = baseSize * 1.2;
        double shortSide = baseSize * 0.85;

        double rectWidth, rectHeight;
        if (direction == Direction.UP || direction == Direction.DOWN) {
            rectWidth = shortSide;
            rectHeight = longSide;
        } else {
            rectWidth = longSide;
            rectHeight = shortSide;
        }

        double circleRadius = Math.min(rectWidth, rectHeight) * 0.4;

        // 根据是否自己的坦克和受击状态选择颜色
        // 自己的坦克：正常绿色，受击黄色
        // 其他坦克：正常红色，受击灰色
        Color bodyColor, circleColor, barrelColor;
        if (isMyTank) {
            if (isHit) {
                bodyColor = Color.YELLOW;
                circleColor = Color.YELLOW;
                barrelColor = Color.YELLOW;
            } else {
                bodyColor = Color.GREEN;
                circleColor = Color.DARKGREEN;
                barrelColor = Color.DARKGREEN;
            }
        } else {
            if (isHit) {
                bodyColor = Color.GRAY;
                circleColor = Color.GRAY;
                barrelColor = Color.GRAY;
            } else {
                bodyColor = Color.RED;
                circleColor = Color.DARKRED;
                barrelColor = Color.DARKRED;
            }
        }

        // 绘制长方形坦克主体
        gc.setFill(bodyColor);
        gc.fillRect(x - rectWidth / 2, y - rectHeight / 2, rectWidth, rectHeight);

        // 绘制炮管
        double barrelWidth = shortSide * 0.35;
        double barrelLength = longSide * 0.5;

        double barrelX, barrelY, barrelW, barrelH;
        switch (direction) {
            case UP:
                barrelW = barrelWidth;
                barrelH = barrelLength;
                barrelX = x - barrelW / 2;
                barrelY = y - circleRadius - barrelH;
                break;
            case DOWN:
                barrelW = barrelWidth;
                barrelH = barrelLength;
                barrelX = x - barrelW / 2;
                barrelY = y + circleRadius;
                break;
            case LEFT:
                barrelW = barrelLength;
                barrelH = barrelWidth;
                barrelX = x - circleRadius - barrelW;
                barrelY = y - barrelH / 2;
                break;
            case RIGHT:
                barrelW = barrelLength;
                barrelH = barrelWidth;
                barrelX = x + circleRadius;
                barrelY = y - barrelH / 2;
                break;
            default:
                return;
        }

        // 绘制炮管
        gc.setFill(barrelColor);
        gc.fillRect(barrelX, barrelY, barrelW, barrelH);

        // 绘制圆形
        gc.setFill(circleColor);
        gc.fillOval(x - circleRadius, y - circleRadius, circleRadius * 2, circleRadius * 2);

        // 绘制边框
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeRect(x - rectWidth / 2, y - rectHeight / 2, rectWidth, rectHeight);
        gc.strokeOval(x - circleRadius, y - circleRadius, circleRadius * 2, circleRadius * 2);

        // 恢复透明度
        if (isHit) {
            gc.setGlobalAlpha(1.0);
        }

        // 绘制血条
        if (state != null) {
            renderHealthBar(gc, x, y, rectHeight, state);
        }
    }

    /**
     * 渲染坦克血条
     */
    private void renderHealthBar(GraphicsContext gc, double tankX, double tankY, double tankHeight, TankState state) {
        // 只在满足显示条件时显示血条
        if (!state.isShowHealthBar()) {
            return;
        }

        double healthBarWidth = CELL_SIZE * 0.8;
        double healthBarHeight = 4;
        double healthBarX = tankX - healthBarWidth / 2;
        double healthBarY = tankY - tankHeight / 2 - 8; // 在坦克上方

        // 血条背景（灰色）
        gc.setFill(Color.GRAY);
        gc.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

        // 当前血量（平滑动画）
        double currentLife = state.getPreviousLife();
        double maxLife = state.getMaxLife();
        double healthRatio = Math.max(0, Math.min(1, currentLife / maxLife));
        double healthBarFillWidth = healthBarWidth * healthRatio;

        // 根据血量比例选择颜色
        Color healthColor;
        if (healthRatio > 0.6) {
            healthColor = Color.GREEN;
        } else if (healthRatio > 0.3) {
            healthColor = Color.ORANGE;
        } else {
            healthColor = Color.RED;
        }

        // 绘制血量条
        gc.setFill(healthColor);
        gc.fillRect(healthBarX, healthBarY, healthBarFillWidth, healthBarHeight);

        // 血条边框
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);
        gc.strokeRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
    }

    /**
     * 在指定位置渲染子弹
     */
    private void renderBulletAtPosition(GraphicsContext gc, double x, double y, Direction direction) {
        double bulletSize = CELL_SIZE * 0.3;
        gc.setFill(Color.YELLOW);
        gc.fillOval(x - bulletSize / 2, y - bulletSize / 2, bulletSize, bulletSize);
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(1.0);
        gc.strokeOval(x - bulletSize / 2, y - bulletSize / 2, bulletSize, bulletSize);
    }

    /**
     * 初始化游戏按键监听（WASD移动，空格射击）
     */
    private void initGameKeyListener() {
        if (gameContainer == null) {
            return;
        }

        Scene scene = gameContainer.getScene();
        if (scene == null) {
            gameContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupGameKeyListener(newScene);
                }
            });
        } else {
            setupGameKeyListener(scene);
        }
    }

    /**
     * 设置游戏按键监听器
     */
    private void setupGameKeyListener(Scene scene) {
        if (gameKeyHandler == null) {
            gameKeyHandler = event -> {
                KeyCode code = event.getCode();
                if (code == KeyCode.W || code == KeyCode.A || code == KeyCode.S || code == KeyCode.D) {
                    // 添加到按下的键集合
                    if (pressedKeys.add(code)) {
                        // 新按下的键，立即发送消息
                        handleTankMove(code);
                        // 如果还没有启动重复定时器，启动它
                        startKeyRepeat();
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
                    // 从按下的键集合中移除
                    pressedKeys.remove(code);
                    // 如果没有按下的移动键了，停止重复定时器
                    if (pressedKeys.isEmpty()) {
                        stopKeyRepeat();
                    }
                    event.consume();
                }
            };
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, gameKeyHandler);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, gameKeyReleasedHandler);
    }

    /**
     * 开始按键重复（持续移动）
     */
    private void startKeyRepeat() {
        stopKeyRepeat(); // 先停止之前的定时器

        keyRepeatTimeline = new Timeline(
                new KeyFrame(
                        Duration.millis(50), // 每50ms发送一次移动消息
                        e -> {
                            // 获取优先级最高的移动键（W > S > A > D）
                            KeyCode priorityKey = null;
                            if (pressedKeys.contains(KeyCode.W)) {
                                priorityKey = KeyCode.W;
                            } else if (pressedKeys.contains(KeyCode.S)) {
                                priorityKey = KeyCode.S;
                            } else if (pressedKeys.contains(KeyCode.A)) {
                                priorityKey = KeyCode.A;
                            } else if (pressedKeys.contains(KeyCode.D)) {
                                priorityKey = KeyCode.D;
                            }

                            if (priorityKey != null) {
                                handleTankMove(priorityKey);
                            }
                        }));
        keyRepeatTimeline.setCycleCount(Animation.INDEFINITE);
        keyRepeatTimeline.play();
    }

    /**
     * 停止按键重复
     */
    private void stopKeyRepeat() {
        if (keyRepeatTimeline != null) {
            keyRepeatTimeline.stop();
            keyRepeatTimeline = null;
        }
    }

    /**
     * 移除游戏按键监听
     */
    private void removeGameKeyListener() {
        stopKeyRepeat();
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
