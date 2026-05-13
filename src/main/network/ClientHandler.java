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

    // direction inputs. set by client, consumed by server tick
    //without volatile, may cache the values in CPU registers and the game loop could read stale directions
    public volatile int dx = 0;
    public volatile int dy = 0;

    // added other statuses so the server can read and write to them
    public boolean isIt = false;
    public boolean isInvulnerable = false;
    public boolean isFrozen = false;
    public boolean isImmune = false;
    public boolean isInvisible = false;
    public long lastTaggedTime = 0;
    
    public int speed = 5; 
    public int trapCharges = 0;

    public long speedEndTime = 0;
    public long freezeEndTime = 0;
    public long immuneEndTime = 0;
    public long invisibleEndTime = 0;

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
                x = 800;
                y = 50;
                break;
            case 2:
                x = 50;
                y = 500;
                break;
            case 3:
                x = 800;
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
        if (out != null) out.println(message);
    }

    @Override
    public void run() {
        try {
            String message;

            while ((message = in.readLine()) != null) {
                String[] parts = message.split(" ");

                if (parts[0].equals("INPUT") && parts.length == 3) {
                    dx = Integer.parseInt(parts[1]);
                    dy = Integer.parseInt(parts[2]);
                }else if (parts[0].equals("TRAP")) {
                    server.placeTrap(this);
                }
            }
        } catch (IOException e) {
            System.out.println("Player " + playerId + " disconnected.");
        } finally {
            // para walang ghost player if may ma disconnect
            // para di magcrash if nagtry magread sa stream ng disconnected player
            server.removeClient(this);
            try { 
                socket.close(); 
            } catch (IOException ignored) {}
        }
    }
}