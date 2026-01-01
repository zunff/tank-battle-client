package com.zunf.tankbattleclient.enums;

/**
 * 视图类型枚举
 * 定义所有视图的文件名、标题、宽度和高度
 */
public enum ViewEnum {
    LOGIN("login-view.fxml", "坦克大战 - 登录", 350, 400),
    REGISTER("register-view.fxml", "坦克大战 - 注册", 350, 400),
    LOBBY("lobby-view.fxml", "坦克大战 - 大厅", 800, 600),
    ROOM("room-view.fxml", "坦克大战 - 房间", 900, 600),
    GAME("game-view.fxml", "坦克大战 - 游戏", 800, 800);

    private final String fxmlFileName;
    private final String title;
    private final double width;
    private final double height;

    ViewEnum(String fxmlFileName, String title, double width, double height) {
        this.fxmlFileName = fxmlFileName;
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public String getFxmlFileName() {
        return fxmlFileName;
    }

    public String getTitle() {
        return title;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}

