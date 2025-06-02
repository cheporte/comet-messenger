package com.comet.controller;

import com.comet.db.DatabaseManager;
import com.comet.db.repository.ChatRepository;
import com.comet.db.repository.ContactRepository;
import com.comet.db.repository.UserRepository;
import com.comet.demo.core.client.ChatClient;
import com.comet.demo.core.client.ProfileDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Pair;
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

    private UserRepository userRepository;
    private ChatRepository chatRepository;
    private ContactRepository contactRepository;

    /**
     * Sets the user credentials and initializes repositories and UI state for the chat controller.
     *
     * @param username the username of the user
     * @param password the password of the user
     */
    public void setUserCredentials(String username, String password) {
        this.username = username;
        this.password = password;

        this.userRepository = new UserRepository();
        this.chatRepository = new ChatRepository();
        this.contactRepository = new ContactRepository();

        this.currentUserId = this.userRepository.getUserId(username, password);
        initializeChatClient();

        // Set initial UI state to reflect no chat is selected
        currentChatLabel.setText("No chat selected");
        chatArea.clear();

        loadChats();
        loadContacts();
        loadUserProfile(currentUserId);
    }

    /**
     * Connects to the WebSocket server for real-time chat updates.
     */
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

    /**
     * Closes the WebSocket connection if it is open.
     */
    public void close() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    /**
     * Initializes the ChatClient and connects to the WebSocket server.
     */
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

    /**
     * Loads the user's profile information and updates the UI.
     *
     * @param userId the ID of the user whose profile is to be loaded
     */
    private void loadUserProfile(int userId) {
        String query = "SELECT display_name, image_url FROM users WHERE id = ?";
        try (
            Connection connection = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
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

    /**
     * Loads the list of chats for the current user and sets up the chat selection listener.
     */
    private void loadChats() {
        List<String> chats = chatRepository.getChatsForUser(currentUserId);
        ObservableList<String> observableChats = FXCollections.observableArrayList(chats);
        chatListView.setItems(observableChats);

        // Clear the selection to avoid selecting the first chat by default
        chatListView.getSelectionModel().clearSelection();

        // Set up the listener for chat selection
        chatListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentChatLabel.setText(newValue);
                currentChatId = chatRepository.getChatId(newValue);
                loadMessages(currentChatId);
            } else {
                // Clear the chat area if no chat is selected
                currentChatLabel.setText("No chat selected");
                chatArea.clear();
                currentChatId = -1; // Reset currentChatId to indicate no chat is selected
            }
        });
    }

    /**
     * Loads the list of contacts for the current user and sets up the contact selection listener.
     */
    private void loadContacts() {
        List<String> contacts = contactRepository.getContactNamesForUser(currentUserId);
        ObservableList<String> observableContacts = FXCollections.observableArrayList(contacts);
        contactListView.setItems(observableContacts);

        // Set up the listener for contact selection
        contactListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentChatLabel.setText("Chat with " + newValue);
                int contactId = userRepository.getUserIdByDisplayName(newValue);
                if (contactId != -1) {
                    currentChatId = chatRepository.getPrivateChatId(currentUserId, contactId);
                    loadMessages(currentChatId);
                }
            } else {
                // Clear the chat area if no contact is selected
                currentChatLabel.setText("No contact selected");
                chatArea.clear();
                currentChatId = -1; // Reset currentChatId to indicate no chat is selected
            }
        });
    }

    /**
     * Handles the creation of a new chat and adds the current user to it.
     */
    @FXML
    private void handleAddChat() {
        String newChatName = "New Chat " + (chatListView.getItems().size() + 1); // Simple naming for new chats
        try {
            int newChatId = chatRepository.createChat(newChatName, false);
            chatRepository.addUserToChat(newChatId, currentUserId);
            loadChats(); // Refresh the list of chats
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to create new chat: " + e.getMessage());
        }
    }

    /**
     * Loads the messages for the specified chat and displays them in the chat area.
     *
     * @param chatId the ID of the chat whose messages are to be loaded
     */
    private void loadMessages(int chatId) {
        chatArea.clear();
        String query = "SELECT u.username, m.content FROM messages m JOIN users u ON m.sender_id = u.id WHERE chat_id = ? ORDER BY timestamp";
        try (
            Connection connection = DatabaseManager.getInstance().getConnection();
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

    /**
     * Handles adding a user to the currently selected chat.
     */
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
                int userId = userRepository.getUserIdByUsername(username);
                if (userId != -1) {
                    chatRepository.addUserToChat(currentChatId, userId);
                    System.out.println("User added to chat successfully.");
                } else {
                    System.err.println("User not found.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Handles adding a contact for the current user.
     */
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
                int userId = userRepository.getUserIdByUsername(username);
                if (userId != -1) {
                    contactRepository.addContact(currentUserId, userId);
                    System.out.println("Contact saved successfully.");
                } else {
                    System.err.println("User not found.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Handles sending a message in the currently selected chat.
     */
    @FXML
    private void handleSend() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && currentChatId != -1) {
            sendMessage(currentChatId, currentUserId, message);
            chatClient.sendMessage(message);
            messageField.clear();
        }
    }

    /**
     * Sends a message to the database for the specified chat and sender.
     *
     * @param chatId the ID of the chat
     * @param senderId the ID of the sender
     * @param content the content of the message
     */
    private void sendMessage(int chatId, int senderId, String content) {
        String insert = "INSERT INTO messages (chat_id, sender_id, content) VALUES (?, ?, ?)";
        try (
            Connection connection = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = connection.prepareStatement(insert)
        ) {
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

    /**
     * Handles updating the user's profile information.
     */
    @FXML
    private void handleUpdateProfile() {
        // Fetch current profile information
        String currentDisplayName = userDisplayName.getText();
        String currentImageUrl = ""; // You can fetch the current image URL from your data model

        ProfileDialog dialog = new ProfileDialog(currentDisplayName, currentImageUrl);
        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(pair -> {
            String newDisplayName = pair.getKey();
            String newImageUrl = pair.getValue();

            // Update the profile in the database
            userRepository.updateUserProfile(currentUserId, newDisplayName, newImageUrl);

            // Update the UI
            userDisplayName.setText(newDisplayName);
            if (newImageUrl != null && !newImageUrl.isEmpty()) {
                Image image = new Image(newImageUrl);
                userImageView.setImage(image);
            }
        });
    }

    /**
     * Callback for when a message is received from the chat client.
     *
     * @param message the message received
     */
    private void onMessageReceived(String message) {
        Platform.runLater(() -> {
            chatArea.appendText(message + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
