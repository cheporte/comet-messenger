package com.comet.controller;

import com.comet.db.DatabaseManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.util.List;

public class ChatController {

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField messageField;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> contactsList;

    @FXML
    private Label chatTitle;

    private ObservableList<String> contacts = FXCollections.observableArrayList();
    private String username;
    private String password;
    private String selectedContact;

    @FXML
    private void initialize() {
        contactsList.setItems(contacts);
        contactsList.setOnMouseClicked(this::handleContactSelection);
        loadContacts();
    }

    public void setUserCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    private void loadContacts() {
        List<String> userContacts = DatabaseManager.getInstance().getContacts(username);
        contacts.setAll(userContacts);
    }

    @FXML
    private void handleSearch() {
        String searchQuery = searchField.getText().trim();
        if (!searchQuery.isEmpty()) {
            List<String> searchResults = DatabaseManager.getInstance().searchUsers(searchQuery);
            contacts.setAll(searchResults);
        } else {
            loadContacts(); // Reload all contacts if search is empty
        }
    }

    @FXML
    private void handleAddContact() {
        String selectedUser = contactsList.getSelectionModel().getSelectedItem();
        if (selectedUser != null && !selectedUser.equals(username)) {
            boolean success = DatabaseManager.getInstance().addContact(username, selectedUser);
            if (success) {
                loadContacts(); // Refresh the contacts list
            } else {
                showAlert("Error", "Failed to add contact.");
            }
        } else {
            showAlert("Error", "No user selected or cannot add yourself.");
        }
    }

    @FXML
    private void handleRemoveContact() {
        String selectedUser = contactsList.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            boolean success = DatabaseManager.getInstance().removeContact(username, selectedUser);
            if (success) {
                loadContacts(); // Refresh the contacts list
            } else {
                showAlert("Error", "Failed to remove contact.");
            }
        } else {
            showAlert("Error", "No user selected.");
        }
    }

    private void handleContactSelection(MouseEvent event) {
        selectedContact = contactsList.getSelectionModel().getSelectedItem();
        if (selectedContact != null) {
            chatTitle.setText("Chat with " + selectedContact);
            loadChatHistory(selectedContact);
        }
    }

    private void loadChatHistory(String contact) {
        List<String> chatHistory = DatabaseManager.getInstance().getChatHistory(username, contact);
        Platform.runLater(() -> {
            chatArea.clear();
            for (String message : chatHistory) {
                chatArea.appendText(message + "\n");
            }
        });
    }

    @FXML
    private void handleSend() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && selectedContact != null) {
            boolean success = DatabaseManager.getInstance().sendMessage(username, selectedContact, message);
            if (success) {
                chatArea.appendText("You: " + message + "\n");
                messageField.clear();
            } else {
                showAlert("Error", "Failed to send message.");
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
