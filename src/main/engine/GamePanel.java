package main.engine;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.JPanel;
import main.input.KeyHandler;
import main.model.Player;
import main.model.PowerUp;
import main.model.Trap;
import main.network.GameClient;

public class GamePanel extends JPanel {

    private final ArrayList<Player> players = new ArrayList<>();
    private final KeyHandler keyH = new KeyHandler();

    private Player mainPlayer;
    private GameClient client;

    private final GameTimer timer = new GameTimer(70);

    private ScoreManager scoreManager;
    
    private final ArrayList<PowerUp> powerUps = new ArrayList<>();
    private final ArrayList<Trap> traps = new ArrayList<>();
    private final Random rand = new Random();

    private long lastSpawn = 0;
    private final long spawnDelay = 3000; // 7 seconds
    
    

    public GamePanel(String host) {
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(keyH);

        try {
            client = new GameClient(host);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // initializePlayers();
        for (int i = 0; i < 4; i++) {
            players.add(new Player(i, 100, 100, false));
        }

        if (mainPlayer == null && client.playerId >= 0 && client.playerId < players.size()) {
            mainPlayer = players.get(client.playerId);
        }

        this.scoreManager = new ScoreManager(players);
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
        if (mainPlayer == null && client.playerId >= 0 && client.playerId < players.size()) {
            mainPlayer = players.get(client.playerId);
        }

        // Only send inputs while game is active
        if (client != null && "PLAYING".equals(client.phase)) {
            updateMainPlayer();
        }

        // updateBots();
        // updatePowerUps();
        // updateEffects();
        // updateTraps(players);

        if (client != null) {
            // Sync positions and state flags from server
            for (Integer id : client.playerStates.keySet()) {
                if (id >= players.size()) continue;
                int[] s = client.playerStates.get(id);
                Player p = players.get(id);
                p.x              = s[0];
                p.y              = s[1];
                p.isIt           = s[2] == 1;
                p.isInvulnerable = s[3] == 1;
                p.isFrozen       = s[4] == 1;
                p.isImmune       = s[5] == 1;
                p.isInvisible    = s[6] == 1;
            }
            // Sync scores from server
            for (Player p : players) {
                if (p.id < client.scores.length) {
                    p.timeAsIt = client.scores[p.id];
                }
            }
        }
    }

    private void updateMainPlayer() {
        if (mainPlayer == null) return;

        int dx = 0;
        int dy = 0;

        if (keyH.up) dy--;
        if (keyH.down) dy++;
        if (keyH.left) dx--;
        if (keyH.right) dx++;

        if (client != null) {
            client.sendInput(dx, dy);
        }

        if (keyH.space) placeTrap(mainPlayer);
    }

    private void updateBots() {
        for (Player p : players) {
            if (p.bot && !p.isFrozen) {
                p.updateBotMovement(getWidth(), getHeight());
            }
        }
    }

    
    private void handleTagging() {
        Player currentIt = getCurrentItPlayer();
        if (currentIt == null) return;

        for (Player p : players) {
            if (p == currentIt) continue;

            // ONLY handle logic if they collide
            if (!currentIt.getBounds().intersects(p.getBounds())) continue;

            // If target is in cooldown
            if (p.isInvulnerable) continue;

            // If target has shield (immune)
            if (p.isImmune) {
                p.isImmune = false; // consume shield
                return; // no tag happens
            }

            // Normal tag
            currentIt.isIt = false;

            // former It gets cooldown
            currentIt.isInvulnerable = true;
            currentIt.lastTaggedTime = System.currentTimeMillis();

            // new It
            p.isIt = true;

            return; // ensure ONLY ONE tag happens
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

        String phase = (client != null) ? client.phase : "LOBBY";

        drawPlayers(g);

        switch (phase) {
            case "LOBBY":
                drawLobbyOverlay(g);
                break;
            case "COUNTDOWN":
                drawUI(g);
                drawCountdownOverlay(g);
                break;
            case "PLAYING":
                drawUI(g);
                drawPowerUps(g);
                break;
            case "GAME_OVER":
                drawUI(g);
                drawPowerUps(g);
                scoreManager.drawWinner(g, getWidth(), getHeight(), client.winnerId);
                break;
        }
    }

    private void drawPlayers(Graphics g) {
        for (Player p : players) {
            if (p.isIt) {
                g.setColor(Color.RED);
            } else {
            	if(p.isImmune) {
            		g.setColor(Color.GREEN);
            	}else {
            	    g.setColor(Color.BLUE);
            	}
                
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

        if (client != null) {
            timer.setTimeLeft(client.timeLeft);
        }
        timer.draw(g);

        scoreManager.drawScores(g, getWidth(), getHeight());
    }

    private void drawLobbyOverlay(Graphics g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        String text = "Waiting for players...";
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (getWidth() - w) / 2, getHeight() / 2);
    }

    private void drawCountdownOverlay(Graphics g) {
        int n = (client != null) ? client.countdownValue : 0;
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 96));
        String big = String.valueOf(n);
        int bw = g.getFontMetrics().stringWidth(big);
        g.drawString(big, (getWidth() - bw) / 2, getHeight() / 2 + 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        String sub = "Game starts in " + n + "...";
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, (getWidth() - sw) / 2, getHeight() / 2 + 75);
    }
    
    private void drawPowerUps(Graphics g) {
        for (PowerUp p : powerUps) {
            switch (p.type) {
                case SPEED: g.setColor(Color.YELLOW); break;
                case FREEZE: g.setColor(Color.CYAN); break;
                case SHIELD: g.setColor(Color.GREEN); break;
                case GHOST: g.setColor(Color.WHITE); break;
            }
            g.fillOval(p.x, p.y, p.size, p.size);
        }
        
        // draw traps
        g.setColor(Color.MAGENTA);
        for (Trap t : traps) {
            g.fillRect(t.x, t.y, t.size, t.size);
        }
    }
    
    // Power-up
    // Track active effects per player
    public static class Effect {
        PowerUp.Type type;
        long endTime;

        Effect(PowerUp.Type type, long duration) {
            this.type = type;
            this.endTime = System.currentTimeMillis() + duration;
        }
    }

    private final java.util.Map<Player, java.util.List<Effect>> activeEffects = new java.util.HashMap<>();
    
    private void applyEffect(Player player, PowerUp.Type type) {

        activeEffects.putIfAbsent(player, new ArrayList<>());

        switch (type) {
            case SPEED:
                player.speed = 6 ; // boosted speed by 50%
                activeEffects.get(player).add(new Effect(type, 3000));
                break;

            case FREEZE:
                player.trapCharges += 1;
                break;
            case SHIELD:
            	player.isImmune = true;
            	activeEffects.get(player).add(new Effect(type, 10000));
            	break;
            case GHOST:
            	player.isInvisible = true;
            	activeEffects.get(player).add(new Effect(type, 4000));
            	break;
        }
    }
    
    private void spawnPowerUp() {
        int width = Math.max(getWidth(), 800);
        int height = Math.max(getHeight(), 600);

        int x = rand.nextInt(width - 20);
        int y = rand.nextInt(height - 20);
        powerUps.add(new PowerUp(x, y, PowerUp.getRandomType()));
    }
    
    private void updatePowerUps() {
        long now = System.currentTimeMillis();

        // allow spawning of power up every 7 seconds 
        if (now - lastSpawn > spawnDelay) {
            spawnPowerUp();
            lastSpawn = now;
        }

        // collision
        Iterator<PowerUp> it = powerUps.iterator();

        while (it.hasNext()) {
            PowerUp p = it.next();

            for (Player player : players) {
                if (player.getBounds().intersects(p.getBounds())) {

                    applyEffect(player, p.type);
                    it.remove();
                    break;
                }
            }
        }
    }
    
    public void placeTrap(Player player) {
        if (player.trapCharges <= 0) return;

        traps.add(new Trap(player.x, player.y, player));
        player.trapCharges--;
    }
    
    private void updateTraps(ArrayList<Player> players) {
        Iterator<Trap> it = traps.iterator();

        while (it.hasNext()) {
            Trap t = it.next();

            for (Player p : players) {
                if (p == t.owner) continue;

                if (p.getBounds().intersects(t.getBounds())) {

                    // apply freeze effect to player (2 seconds)
                    activeEffects.putIfAbsent(p, new ArrayList<>());
                    activeEffects.get(p).add(new Effect(PowerUp.Type.FREEZE, 2000));

                    it.remove();
                    break;
                }
            }
        }
    }
    
    private void updateEffects() {
        long now = System.currentTimeMillis();

        for (Player player : players) {

            // skip if no effects
            if (!activeEffects.containsKey(player)) continue;

            java.util.List<Effect> effects = activeEffects.get(player);

            Iterator<Effect> it = effects.iterator();

            boolean hasSpeed = false;
            boolean isFrozen = false;
            boolean isImmune = false;
            boolean isInvisible = false;

            while (it.hasNext()) {
                Effect e = it.next();

                if (now > e.endTime) {
                    it.remove();
                } else {
                    if (e.type == PowerUp.Type.SPEED) hasSpeed = true;
                    if (e.type == PowerUp.Type.FREEZE) isFrozen = true;
                    if( e.type == PowerUp.Type.SHIELD) isImmune = true;
                    if( e.type == PowerUp.Type.GHOST) isInvisible = true;
                }
            }

            player.speed = hasSpeed ? 6 : 4;
            player.isFrozen = isFrozen;
            //player.isImmune = isImmune;
            player.isInvisible = isInvisible;
        }
    }
}