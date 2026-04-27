package main.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class GameClient {
    public Map<Integer, int[]> playerStates = new HashMap<>();

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    public static GameClient client;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public int playerId = -1;

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
            String msg;

            while ((msg = in.readLine()) != null) {
                String[] parts = msg.split(" ");

                if (parts[0].equals("ID")) {
                    playerId = Integer.parseInt(parts[1]);
                    System.out.println("My ID: " + playerId);
                }

                else if (parts[0].equals("STATE")) {

                    for (int i = 1; i < parts.length; i++) {
                        String[] data = parts[i].split(",");

                        int id = Integer.parseInt(data[0]);
                        int x = Integer.parseInt(data[1]);
                        int y = Integer.parseInt(data[2]);

                        playerStates.put(id, new int[]{x, y});
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }

    public void send(String message) {
        out.println(message);
    }

    public void sendMove(int dx, int dy) {
    if (out != null) {
        out.println("MOVE " + dx + " " + dy);
    }
}

    public static void main(String[] args) {
        new GameClient();
    }
}