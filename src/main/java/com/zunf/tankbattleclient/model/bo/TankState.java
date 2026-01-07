package com.zunf.tankbattleclient.model.bo;

public class TankState extends AnimationXyState {
    private int life = 100; // 当前血量
    private int maxLife = 100; // 最大血量
    private boolean isHit = false; // 是否正在受击动画
    private long hitAnimationStartTime = 0; // 受击动画开始时间
    private double previousLife = 100; // 上一帧的血量（用于动画）
    private boolean showHealthBar = false; // 是否显示血条
    private long healthBarShowStartTime = 0; // 血条显示开始时间

    public int getLife() {
        return life;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public int getMaxLife() {
        return maxLife;
    }

    public void setMaxLife(int maxLife) {
        this.maxLife = maxLife;
    }

    public boolean isHit() {
        return isHit;
    }

    public void setHit(boolean hit) {
        this.isHit = hit;
    }

    public long getHitAnimationStartTime() {
        return hitAnimationStartTime;
    }

    public void setHitAnimationStartTime(long hitAnimationStartTime) {
        this.hitAnimationStartTime = hitAnimationStartTime;
    }

    public double getPreviousLife() {
        return previousLife;
    }

    public void setPreviousLife(double previousLife) {
        this.previousLife = previousLife;
    }

    public boolean isShowHealthBar() {
        return showHealthBar;
    }

    public void setShowHealthBar(boolean showHealthBar) {
        this.showHealthBar = showHealthBar;
    }

    public long getHealthBarShowStartTime() {
        return healthBarShowStartTime;
    }

    public void setHealthBarShowStartTime(long healthBarShowStartTime) {
        this.healthBarShowStartTime = healthBarShowStartTime;
    }
}