module com.comet.demo {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;

    opens com.comet.controller to javafx.fxml;

    exports com.comet.controller;
    exports com.comet.db;
    exports com.comet.demo.core.client;
    exports com.comet.demo.core.server;
    exports com.comet.demo;
}