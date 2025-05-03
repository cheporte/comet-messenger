package com.comet.demo.core.server;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final List<PrintWriter> clientWriters;

    public ClientHandler(Socket clientSocket, List<PrintWriter> clientWriters) {
        this.clientSocket = clientSocket;
        this.clientWriters = clientWriters;
    }

    private void broadcastMessage(String message) {
        synchronized (clientWriters) {
            for (PrintWriter pw : clientWriters) {
                pw.println(message);
            }
        }
    }

    @Override
    public void run() {
        try {
            Scanner input = new Scanner(clientSocket.getInputStream());
            PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);

            String username = input.nextLine();
            System.out.println(username + " has joined!");

            while (input.hasNextLine()) {
                String message = input.nextLine();
                System.out.println("Received from " + username + ": " + message);
                broadcastMessage(username + ": " + message);
            }
        } catch (Exception e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (Exception ignored) {}
        }
    }
}
