package com.comet.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.logging.*;

public class App extends Application {

    private static Stage primaryStage;
    private static final Logger logger = Logger.getLogger(App.class.getName());

    @Override
    public void start(Stage stage) {
        try {
            logger.info("Starting the application...");

            primaryStage = stage;
            showLoginScreen();

            primaryStage.setTitle("Comet Messenger");
            primaryStage.show();

            logger.info("Application started successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start the application", e);
        }
    }

    public static void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/comet/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);
            scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/styles/auth.css")).toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to open the Login view", e);
        }
    }

    public static void showSignupScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/comet/signup-view.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);
            scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/styles/auth.css")).toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to open the Signup view", e);
        }
    }

    private static void configureLogger() {
        try {
            Logger rootLogger = Logger.getLogger("com.comet");

            // Configure the console handler
            Handler[] handlers = rootLogger.getHandlers();
            if (handlers.length > 0) {
                handlers[0].setFormatter(new SimpleFormatter() {

                    @Override
                    public synchronized String format(LogRecord lr) {
                        String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";
                        return String.format(format,
                                new Date(lr.getMillis()),
                                lr.getLevel().getLocalizedName(),
                                lr.getMessage());
                    }
                });
            }

            // Ensure logs directory exists
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                boolean dirCreated = logsDir.mkdir();
                if (!dirCreated) {
                    logger.severe("Failed to create logs directory.");
                    return;
                }
            }

            // Configure the file handler
            FileHandler fileHandler = getFileHandler();
            rootLogger.addHandler(fileHandler);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to configure logger file handler", e);
        }
    }

    private static FileHandler getFileHandler() throws IOException {
        FileHandler fileHandler = new FileHandler("logs/comet_messenger.log", true);
        fileHandler.setFormatter(new SimpleFormatter() {

            @Override
            public synchronized String format(LogRecord lr) {
                String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage());
            }
        });
        return fileHandler;
    }

        public static void main(String[] args) {
        configureLogger();
        launch(args);
    }
}
