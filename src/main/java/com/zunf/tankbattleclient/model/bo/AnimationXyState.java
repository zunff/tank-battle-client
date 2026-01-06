package com.zunf.tankbattleclient.model.bo;

import com.zunf.tankbattleclient.enums.Direction;

public class AnimationXyState {

    double currentX, currentY; // 当前显示位置（用于动画）
    double targetX, targetY; // 目标位置
    Direction direction;
    boolean isAnimating = false;


    public double getCurrentY() {
        return currentY;
    }

    public void setCurrentY(double currentY) {
        this.currentY = currentY;
    }

    public double getCurrentX() {
        return currentX;
    }

    public void setCurrentX(double currentX) {
        this.currentX = currentX;
    }

    public double getTargetY() {
        return targetY;
    }

    public void setTargetY(double targetY) {
        this.targetY = targetY;
    }

    public double getTargetX() {
        return targetX;
    }

    public void setTargetX(double targetX) {
        this.targetX = targetX;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public void setAnimating(boolean isAnimating) {
        this.isAnimating = isAnimating;
    }
}
