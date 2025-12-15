module com.zunf.tankbattleclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires com.almasb.fxgl.all;
    requires java.sql; // 让 java.sql.SQLException 可用
    requires cn.hutool;
    requires com.google.protobuf;

    opens com.zunf.tankbattleclient.ui to javafx.fxml;
    opens com.zunf.tankbattleclient to javafx.fxml;
    opens com.zunf.tankbattleclient.controller to javafx.fxml;
    opens com.zunf.tankbattleclient.model.qo to cn.hutool;

    exports com.zunf.tankbattleclient;
    exports com.zunf.tankbattleclient.controller;
}