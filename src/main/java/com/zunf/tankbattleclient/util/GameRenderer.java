package com.zunf.tankbattleclient.util;

import com.google.protobuf.ByteString;
import com.zunf.tankbattleclient.constant.GameConstants;
import com.zunf.tankbattleclient.enums.Direction;
import com.zunf.tankbattleclient.enums.MapIndex;
import com.zunf.tankbattleclient.model.bo.TankState;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * 游戏渲染工具类
 * 负责所有游戏元素的渲染逻辑
 */
public class GameRenderer {

    /**
     * 根据单元格类型获取颜色
     */
    public static Color getCellColor(MapIndex cellType) {
        return switch (cellType) {
            case EMPTY -> GameConstants.EMPTY_COLOR;
            case WALL -> GameConstants.WALL_COLOR;
            case BRICK -> GameConstants.BRICK_COLOR;
            case SPAWN -> GameConstants.SPAWN_COLOR;
            case DESTROYED_WALL -> GameConstants.DESTROYED_WALL_COLOR;
            default -> GameConstants.UNKNOWN_COLOR;
        };
    }

    /**
     * 渲染地图背景
     */
    public static void renderMapBackground(GraphicsContext gc, GameRoomProto.StartNotice gameData,
                                           Map<String, Long> destroyedBrickAnimations) {
        if (gameData == null || gameData.getMapDataCount() == 0) {
            return;
        }

        int mapHeight = gameData.getMapDataCount();
        int mapWidth = gameData.getMapData(0).size();

        for (int row = 0; row < mapHeight && row < GameConstants.MAP_SIZE; row++) {
            ByteString rowData = gameData.getMapData(row);
            byte[] rowBytes = rowData.toByteArray();

            for (int col = 0; col < rowBytes.length && col < mapWidth && col < GameConstants.MAP_SIZE; col++) {
                byte cellType = rowBytes[col];
                double x = col * GameConstants.CELL_SIZE;
                double y = row * GameConstants.CELL_SIZE;

                String key = row + "_" + col;
                boolean isDestroying = destroyedBrickAnimations.containsKey(key);

                // 如果正在播放摧毁动画，显示动画效果
                if (isDestroying) {
                    long elapsed = System.currentTimeMillis() - destroyedBrickAnimations.get(key);
                    double progress = Math.min(elapsed / (double) GameConstants.BRICK_DESTROY_ANIMATION_DURATION, 1.0);

                    // 摧毁动画：闪烁和缩放效果
                    double alpha = 1.0 - progress;
                    double scale = 1.0 - progress * 0.5; // 缩小到50%

                    // 绘制原始砖块（带透明度）
                    Color brickColor = getCellColor(MapIndex.BRICK);
                    gc.setGlobalAlpha(alpha);
                    gc.setFill(brickColor);
                    double offsetX = (GameConstants.CELL_SIZE - GameConstants.CELL_SIZE * scale) / 2;
                    double offsetY = (GameConstants.CELL_SIZE - GameConstants.CELL_SIZE * scale) / 2;
                    gc.fillRect(x + offsetX, y + offsetY, GameConstants.CELL_SIZE * scale, GameConstants.CELL_SIZE * scale);
                    gc.setGlobalAlpha(1.0);

                    // 如果动画完成，显示为空地
                    if (progress >= 1.0) {
                        cellType = MapIndex.EMPTY.getCode();
                    }
                }

                Color cellColor = getCellColor(MapIndex.of(cellType));
                gc.setFill(cellColor);
                gc.fillRect(x, y, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);

                // 只对不可破坏墙和可破坏墙绘制边框，已摧毁墙、空地和正在摧毁的砖块不绘制边框
                byte finalCellType = cellType;
                if (!isDestroying
                        && finalCellType != MapIndex.DESTROYED_WALL.getCode()
                        && finalCellType != MapIndex.EMPTY.getCode()
                        && (finalCellType == MapIndex.WALL.getCode() || finalCellType == MapIndex.BRICK.getCode())) {
                    gc.setStroke(Color.GRAY);
                    gc.setLineWidth(0.5);
                    gc.strokeRect(x, y, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
                }
            }
        }
    }

    /**
     * 在指定位置渲染坦克
     */
    public static void renderTank(GraphicsContext gc, double x, double y, Direction direction, TankState state,
                                  boolean isMyTank) {
        // 受击动画效果
        boolean isHit = state != null && state.isHit();
        if (isHit) {
            // 受击时闪烁效果
            long elapsed = System.currentTimeMillis() - state.getHitAnimationStartTime();
            double alpha = 0.5 + 0.5 * Math.sin(elapsed / 50.0); // 闪烁效果
            gc.setGlobalAlpha(alpha);
        }

        // 计算坦克尺寸
        double baseSize = GameConstants.CELL_SIZE * GameConstants.TANK_BASE_SIZE_RATIO;
        double longSide = baseSize * GameConstants.TANK_LONG_SIDE_RATIO;
        double shortSide = baseSize * GameConstants.TANK_SHORT_SIDE_RATIO;

        double rectWidth, rectHeight;
        if (direction == Direction.UP || direction == Direction.DOWN) {
            rectWidth = shortSide;
            rectHeight = longSide;
        } else {
            rectWidth = longSide;
            rectHeight = shortSide;
        }

        double circleRadius = Math.min(rectWidth, rectHeight) * GameConstants.TANK_CIRCLE_RADIUS_RATIO;

        // 根据是否自己的坦克和受击状态选择颜色
        TankColors colors = getTankColors(isMyTank, isHit);

        // 绘制长方形坦克主体
        gc.setFill(colors.bodyColor);
        gc.fillRect(x - rectWidth / 2, y - rectHeight / 2, rectWidth, rectHeight);

        // 绘制炮管
        renderTankBarrel(gc, x, y, direction, shortSide, longSide, circleRadius, colors.barrelColor);

        // 绘制圆形
        gc.setFill(colors.circleColor);
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
     * 渲染坦克炮管
     */
    private static void renderTankBarrel(GraphicsContext gc, double x, double y, Direction direction,
                                         double shortSide, double longSide, double circleRadius, Color barrelColor) {
        double barrelWidth = shortSide * GameConstants.TANK_BARREL_WIDTH_RATIO;
        double barrelLength = longSide * GameConstants.TANK_BARREL_LENGTH_RATIO;

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

        gc.setFill(barrelColor);
        gc.fillRect(barrelX, barrelY, barrelW, barrelH);
    }

    /**
     * 获取坦克颜色
     */
    private static TankColors getTankColors(boolean isMyTank, boolean isHit) {
        if (isMyTank) {
            if (isHit) {
                return new TankColors(Color.ORANGE, Color.ORANGE, Color.ORANGE);
            } else {
                return new TankColors(Color.GREEN, Color.DARKGREEN, Color.DARKGREEN);
            }
        } else {
            if (isHit) {
                return new TankColors(Color.CORAL, Color.CORAL, Color.CORAL);
            } else {
                return new TankColors(Color.DARKRED, Color.MAROON, Color.MAROON);
            }
        }
    }

    /**
     * 坦克颜色数据类
     */
    private static class TankColors {
        final Color bodyColor;
        final Color circleColor;
        final Color barrelColor;

        TankColors(Color bodyColor, Color circleColor, Color barrelColor) {
            this.bodyColor = bodyColor;
            this.circleColor = circleColor;
            this.barrelColor = barrelColor;
        }
    }

    /**
     * 渲染坦克血条
     */
    public static void renderHealthBar(GraphicsContext gc, double tankX, double tankY, double tankHeight, TankState state) {
        // 只在满足显示条件时显示血条
        if (!state.isShowHealthBar()) {
            return;
        }

        double healthBarWidth = GameConstants.CELL_SIZE * GameConstants.HEALTH_BAR_WIDTH_RATIO;
        double healthBarHeight = GameConstants.HEALTH_BAR_HEIGHT;
        double healthBarX = tankX - healthBarWidth / 2;
        double healthBarY = tankY - tankHeight / 2 - GameConstants.HEALTH_BAR_OFFSET_Y;

        // 血条背景（灰色）
        gc.setFill(Color.GRAY);
        gc.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);

        // 当前血量（平滑动画）
        double currentLife = state.getPreviousLife();
        double maxLife = state.getMaxLife();
        double healthRatio = Math.max(0, Math.min(1, currentLife / maxLife));
        double healthBarFillWidth = healthBarWidth * healthRatio;

        // 根据血量比例选择颜色
        Color healthColor = getHealthColor(healthRatio);

        // 绘制血量条
        gc.setFill(healthColor);
        gc.fillRect(healthBarX, healthBarY, healthBarFillWidth, healthBarHeight);

        // 血条边框
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);
        gc.strokeRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
    }

    /**
     * 根据血量比例获取颜色
     */
    private static Color getHealthColor(double healthRatio) {
        if (healthRatio > 0.6) {
            return Color.GREEN;
        } else if (healthRatio > 0.3) {
            return Color.ORANGE;
        } else {
            return Color.RED;
        }
    }

    /**
     * 在指定位置渲染子弹
     */
    public static void renderBullet(GraphicsContext gc, double x, double y, Direction direction) {
        double bulletSize = GameConstants.CELL_SIZE * GameConstants.BULLET_SIZE_RATIO;
        gc.setFill(Color.YELLOW);
        gc.fillOval(x - bulletSize / 2, y - bulletSize / 2, bulletSize, bulletSize);
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(1.0);
        gc.strokeOval(x - bulletSize / 2, y - bulletSize / 2, bulletSize, bulletSize);
    }
}

