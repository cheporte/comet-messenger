package com.comet.controller;

import org.controlsfx.control.Notifications;

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
    // private int currentChatId;

    private UserRepository userRepository;
    private ChatRepository chatRepository;
    private ContactRepository contactRepository;

    // Track chat type for current selection
    private enum ChatType { NONE, PRIVATE, GROUP }
    private ChatType currentChatType = ChatType.NONE;
    private int currentPrivateChatId = -1;
    private int currentGroupChatId = -1;

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
                            // If a group chat is open, reload its messages
                            if (currentChatType == ChatType.GROUP && currentGroupChatId != -1) {
                                loadGroupMessages(currentGroupChatId);
                            }
                            // If a private chat is open, reload its messages
                            if (currentChatType == ChatType.PRIVATE && currentPrivateChatId != -1) {
                                loadPrivateMessages(currentPrivateChatId);
                            }
                        } else if (message.equals("refresh_contacts")) {
                            loadContacts();
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
        // Load group chats only
        List<String> groupChats = new ArrayList<>();
        try {
            groupChats = chatRepository.getGroupChatsForUser(currentUserId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ObservableList<String> observableChats = FXCollections.observableArrayList(groupChats);
        chatListView.setItems(observableChats);
        chatListView.getSelectionModel().clearSelection();
        // Listener for group chat selection
        chatListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentChatLabel.setText(newValue);
                try {
                    int groupId = chatRepository.getGroupChatIdByName(newValue);
                    currentGroupChatId = groupId;
                    currentPrivateChatId = -1;
                    currentChatType = ChatType.GROUP;
                    loadGroupMessages(currentGroupChatId);
                    // Deselect contact list to allow switching back to contacts
                    contactListView.getSelectionModel().clearSelection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                currentChatLabel.setText("No chat selected");
                chatArea.clear();
                currentGroupChatId = -1;
                currentChatType = ChatType.NONE;
            }
        });

        // Listener for private chat selection
        contactListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentChatLabel.setText("Chat with " + newValue);
                int contactId = userRepository.getUserIdByDisplayName(newValue);
                if (contactId != -1) {
                    try {
                        int privateChatId = chatRepository.getPrivateChatId(currentUserId, contactId);
                        if (privateChatId == -1) {
                            privateChatId = chatRepository.createPrivateChat(currentUserId, contactId);
                        }
                        currentPrivateChatId = privateChatId;
                        currentGroupChatId = -1;
                        currentChatType = ChatType.PRIVATE;
                        loadPrivateMessages(currentPrivateChatId);
                        // Deselect group chat to allow switching back to groups
                        chatListView.getSelectionModel().clearSelection();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                currentChatLabel.setText("No contact selected");
                chatArea.clear();
                currentPrivateChatId = -1;
                currentChatType = ChatType.NONE;
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
        // Listener for private chat selection
        contactListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentChatLabel.setText("Chat with " + newValue);
                int contactId = userRepository.getUserIdByDisplayName(newValue);
                if (contactId != -1) {
                    try {
                        int privateChatId = chatRepository.getPrivateChatId(currentUserId, contactId);
                        if (privateChatId == -1) {
                            privateChatId = chatRepository.createPrivateChat(currentUserId, contactId);
                        }
                        currentPrivateChatId = privateChatId;
                        currentGroupChatId = -1;
                        currentChatType = ChatType.PRIVATE;
                        loadPrivateMessages(currentPrivateChatId);
                        // Deselect group chat to allow switching back to groups
                        chatListView.getSelectionModel().clearSelection();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                currentChatLabel.setText("No contact selected");
                chatArea.clear();
                currentPrivateChatId = -1;
                currentChatType = ChatType.NONE;
            }
        });
    }

    private void loadPrivateMessages(int privateChatId) {
        chatArea.clear();
        try {
            List<String> messages = chatRepository.getPrivateMessages(privateChatId);
            for (String msg : messages) {
                chatArea.appendText(msg + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadGroupMessages(int groupChatId) {
        chatArea.clear();
        try {
            List<String> messages = chatRepository.getGroupMessages(groupChatId);
            for (String msg : messages) {
                chatArea.appendText(msg + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the creation of a new chat and adds the current user to it.
     */
    @FXML
    private void handleAddChat() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Group Chat");
        dialog.setHeaderText("Enter a name for the new group chat:");
        dialog.setContentText("Group Name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(groupName -> {
            try {
                int groupId = chatRepository.createGroupChat(groupName, currentUserId);
                chatRepository.addUserToGroup(groupId, currentUserId);
                webSocketClient.send("refresh_chats");
                loadChats();
                chatListView.getSelectionModel().select(groupName);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Handles adding a user to the currently selected chat.
     */
    @FXML
    private void handleAddUser() {
        if (currentChatType != ChatType.GROUP || currentGroupChatId == -1) {
            System.err.println("No group chat selected.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add User to Group Chat");
        dialog.setHeaderText("Enter the username of the user to add:");
        dialog.setContentText("Username:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            try {
                int userId = userRepository.getUserIdByUsername(username);
                if (userId != -1) {
                    chatRepository.addUserToGroup(currentGroupChatId, userId);
                    System.out.println("User added to group chat successfully.");
                    // Notify all clients to refresh chats
                    if (webSocketClient != null && webSocketClient.isOpen()) {
                        webSocketClient.send("refresh_chats");
                    }
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
        if (!message.isEmpty()) {
            try {
                if (currentChatType == ChatType.PRIVATE && currentPrivateChatId != -1) {
                    chatRepository.sendPrivateMessage(currentPrivateChatId, currentUserId, message);
                    chatClient.sendMessage(message);
                } else if (currentChatType == ChatType.GROUP && currentGroupChatId != -1) {
                    chatRepository.sendGroupMessage(currentGroupChatId, currentUserId, message);
                    loadGroupMessages(currentGroupChatId);
                } else {
                    System.err.println("No chat selected.");
                    return;
                }
                messageField.clear();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
     * Handles adding a contact for the current user.
     */
    @FXML
    private void handleAddContact() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Contact");
        dialog.setHeaderText("Enter the username of the user to add as a contact:");
        dialog.setContentText("Username:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            try {
                int userId = userRepository.getUserIdByUsername(username);
                if (userId != -1) {
                    contactRepository.addContact(currentUserId, userId);
                    loadContacts();
                    System.out.println("Contact added successfully.");
                    // Notify all clients to refresh contacts
                    if (webSocketClient != null && webSocketClient.isOpen()) {
                        webSocketClient.send("refresh_contacts");
                    }
                } else {
                    System.err.println("User not found.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Handles removing a contact for the current user.
     */
    @FXML
    private void handleRemoveContact() {
        String selectedContact = contactListView.getSelectionModel().getSelectedItem();
        if (selectedContact == null) {
            System.err.println("No contact selected.");
            return;
        }
        int contactId = userRepository.getUserIdByDisplayName(selectedContact);
        if (contactId == -1) {
            System.err.println("Contact not found.");
            return;
        }
        try {
            contactRepository.removeContact(currentUserId, contactId);
            loadContacts();
            System.out.println("Contact removed successfully.");
            // Notify all clients to refresh contacts
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.send("refresh_contacts");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

            String sender = message.contains(":") ? message.substring(0, message.indexOf(":")) : "";
            String msg = message.contains(":") ? message.substring(message.indexOf(":") + 1).trim() : message;

            // Only show notification for incoming messages (not sent by current user)
            if (!sender.equals(username)) {
                // Fetch sender's image_url from the database
                String imageUrl = null;
                try {
                    int senderId = userRepository.getUserIdByDisplayName(sender);
                    if (senderId != -1) {
                        imageUrl = userRepository.getUserImageUrl(senderId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ImageView icon = null;
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    try {
                        Image img = new Image(imageUrl, 32, 32, true, true);
                        icon = new ImageView(img);
                    } catch (Exception e) {
                        System.err.println("Failed to load image for notification icon: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                Notifications notification = Notifications.create()
                    .title("New message")
                    .text(sender + ": " + msg)
                    .hideAfter(javafx.util.Duration.seconds(4))
                    .position(javafx.geometry.Pos.TOP_RIGHT)
                    .darkStyle();
                if (icon != null) {
                    notification.graphic(icon);
                }
                notification.showInformation();
            }
        });
    }
}
