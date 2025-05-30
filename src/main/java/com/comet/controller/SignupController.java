package com.comet.controller;

import com.comet.db.DatabaseManager;
import com.comet.demo.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Please fill out all the fields");
            return;
        }

        boolean success = DatabaseManager.getInstance().createUser(username, password);
        if (success) {
            System.out.println("User created successfully");
            LoginController.showChatScreen(username, password);

            // Close the signup window
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();
        } else {
            System.out.println("User creation failed");
        }
    }

    private void handleLoginRedirect(ActionEvent event) {
        App.showLoginScreen();
    }
}
