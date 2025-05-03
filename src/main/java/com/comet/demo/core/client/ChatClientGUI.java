package com.comet.demo.core.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ChatClientGUI extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Label label = new Label("Welcome to the Chat Client!");
        Scene scene = new Scene(label, 400, 300);

        primaryStage.setTitle("Comet Messenger");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
