package com.comet.controller;

import com.comet.demo.core.client.ChatClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatController {
    private ChatClient chatClient;
    @FXML
    private TextArea chatArea;

    @FXML
    private TextField messageField;

    public void initialize() {
        chatClient = new ChatClient("localhost", 12345, "Default User", this::onMessageReceived);
        chatClient.start();
    }

    private void onMessageReceived(String message) {
        Platform.runLater(() -> chatArea.appendText(message + "\n"));
    }

    @FXML
    private void handleSend() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            chatArea.appendText("You: " + message + "\n");
            messageField.clear();
            chatClient.sendMessage(messageField.getText());
        }
    }
}
