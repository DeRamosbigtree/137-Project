package main.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GameClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private int playerId = -1;

    public GameClient() {
        try {
            socket = new Socket(HOST, PORT);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connected to server.");

            new Thread(this::listenToServer).start();

            send("Hello from client!");

        } catch (IOException e) {
            System.out.println("Could not connect to server.");
            e.printStackTrace();
        }
    }

    private void listenToServer() {
        try {
            String message;

            while ((message = in.readLine()) != null) {
                if (message.startsWith("ID")) {
                    String[] parts = message.split(" ");
                    playerId = Integer.parseInt(parts[1]);
                    System.out.println("Assigned Player ID: " + playerId);
                } else {
                    System.out.println(message);
                }
            }

        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }

    public void send(String message) {
        out.println(message);
    }

    public static void main(String[] args) {
        new GameClient();
    }
}