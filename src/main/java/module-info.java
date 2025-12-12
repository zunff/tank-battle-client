module com.zunf.tankbattleclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires com.almasb.fxgl.all;
    requires cn.hutool;
    requires com.google.protobuf;

    opens com.zunf.tankbattleclient to javafx.fxml;
    exports com.zunf.tankbattleclient;
    exports com.zunf.tankbattleclient.controller;
    opens com.zunf.tankbattleclient.controller to javafx.fxml;
}