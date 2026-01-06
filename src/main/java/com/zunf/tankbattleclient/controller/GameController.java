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
import java.util.Map;
import java.util.function.Consumer;

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
    private boolean adjustingAspectRatio = false; // 防止循环调整的标志
    private Consumer<com.google.protobuf.MessageLite> tickListener; // Tick消息监听器

    // 游戏状态
    private long matchId;
    private Map<Long, TankState> tanks = new HashMap<>(); // playerId -> TankState
    private Map<String, BulletState> bullets = new HashMap<>(); // bulletId -> BulletState
    private boolean isFirstTick = true;
    private AnimationTimer animationTimer;

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

            TankState state = tanks.computeIfAbsent(playerId, k -> new TankState());
            

            if (isFirstTick) {
                // 第一个 tick直接设置位置
                state.setCurrentX(x);
                state.setCurrentY(y);
                state.setTargetX(x);
                state.setTargetY(y);
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
        tanks.entrySet().removeIf(entry -> 
            tick.getTanksList().stream().noneMatch(t -> t.getPlayerId() == entry.getKey())
        );

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
            // 可以更新地图数据，这里暂时不处理
        }

        isFirstTick = false;
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
        final double ANIMATION_SPEED = 5.0; // 每帧移动的像素数

        // 更新坦克动画
        for (TankState state : tanks.values()) {
            if (state.isAnimating()) {
                handlerAnimationState(state, GameConstants.TANK_ANIMATION_SPEED);
            }
        }

        // 更新子弹动画
        for (BulletState state : bullets.values()) {
            if (state.isAnimating()) {
                handlerAnimationState(state, GameConstants.BULLET_ANIMATION_SPEED);
            }
        }
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

        for (TankState state : tanks.values()) {
            renderTankAtPosition(gc, state.getCurrentX(), state.getCurrentY(), state.getDirection());
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

                Color cellColor = getCellColor(MapIndex.of(cellType));
                gc.setFill(cellColor);
                gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                if (cellType != MapIndex.EMPTY.getCode()) {
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
    private void renderTankAtPosition(GraphicsContext gc, double x, double y, Direction direction) {
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

        // 绘制长方形坦克主体
        gc.setFill(Color.GREEN);
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

        gc.setFill(Color.DARKGREEN);
        gc.fillRect(barrelX, barrelY, barrelW, barrelH);

        // 绘制圆形
        gc.setFill(Color.DARKGREEN);
        gc.fillOval(x - circleRadius, y - circleRadius, circleRadius * 2, circleRadius * 2);

        // 绘制边框
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeRect(x - rectWidth / 2, y - rectHeight / 2, rectWidth, rectHeight);
        gc.strokeOval(x - circleRadius, y - circleRadius, circleRadius * 2, circleRadius * 2);
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
                    handleTankMove(code);
                    event.consume();
                } else if (code == KeyCode.SPACE) {
                    handleTankShoot();
                    event.consume();
                }
            };
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, gameKeyHandler);
    }

    /**
     * 移除游戏按键监听
     */
    private void removeGameKeyListener() {
        if (gameContainer != null && gameKeyHandler != null) {
            Scene scene = gameContainer.getScene();
            if (scene != null) {
                scene.removeEventFilter(KeyEvent.KEY_PRESSED, gameKeyHandler);
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
