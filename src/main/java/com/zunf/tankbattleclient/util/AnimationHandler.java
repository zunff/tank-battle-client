package com.zunf.tankbattleclient.util;

import com.zunf.tankbattleclient.constant.GameConstants;
import com.zunf.tankbattleclient.model.bo.AnimationXyState;
import com.zunf.tankbattleclient.model.bo.BulletState;
import com.zunf.tankbattleclient.model.bo.TankState;

import java.util.Collection;
import java.util.Map;

/**
 * 动画处理工具类
 * 负责处理游戏对象的动画逻辑
 */
public class AnimationHandler {

    /**
     * 处理动画状态更新（移动动画）
     */
    public static void handleAnimationState(AnimationXyState state, double speed) {
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
     * 更新所有坦克动画
     */
    public static void updateTankAnimations(Collection<TankState> tanks) {
        for (TankState state : tanks) {
            // 更新移动动画
            if (state.isAnimating()) {
                handleAnimationState(state, GameConstants.TANK_ANIMATION_SPEED);
            }

            // 更新受击动画
            if (state.isHit()) {
                long elapsed = System.currentTimeMillis() - state.getHitAnimationStartTime();
                if (elapsed > GameConstants.HIT_ANIMATION_DURATION) {
                    state.setHit(false);
                }
            }

            // 更新血条显示状态
            if (state.isShowHealthBar()) {
                long elapsed = System.currentTimeMillis() - state.getHealthBarShowStartTime();
                if (elapsed > GameConstants.HEALTH_BAR_DISPLAY_DURATION) {
                    state.setShowHealthBar(false);
                }
            }

            // 更新血条动画（平滑变化）
            updateHealthBarAnimation(state);
        }
    }

    /**
     * 更新血条动画（平滑变化）
     */
    private static void updateHealthBarAnimation(TankState state) {
        double currentDisplayLife = state.getPreviousLife();
        double targetLife = state.getLife();
        double diff = targetLife - currentDisplayLife;

        if (Math.abs(diff) > GameConstants.HEALTH_BAR_DIFF_THRESHOLD) {
            // 平滑变化（增加或减少）
            state.setPreviousLife(currentDisplayLife + diff * GameConstants.HEALTH_BAR_INTERPOLATION_SPEED);
        } else {
            // 接近目标值，直接设置
            state.setPreviousLife(targetLife);
        }
    }

    /**
     * 更新所有子弹动画
     */
    public static void updateBulletAnimations(Collection<BulletState> bullets) {
        for (BulletState state : bullets) {
            if (state.isAnimating()) {
                handleAnimationState(state, GameConstants.BULLET_ANIMATION_SPEED);
            }
        }
    }

    /**
     * 清理过期的砖块摧毁动画
     */
    public static void cleanupBrickAnimations(Map<String, Long> destroyedBrickAnimations) {
        long currentTime = System.currentTimeMillis();
        destroyedBrickAnimations.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > GameConstants.BRICK_DESTROY_ANIMATION_DURATION
        );
    }
}

