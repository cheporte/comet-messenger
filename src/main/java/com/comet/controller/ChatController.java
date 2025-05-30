package com.comet.controller;

import com.comet.demo.core.client.ChatClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatController {
    private ChatClient chatClient;
    private String username;
    private String password; // Store the password

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField messageField;

    public void setUserCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        initializeChatClient();
    }

    private void initializeChatClient() {
        if (username == null || username.isEmpty()) {
            username = "Anonymous :3"; // fallback if ever needed
        }

        // Initialize ChatClient with both username and password
        chatClient = new ChatClient("localhost", 12345, username, password, this::onMessageReceived);
        chatClient.start();
    }

    @FXML
    private void handleSend() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            chatArea.appendText("You: " + message + "\n");
            chatClient.sendMessage(message);
            messageField.clear();
        }
    }

    private void onMessageReceived(String message) {
        Platform.runLater(() -> chatArea.appendText(message + "\n"));
    }
}
