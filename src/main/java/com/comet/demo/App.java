package com.comet.demo;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showLoginScreen();
        primaryStage.setTitle("Comet Messenger");
        primaryStage.show();
    }

    public static void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/comet/login-view.fxml"));
            primaryStage.setScene(new Scene(loader.load(), 400, 300));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showSignupScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/comet/signup-view.fxml"));
            primaryStage.setScene(new Scene(loader.load(), 400, 400));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showChatScreen() {
        try {
            FXMLLoader loader = new  FXMLLoader(App.class.getResource("/com/comet/main-view.fxml"));
            primaryStage.setScene(new Scene(loader.load(), 400, 400));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}