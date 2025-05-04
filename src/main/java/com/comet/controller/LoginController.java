package com.comet.controller;

import com.comet.db.DatabaseManager;
import com.comet.demo.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button signupRedirectButton;

    @FXML
    private void initialize() {
        signupRedirectButton.setOnAction(this::handleSignupRedirect);
        loginButton.setOnAction(this::handleLogin);
    }

    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username or password is empty");
            return;
        }

        boolean success = DatabaseManager.getInstance().checkLogin(username, password);
        if (success) {
            System.out.println("Login successful");
            App.showChatScreen();
        } else {
            System.out.println("Login failed");
        }
    }

    private void handleSignupRedirect(ActionEvent event) {
        App.showSignupScreen();
    }
}
