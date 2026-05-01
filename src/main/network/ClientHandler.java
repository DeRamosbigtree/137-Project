package main.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    // made these final 
    private final Socket socket;
    public final int playerId;
    private final GameServer server;

    private BufferedReader in;
    private PrintWriter out;

    public int x;
    public int y;

    public ClientHandler(Socket socket, int playerId, GameServer server) {
        this.socket = socket;
        this.playerId = playerId;
        this.server = server;

        // Assign spawn positions
        switch (playerId) {
            case 0:
                x = 50;
                y = 50;
                break;
            case 1:
                x = 700;
                y = 50;
                break;
            case 2:
                x = 50;
                y = 500;
                break;
            case 3:
                x = 700;
                y = 500;
                break;
        }

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            send("ID " + playerId);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            String message;

            while ((message = in.readLine()) != null) {
                String[] parts = message.split(" ");

                if (parts[0].equals("MOVE")) {
                    int dx = Integer.parseInt(parts[1]);
                    int dy = Integer.parseInt(parts[2]);

                    x += dx * 5;
                    y += dy * 5;

                    if (x < 0) x = 0;
                    if (y < 0) y = 0;
                    if (x > 800 - 30) x = 800 - 30;
                    if (y > 600 - 30) y = 600 - 30;

                    server.broadcastAllPlayers();
                }
            }

        } catch (IOException e) {
            System.out.println("Player " + playerId + " disconnected.");
        }
    }
}