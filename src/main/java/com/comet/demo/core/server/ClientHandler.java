package com.comet.demo.core.server;

import com.comet.db.DatabaseManager;
import com.comet.db.repository.UserRepository;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final Socket clientSocket;
    private final List<ClientHandler> clientHandlers;
    private final PrintWriter output;
    private final Scanner input;
    private String username;
    private final static List<String> messageHistory = new ArrayList<>();

    private final UserRepository userRepository;

    /**
     * Constructs a ClientHandler for the given client socket and list of handlers.
     * Initializes input/output streams and the user repository.
     *
     * @param clientSocket the socket for the connected client
     * @param clientHandlers the list of all connected client handlers
     * @throws IOException if an I/O error occurs or database connection fails
     */
    public ClientHandler(Socket clientSocket, List<ClientHandler> clientHandlers) throws IOException {
        try {
            this.clientSocket = clientSocket;
            this.clientHandlers = clientHandlers;
            this.output = new PrintWriter(this.clientSocket.getOutputStream(), true);
            this.input = new Scanner(this.clientSocket.getInputStream());
            this.userRepository = new UserRepository(DatabaseManager.getInstance().getConnection());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error initializing client handler.", e);
            throw e;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error initializing UserRepository.", e);
            throw new IOException("Database connection error", e);
        }
    }

    /**
     * Handles the client connection: authenticates, listens for messages, and manages disconnection.
     * Broadcasts messages to all connected clients.
     */
    @Override
    public void run() {
        try {
            // Step 1: Receive credentials from client (2 lines: username + password)
            String username = input.nextLine();
            String password = input.nextLine();

            // Step 2: Validate user in DB
            boolean loggedIn = userRepository.checkLogin(username, password);

            if (!loggedIn) {
                output.println("[Server] Authentication failed. Closing connection.");
                logger.warning("Authentication failed for user: " + username);
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

            logger.info("User connected: " + username);

            // Step 4: Listen for messages and broadcast them
            String message;
            while (input.hasNextLine() && (message = input.nextLine()) != null) {
                logger.info("Message from " + username + ": " + message);
                broadcastMessage(message, true);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling client.", e);
        } finally {
            try {
                synchronized (clientHandlers) {
                    if (!clientHandlers.contains(this)) {
                        clientHandlers.remove(this);
                    }
                }
                clientSocket.close();
                logger.info("User disconnected: " + username);
                broadcastMessage("[Server] " + username + " has left the chat 💔", false);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error closing client connection.", e);
            }
        }
    }

    /**
     * Broadcasts a message to all connected clients.
     * Optionally includes the sender in the broadcast.
     * Stores the message in the message history.
     *
     * @param message the message to broadcast
     * @param includeSelf true to include the sender, false to exclude
     */
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
