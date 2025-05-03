package com.comet.controller;

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
        String username = usernameField.getText();
        String password = passwordField.getText();
        // 🔥 TODO: Verify login from DB
        System.out.println("Trying login with " + username);
        // If login successful:
        App.showChatScreen();
    }

    private void handleSignupRedirect(ActionEvent event) {
        App.showSignupScreen();
    }
}
