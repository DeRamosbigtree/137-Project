package main.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private int playerId;
    private GameServer server;

    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket, int playerId, GameServer server) {
        this.socket = socket;
        this.playerId = playerId;
        this.server = server;

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
                System.out.println("Player " + playerId + ": " + message);

                server.broadcast("PLAYER " + playerId + ": " + message);
            }

        } catch (IOException e) {
            System.out.println("Player " + playerId + " disconnected.");
        }
    }
}