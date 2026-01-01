package com.zunf.tankbattleclient.controller;

import com.zunf.tankbattleclient.TankBattleApplication;
import com.zunf.tankbattleclient.enums.ViewEnum;
import com.zunf.tankbattleclient.manager.ViewManager;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

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

    // 地图元素编码
    private static final byte EMPTY = 0; // 空地
    private static final byte WALL = 1; // 不可破坏墙
    private static final byte BRICK = 2; // 可破坏砖块
    private static final byte SPAWN = 3; // 出生点

    // 坦克方向枚举
    private enum TankDirection {
        UP, // 上
        DOWN, // 下
        LEFT, // 左
        RIGHT // 右
    }

    @Override
    public void onShow(Object data) {
        if (data instanceof GameRoomProto.StartNotice) {
            this.gameData = (GameRoomProto.StartNotice) data;
            // 加载CSS样式
            loadStylesheet();
            // 绑定Canvas大小到容器
            bindCanvasSize();
            renderMap();
            // 初始化ESC键监听
            initEscKeyListener();
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
        gameData = null;
    }

    /**
     * 绑定Canvas大小到容器
     */
    private void bindCanvasSize() {
        if (gameContainer == null || gameCanvas == null) {
            return;
        }

        // 绑定Canvas大小到容器
        gameCanvas.widthProperty().bind(gameContainer.widthProperty());
        gameCanvas.heightProperty().bind(gameContainer.heightProperty());

        // 监听大小变化，重新渲染
        canvasSizeListener = (obs, oldVal, newVal) -> renderMap();
        gameContainer.widthProperty().addListener(canvasSizeListener);
        gameContainer.heightProperty().addListener(canvasSizeListener);
    }

    /**
     * 渲染地图
     */
    private void renderMap() {
        if (gameData == null || gameData.getMapDataCount() == 0 || gameCanvas == null) {
            return;
        }

        double canvasWidth = gameCanvas.getWidth();
        double canvasHeight = gameCanvas.getHeight();

        // 如果Canvas还没有初始化大小，等待下次渲染
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            return;
        }

        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvasWidth, canvasHeight);

        // 获取地图尺寸
        int mapHeight = gameData.getMapDataCount();
        if (mapHeight == 0) {
            return;
        }

        // 获取第一行的宽度作为地图宽度
        int mapWidth = gameData.getMapData(0).size();
        if (mapWidth == 0) {
            return;
        }

        // 计算每个格子的尺寸
        double cellWidth = canvasWidth / mapWidth;
        double cellHeight = canvasHeight / mapHeight;

        // 渲染每个格子
        for (int row = 0; row < mapHeight; row++) {
            com.google.protobuf.ByteString rowData = gameData.getMapData(row);
            byte[] rowBytes = rowData.toByteArray();

            for (int col = 0; col < rowBytes.length && col < mapWidth; col++) {
                byte cellType = rowBytes[col];
                double x = col * cellWidth;
                double y = row * cellHeight;

                // 根据单元格类型选择颜色
                Color cellColor = getCellColor(cellType);

                // 绘制单元格
                gc.setFill(cellColor);
                gc.fillRect(x, y, cellWidth, cellHeight);

                // 绘制边框（只在非空地上绘制，避免视觉混乱）
                if (cellType != EMPTY) {
                    gc.setStroke(Color.GRAY);
                    gc.setLineWidth(0.5);
                    gc.strokeRect(x, y, cellWidth, cellHeight);
                }
            }
        }

        // 绘制出生点的坦克
        renderTank(gc, cellWidth, cellHeight);
    }

    /**
     * 根据单元格类型获取颜色
     */
    private Color getCellColor(byte cellType) {
        return switch (cellType) {
            case EMPTY -> Color.LIGHTGRAY; // 空地 - 浅灰色
            case WALL -> Color.DARKGRAY; // 不可破坏墙 - 深灰色
            case BRICK -> Color.SADDLEBROWN; // 可破坏砖块 - 棕色
            case SPAWN -> Color.LIGHTGREEN; // 出生点 - 浅绿色
            default -> Color.WHITE; // 未知类型 - 白色
        };
    }

    /**
     * 在出生点位置绘制绿色坦克
     */
    private void renderTank(GraphicsContext gc, double cellWidth, double cellHeight) {
        if (gameData == null) {
            return;
        }

        // 获取出生点字节数组 [x, y]
        com.google.protobuf.ByteString spawnPointBytes = gameData.getSpawnPoint();

        // 检查是否有出生点数据
        if (spawnPointBytes == null || spawnPointBytes.isEmpty()) {
            return;
        }

        byte[] spawnPoint = spawnPointBytes.toByteArray();

        // 检查字节数组长度，至少需要2个字节
        if (spawnPoint.length < 2) {
            return;
        }

        // 解析坐标（将byte转换为无符号整数）
        int x = Byte.toUnsignedInt(spawnPoint[0]);
        int y = Byte.toUnsignedInt(spawnPoint[1]);

        // 根据坐标生成固定的随机方向（确保同一位置的方向不变）
        TankDirection direction = getTankDirection(x, y);

        // 计算坦克在Canvas上的位置（居中在格子中）
        double tankX = x * cellWidth + cellWidth / 2;
        double tankY = y * cellHeight + cellHeight / 2;

        // 坦克大小（占格子的80%）
        double baseSize = Math.min(cellWidth, cellHeight) * 0.8;

        // 长方形坦克：长边和短边（增加短边宽度，让坦克更胖）
        double longSide = baseSize * 1.2; // 长边
        double shortSide = baseSize * 0.85; // 短边（增加宽度，让坦克更胖）

        // 根据方向确定长边和短边的方向
        // 炮管从短边延伸，炮管方向就是坦克方向：
        // - UP/DOWN方向：炮管从上下延伸，长方形纵向（宽=短边，高=长边），短边在左右
        // - LEFT/RIGHT方向：炮管从左右延伸，长方形横向（宽=长边，高=短边），短边在上下
        double rectWidth, rectHeight;
        if (direction == TankDirection.UP || direction == TankDirection.DOWN) {
            // 上下方向：炮管从上下延伸，长方形纵向（宽=短边，高=长边）
            rectWidth = shortSide; // 短边在左右
            rectHeight = longSide; // 长边在上下
        } else {
            // 左右方向：炮管从左右延伸，长方形横向（宽=长边，高=短边）
            rectWidth = longSide; // 长边在左右
            rectHeight = shortSide; // 短边在上下
        }

        // 计算圆形半径
        double circleRadius = Math.min(rectWidth, rectHeight) * 0.4;

        // 绘制长方形坦克主体（绿色）- 先绘制底层
        gc.setFill(Color.GREEN);
        gc.fillRect(tankX - rectWidth / 2, tankY - rectHeight / 2, rectWidth, rectHeight);

        // 绘制炮管（矩形，根据方向）
        // 炮管从圆形边缘延伸，与圆形相接
        double barrelWidth = shortSide * 0.35; // 炮管宽度（基于短边）
        double barrelLength = longSide * 0.5; // 炮管长度

        double barrelX, barrelY, barrelW, barrelH;
        switch (direction) {
            case UP:
                // 向上：炮管从圆形上方边缘延伸
                barrelW = barrelWidth;
                barrelH = barrelLength;
                barrelX = tankX - barrelW / 2;
                barrelY = tankY - circleRadius - barrelH; // 从圆形边缘开始
                break;
            case DOWN:
                // 向下：炮管从圆形下方边缘延伸
                barrelW = barrelWidth;
                barrelH = barrelLength;
                barrelX = tankX - barrelW / 2;
                barrelY = tankY + circleRadius; // 从圆形边缘开始
                break;
            case LEFT:
                // 向左：炮管从圆形左侧边缘延伸
                barrelW = barrelLength;
                barrelH = barrelWidth;
                barrelX = tankX - circleRadius - barrelW; // 从圆形边缘开始
                barrelY = tankY - barrelH / 2;
                break;
            case RIGHT:
                // 向右：炮管从圆形右侧边缘延伸
                barrelW = barrelLength;
                barrelH = barrelWidth;
                barrelX = tankX + circleRadius; // 从圆形边缘开始
                barrelY = tankY - barrelH / 2;
                break;
            default:
                return;
        }

        // 绘制炮管（在长方形之后，圆形之前）
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(barrelX, barrelY, barrelW, barrelH);

        // 绘制中间的圆形（深绿色）- 最后绘制，确保在最上层
        gc.setFill(Color.DARKGREEN);
        gc.fillOval(tankX - circleRadius, tankY - circleRadius, circleRadius * 2, circleRadius * 2);

        // 绘制坦克边框
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeRect(tankX - rectWidth / 2, tankY - rectHeight / 2, rectWidth, rectHeight);
        gc.strokeOval(tankX - circleRadius, tankY - circleRadius, circleRadius * 2, circleRadius * 2);
    }

    /**
     * 根据坐标生成固定的随机方向
     */
    private TankDirection getTankDirection(int x, int y) {
        // 使用坐标作为随机种子，确保同一位置的方向固定
        int seed = x * 31 + y;
        TankDirection[] directions = TankDirection.values();
        return directions[Math.abs(seed) % directions.length];
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
}
