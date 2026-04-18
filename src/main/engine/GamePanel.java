package main.engine;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import javax.swing.JPanel;
import main.input.KeyHandler;
import main.model.Player;

public class GamePanel extends JPanel {

    private final ArrayList<Player> players = new ArrayList<>();
    private final KeyHandler keyH = new KeyHandler();

    private Player mainPlayer;

    private final GameTimer timer = new GameTimer(70);

    public GamePanel() {
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(keyH);

        initializePlayers();
    }

    private void initializePlayers() {
        mainPlayer = new Player(0, 100, 100, false);
        players.add(mainPlayer);

        players.add(new Player(1, 500, 100, true));
        players.add(new Player(2, 100, 400, true));
        players.add(new Player(3, 500, 400, true));

        // Start with player 0 as It
        players.get(0).isIt = true;
    }

    public void updateGame() {

        timer.update();

        if (timer.isGameOver()) {
            return;
        }

        updateMainPlayer();
        updateBots();
        handleTagging();
    }

    private void updateMainPlayer() {
        int dx = 0;
        int dy = 0;

        if (keyH.up) dy--;
        if (keyH.down) dy++;
        if (keyH.left) dx--;
        if (keyH.right) dx++;

        mainPlayer.move(dx, dy);
        mainPlayer.clampToBounds(getWidth(), getHeight());
    }

    private void updateBots() {
        for (Player p : players) {
            if (p.bot) {
                p.updateBotMovement(getWidth(), getHeight());
            }
        }
    }

    private void handleTagging() {
        Player currentIt = getCurrentItPlayer();
        if (currentIt == null) return;

        for (Player p : players) {
            if (p == currentIt) continue;

            if (currentIt.getBounds().intersects(p.getBounds())) {
                currentIt.isIt = false;
                p.isIt = true;
                break;
            }
        }
    }

    private Player getCurrentItPlayer() {
        for (Player p : players) {
            if (p.isIt) {
                return p;
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawPlayers(g);
        drawUI(g);
    }

    private void drawPlayers(Graphics g) {
        for (Player p : players) {
            if (p.isIt) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.BLUE);
            }

            g.fillRect(p.x, p.y, p.size, p.size);

            g.setColor(Color.WHITE);
            g.drawString("P" + p.id, p.x + 8, p.y + 18);
        }
    }

    private void drawUI(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("WASD to move", 10, 20);

        Player itPlayer = getCurrentItPlayer();
        if (itPlayer != null) {
            g.drawString("Current It: Player " + itPlayer.id, 10, 45);
        }

        g.drawString("Week 2: Bots + Collision + Tagging", 10, 70);

        timer.draw(g);
    }
}