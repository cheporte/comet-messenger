package com.comet.controller;

import com.comet.demo.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignupController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signupButton;

    @FXML
    private Button loginRedirectButton;

    @FXML
    private void initialize() {
        signupButton.setOnAction(this::handleSignup);
        loginRedirectButton.setOnAction(this::handleLoginRedirect);
    }

    private void handleSignup(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        // 🔥 TODO: Save user to DB
        System.out.println("Creating user " + username);
        // After signup:
        App.showLoginScreen();
    }

    private void handleLoginRedirect(ActionEvent event) {
        App.showLoginScreen();
    }
}
