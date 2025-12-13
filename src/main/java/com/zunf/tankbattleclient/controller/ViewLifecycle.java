package com.zunf.tankbattleclient.controller;

import javafx.fxml.FXML;

public abstract class ViewLifecycle {

    /**
     * 当视图被显示时调用
     */
    public void onShow(){};
    /**
     * 当视图被隐藏时调用（切换）
     */
    public void onHide(){};

    /**
     * 当视图被关闭时调用
     */
    public void onClose() {
        onHide();
    }
}
