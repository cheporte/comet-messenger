package com.comet.controller;

import com.comet.db.repository.UserRepository;
import com.comet.demo.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    UserRepository userRepository;

    @FXML TextField usernameField;
    @FXML PasswordField passwordField;
    @FXML Button loginButton;
    @FXML Button signupRedirectButton;

    @FXML void initialize() {
        userRepository = new UserRepository();

        signupRedirectButton.setOnAction(this::handleSignupRedirect);
        loginButton.setOnAction(this::handleLogin);
    }

    public static void showChatScreen(String username, String password) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/comet/main-view.fxml"));
            Parent root = loader.load();

            ChatController controller = loader.getController();
            controller.setUserCredentials(username, password);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add(App.class.getResource("/styles/main.css").toExternalForm());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username or password is empty");
            return;
        }

        boolean success = userRepository.checkLogin(username, password);
        if (success) {
            System.out.println("Login successful");
            showChatScreen(username, password);

            // Close the login window
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();
        } else {
            System.out.println("Login failed");
        }
    }

    private void handleSignupRedirect(ActionEvent event) {
        App.showSignupScreen();
    }
}