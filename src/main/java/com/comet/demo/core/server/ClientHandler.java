package com.comet.demo.core.server;

import com.comet.db.DatabaseManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final List<ClientHandler> clientHandlers;
    private final PrintWriter output;
    private final Scanner input;
    private String username;

    private final static List<String> messageHistory = new ArrayList<>();

    public ClientHandler(Socket clientSocket, List<ClientHandler> clientHandlers) throws IOException {
        this.clientSocket = clientSocket;
        this.clientHandlers = clientHandlers;
        this.output = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.input = new Scanner(this.clientSocket.getInputStream());
    }

    @Override
    public void run() {
        try {
            // Step 1: Receive credentials from client (2 lines: username + password)
            String username = input.nextLine();
            String password = input.nextLine();

            // Step 2: Validate user in DB
            boolean loggedIn = DatabaseManager.getInstance().checkLogin(username, password);

            if (!loggedIn) {
                output.println("[Server] ❌ Authentication failed. Closing connection.");
                clientSocket.close();
                return; // stop here for bad login
            }

            // Step 3: User is legit, add handler & announce
            this.username = username;

            synchronized (clientHandlers) {
                if (!clientHandlers.contains(this)) {
                    clientHandlers.add(this);
                }
            }

            synchronized (messageHistory) {
                for (String message : messageHistory) {
                    output.println(message);
                }
            }

            broadcastMessage("[Server] " + username + " has joined the chat 🎉", false);

            // Step 4: Listen for messages and broadcast them
            String message;
            while (input.hasNextLine() && (message = input.nextLine()) != null) {
                broadcastMessage(username + ": " + message, true);
            }
        } catch (Exception e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            try {
                synchronized (clientHandlers) {
                    if (!clientHandlers.contains(this)) {
                        clientHandlers.remove(this);
                    }
                }
                clientSocket.close();
                broadcastMessage("[Server] " + username + " has left the chat 💔", false);
            } catch (Exception ignored) {
            }
        }
    }

    private void broadcastMessage(String message, boolean includeSelf) {
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                if (!includeSelf && handler == this) continue;
                handler.output.println(message);
            }
        }
        synchronized (messageHistory) {
            messageHistory.add(message); // Store the message in history
        }
    }
}
