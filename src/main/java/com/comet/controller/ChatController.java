package com.comet.controller;

import com.comet.db.DatabaseManager;
import com.comet.demo.core.client.ChatClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ChatController {
    private WebSocketClient webSocketClient;

    private ChatClient chatClient;
    private String username;
    private String password; // Store the password

    @FXML private ListView<String> contactListView;
    @FXML private ListView<String> chatListView;
    @FXML private ImageView userImageView;
    @FXML private Label userDisplayName;

    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private Label currentChatLabel;

    private int currentUserId;
    private int currentChatId;
    private DatabaseManager databaseManager;

    public void setUserCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        this.databaseManager = DatabaseManager.getInstance(); // Ensure DatabaseManager is initialized
        this.currentUserId = getUserId(username, password);
        initializeChatClient();

        // Set initial UI state to reflect no chat is selected
        currentChatLabel.setText("No chat selected");
        chatArea.clear();

        loadChats();
        loadContacts();
        loadUserProfile(currentUserId);
    }

    private void connectWebSocket() {
        try {
            // Replace with your WebSocket server URL
            URI serverUri = new URI("ws://localhost:8887");
            webSocketClient = new WebSocketClient(serverUri) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("Connected to WebSocket server");
                }

                @Override
                public void onMessage(String message) {
                    // Handle incoming WebSocket messages
                    System.out.println("Received WebSocket message: " + message);
                    Platform.runLater(() -> {
                        // Refresh chats or handle specific messages
                        if (message.equals("refresh_chats")) {
                            loadChats();
                            if (currentChatId != -1) {
                                loadMessages(currentChatId);
                            }
                        }
                    });
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Disconnected from WebSocket server");
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    private void initializeChatClient() {
        if (username == null || username.isEmpty()) {
            username = "Anonymous :3"; // fallback if ever needed
        }

        try {
            connectWebSocket();
            chatClient = new ChatClient("localhost", 12345, username, password, this::onMessageReceived);
            chatClient.start();
        } catch (Exception e) {
            Platform.runLater(() -> chatArea.appendText("Failed to connect to the chat server: " + e.getMessage() + "\n"));
            e.printStackTrace();
        }
    }

    private int getUserId(String username, String password) {
        String query = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 or handle error appropriately
    }

    private void loadContacts() {
        List<String> contacts = getContactsForUser(currentUserId);
        ObservableList<String> observableContacts = FXCollections.observableArrayList(contacts);
        contactListView.setItems(observableContacts);
    }

    private List<String> getContactsForUser(int userId) {
        List<String> contacts = new ArrayList<>();
        String query = "SELECT u.display_name FROM contacts c JOIN users u ON c.contact_id = u.id WHERE c.user_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                contacts.add(rs.getString("display_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    private void loadUserProfile(int userId) {
        String query = "SELECT display_name, image_url FROM users WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userDisplayName.setText(rs.getString("display_name"));
                String imageUrl = rs.getString("image_url");
                if (imageUrl != null) {
                    Image image = new Image(imageUrl);
                    userImageView.setImage(image);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadChats() {
        List<String> chats = getChatsForUser(currentUserId);
        ObservableList<String> observableChats = FXCollections.observableArrayList(chats);
        chatListView.setItems(observableChats);

        // Clear the selection to avoid selecting the first chat by default
        chatListView.getSelectionModel().clearSelection();

        // Set up the listener for chat selection
        chatListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentChatLabel.setText(newValue);
                currentChatId = getChatId(newValue);
                loadMessages(currentChatId);
            } else {
                // Clear the chat area if no chat is selected
                currentChatLabel.setText("No chat selected");
                chatArea.clear();
                currentChatId = -1; // Reset currentChatId to indicate no chat is selected
            }
        });
    }


    @FXML
    private void handleAddChat() {
        String newChatName = "New Chat " + (chatListView.getItems().size() + 1); // Simple naming for new chats
        try {
            int newChatId = databaseManager.createChat(newChatName, false);
            databaseManager.addUserToChat(newChatId, currentUserId);
            loadChats(); // Refresh the list of chats
            // Do not automatically select the new chat, let the user select it
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private List<String> getChatsForUser(int userId) {
        List<String> chats = new ArrayList<>();
        String query = "SELECT c.name FROM chats c JOIN chat_members cm ON c.id = cm.chat_id WHERE cm.user_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                chats.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chats;
    }

    private int getChatId(String chatName) {
        String query = "SELECT id FROM chats WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, chatName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 or handle error appropriately
    }

    private void loadMessages(int chatId) {
        chatArea.clear();
        String query = "SELECT u.username, m.content FROM messages m JOIN users u ON m.sender_id = u.id WHERE chat_id = ? ORDER BY timestamp";
        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            stmt.setInt(1, chatId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                chatArea.appendText(rs.getString("username") + ": " + rs.getString("content") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddUser() {
        if (currentChatId == -1) {
            System.err.println("No chat selected.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add User to Chat");
        dialog.setHeaderText("Enter the username of the user to add to the chat:");
        dialog.setContentText("Username:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            try {
                int userId = getUserIdByUsername(username);
                if (userId != -1) {
                    databaseManager.addUserToChat(currentChatId, userId);
                    System.out.println("User added to chat successfully.");
                } else {
                    System.err.println("User not found.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleAddContact() {
        if (currentChatId == -1) {
            System.err.println("No chat selected.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Contact");
        dialog.setHeaderText("Enter the username of the user to add your contacts:");
        dialog.setContentText("Username:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            try {
                int userId = getUserIdByUsername(username);
                if (userId != -1) {
                    databaseManager.addContact(currentUserId, userId);
                    System.out.println("Contact saved successfully.");
                } else {
                    System.err.println("User not found.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private int getUserIdByUsername(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if user not found
    }

    @FXML
    private void handleSend() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && currentChatId != -1) {
            sendMessage(currentChatId, currentUserId, message);
            chatClient.sendMessage(message);
            messageField.clear();
        }
    }

    private void sendMessage(int chatId, int senderId, String content) {
        String insert = "INSERT INTO messages (chat_id, sender_id, content) VALUES (?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, chatId);
            stmt.setInt(2, senderId);
            stmt.setString(3, content);
            stmt.executeUpdate();

            // Append the message directly to the chatArea
            Platform.runLater(() -> {
                chatArea.setScrollTop(Double.MAX_VALUE);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void onMessageReceived(String message) {
        Platform.runLater(() -> {
            chatArea.appendText(message + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
