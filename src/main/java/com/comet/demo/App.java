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

//        primaryStage.setFullScreen(true);
        primaryStage.setTitle("Comet Messenger");
        primaryStage.show();
    }

    public static void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/comet/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);
            scene.getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showSignupScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/comet/signup-view.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);
            scene.getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showChatScreen() {
        try {
            FXMLLoader loader = new  FXMLLoader(App.class.getResource("/com/comet/main-view.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);
            scene.getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}