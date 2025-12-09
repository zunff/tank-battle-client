module com.zunf.tankbattleclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires com.almasb.fxgl.all;

    opens com.zunf.tankbattleclient to javafx.fxml;
    exports com.zunf.tankbattleclient;
}