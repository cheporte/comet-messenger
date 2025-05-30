package com.comet.demo.core.server;

import com.comet.db.DatabaseManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final List<ClientHandler> clientHandlers;
    private final PrintWriter output;
    private final Scanner input;
    private String username;
    private final DatabaseManager dbManager;
    private String currentChatId;

    public ClientHandler(
            Socket clientSocket,
            List<ClientHandler> clientHandlers,
            String currentChatId
    ) throws IOException {
        this.clientSocket = clientSocket;
        this.clientHandlers = clientHandlers;
        this.output = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.input = new Scanner(this.clientSocket.getInputStream());
        this.dbManager = DatabaseManager.getInstance();
        this.currentChatId = currentChatId;
    }

    @Override
    public void run() {
        try {
            // Step 1: Authentication
            if (!authenticateUser()) {
                return;
            }

            // Step 2: User is authenticated, add to active clients
            synchronized (clientHandlers) {
                clientHandlers.add(this);
            }

            // Step 3: Send chat history
            sendChatHistory();

            // Step 4: Notify others of new user
            broadcastSystemMessage(username + " has joined the chat 🎉");

            // Step 5: Main message loop
            handleMessageLoop();

        } catch (Exception e) {
            System.err.println("ClientHandler error for " + username + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private boolean authenticateUser() {
        try {
            // Get credentials
            username = input.nextLine();
            String password = input.nextLine();

            // Validate credentials
            if (!dbManager.checkLogin(username, password)) {
                output.println("[Server] Authentication failed. Closing connection.");
                clientSocket.close();
                return false;
            }

            return true;
        } catch (IOException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return false;
    }

    private void sendChatHistory() {
        try {
            // Get chat history from database
            List<String> chatHistory = dbManager.getMessagesForChat(Integer.parseInt(currentChatId));

            // Send history line by line to avoid overwhelming client
            for (String message : chatHistory) {
                output.println(message);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid chat ID format: " + currentChatId);
        }
    }

    private void handleMessageLoop() {
        while (input.hasNextLine()) {
            String message = input.nextLine();

            // Handle special commands
            if (message.startsWith("/")) {
                handleCommand(message);
                continue;
            }

            // Broadcast regular message
            broadcastUserMessage(message);

            // Save to database
            saveMessageToDatabase(message);
        }
    }

    private void handleCommand(String command) {
        if (command.equals("/contacts")) {
            List<String> contacts = dbManager.getContacts(username);
            output.println("[Server] Your contacts:");
            for (String contact : contacts) {
                output.println("- " + contact);
            }
        }
        // Add more command handlers as needed
    }

    private void broadcastUserMessage(String message) {
        String formattedMessage = username + ": " + message;
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                if (handler.currentChatId.equals(this.currentChatId)) {
                    handler.output.println(formattedMessage);
                }
            }
        }
        System.out.println("Message in chat " + currentChatId + ": " + formattedMessage);
    }

    private void broadcastSystemMessage(String message) {
        String formattedMessage = "[Server] " + message;
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                if (handler.currentChatId.equals(this.currentChatId)) {
                    handler.output.println(formattedMessage);
                }
            }
        }
    }

    private void saveMessageToDatabase(String message) {
        try {
            dbManager.sendMessage(username, getCurrentChatRecipient(), message);
        } catch (Exception e) {
            System.err.println("Failed to save message to DB: " + e.getMessage());
        }
    }

    private String getCurrentChatRecipient() {
        // This needs proper implementation based on your chat logic
        // For now, returns the first contact (demo purposes)
        List<String> contacts = dbManager.getContacts(username);
        return contacts.isEmpty() ? null : contacts.get(0);
    }

    private void cleanup() {
        try {
            synchronized (clientHandlers) {
                clientHandlers.remove(this);
            }
            if (username != null) {
                broadcastSystemMessage(username + " has left the chat 💔");
            }
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }

    public String getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChatId(String chatId) {
        this.currentChatId = chatId;
    }
}