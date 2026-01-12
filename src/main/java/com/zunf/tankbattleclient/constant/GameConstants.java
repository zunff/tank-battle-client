package com.zunf.tankbattleclient.constant;

import javafx.scene.paint.Color;

public interface GameConstants {

    // ========== 地图常量 ==========
    /**
     * 地图大小 32x32
     */
    int MAP_SIZE = 32;

    /**
     * 每个格子固定25px
     */
    int CELL_SIZE = 25;

    /**
     * Canvas大小 800x800
     */
    int CANVAS_SIZE = MAP_SIZE * CELL_SIZE;

    Color EMPTY_COLOR = Color.rgb(220, 220, 220);        // 空地：浅灰（接近经典游戏的背景色）
    Color WALL_COLOR = Color.rgb(105, 105, 105);         // 铁墙（不可破坏）：暗灰色（金属质感）
    Color BRICK_COLOR = Color.rgb(138, 54, 15);          // 砖墙（可破坏）：砖棕色（经典土砖色）
    Color SPAWN_COLOR = Color.rgb(144, 238, 144);       // 出生点：淡绿色（柔和不刺眼）
    Color DESTROYED_WALL_COLOR = EMPTY_COLOR;            // 被摧毁的墙：同空地色
    Color UNKNOWN_COLOR = Color.WHITE;                   // 未知类型：白色（便于调试）

    // ========== 动画速度 ==========
    /**
     * 坦克动画移动速度 px/帧
     */
    double TANK_ANIMATION_SPEED = 1;

    /**
     * 子弹动画移动速度 px/帧
     */
    double BULLET_ANIMATION_SPEED = 3;

    // ========== 动画持续时间 ==========
    /**
     * 受击动画持续时间（毫秒）
     */
    long HIT_ANIMATION_DURATION = 300;

    /**
     * 血条显示持续时间（毫秒）
     */
    long HEALTH_BAR_DISPLAY_DURATION = 500;

    /**
     * 砖块摧毁动画持续时间（毫秒）
     */
    long BRICK_DESTROY_ANIMATION_DURATION = 500;

    // ========== 渲染常量 ==========
    /**
     * 坦克基础大小比例（相对于CELL_SIZE）
     */
    double TANK_BASE_SIZE_RATIO = 0.8;

    /**
     * 坦克长边比例（相对于baseSize）
     */
    double TANK_LONG_SIDE_RATIO = 1.2;

    /**
     * 坦克短边比例（相对于baseSize）
     */
    double TANK_SHORT_SIDE_RATIO = 0.85;

    /**
     * 坦克圆形半径比例（相对于矩形最小边）
     */
    double TANK_CIRCLE_RADIUS_RATIO = 0.4;

    /**
     * 炮管宽度比例（相对于shortSide）
     */
    double TANK_BARREL_WIDTH_RATIO = 0.35;

    /**
     * 炮管长度比例（相对于longSide）
     */
    double TANK_BARREL_LENGTH_RATIO = 0.5;

    /**
     * 子弹大小比例（相对于CELL_SIZE）
     */
    double BULLET_SIZE_RATIO = 0.3;

    /**
     * 血条宽度比例（相对于CELL_SIZE）
     */
    double HEALTH_BAR_WIDTH_RATIO = 0.8;

    /**
     * 血条高度（像素）
     */
    double HEALTH_BAR_HEIGHT = 4;

    /**
     * 血条距离坦克上方的偏移（像素）
     */
    double HEALTH_BAR_OFFSET_Y = 8;

    // ========== 按键重复 ==========
    /**
     * 按键重复间隔（毫秒）
     */
    long KEY_REPEAT_INTERVAL = 50;

    // ========== 动画插值 ==========
    /**
     * 血条动画插值速度
     */
    double HEALTH_BAR_INTERPOLATION_SPEED = 0.1;

    /**
     * 血条动画差值阈值
     */
    double HEALTH_BAR_DIFF_THRESHOLD = 0.1;
}
