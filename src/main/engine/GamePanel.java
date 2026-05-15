package main.engine;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import main.input.KeyHandler;
import main.model.Player;
import main.model.PowerUp;
import main.model.Trap;
import main.network.GameClient;
import tile.TileManager;

public class GamePanel extends JPanel {
    private String serverHost;
    private boolean isHost;
	
	TileManager tileM = new TileManager(this);
	ObstacleCollisionChecker oChecker = new ObstacleCollisionChecker(this);
    private final ArrayList<Player> players = new ArrayList<>();
    private final KeyHandler keyH = new KeyHandler();
    

    private Player mainPlayer;
    private GameClient client;

    private final GameTimer timer = new GameTimer(70);

    private ScoreManager scoreManager;
    
    //private final ArrayList<PowerUp> powerUps = new ArrayList<>();
    private final Random rand = new Random();

    //private long lastSpawn = 0;
    //private final long spawnDelay = 3000; //  seconds
    public final int tileSize = 30;
    public final int maxScreenCol = 30;
    public final int maxScreenRow = 20;
    
    // for player sprite
    public BufferedImage up1, up2, down1, down2, left1, left2, right1, right2, frozen,
    					 p1up1, p1up2, p1down1, p1down2, p1left1, p1left2, p1right1, p1right2,
    					 p2up1, p2up2, p2down1, p2down2, p2left1, p2left2, p2right1, p2right2,
    					 p2down, p3down,
    					 speed, freeze, shield, ghost, barrier, trap;
    
    
    
    public GamePanel(String host) {
        this.serverHost = host;
        this.isHost = host.equals("localhost");

        this.setPreferredSize(new Dimension(900, 600));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(keyH);
        
        getPlayerImage();
        getPowerupImage();

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

    private void drawServerInfo(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("Arial", Font.BOLD, 14));

        String role = isHost ? "HOST" : "CLIENT";
        String idText = "Player ID: ";

        if (client != null && client.playerId >= 0) {
            idText += client.playerId;
        } else {
            idText += "Waiting...";
        }

        String serverText = "Mode: " + role + " | Server: " + serverHost;
        String phaseText = "Phase: " + ((client != null) ? client.phase : "LOBBY");
        String connectedText = "Players connected: " + ((client != null) ? client.playerStates.size() : 0) + "/4";

        g.drawString(serverText, 10, 560);
        g.drawString(idText, 10, 580);

        int phaseWidth = g.getFontMetrics().stringWidth(phaseText);
        int connectedWidth = g.getFontMetrics().stringWidth(connectedText);

        g.drawString(phaseText, getWidth() - phaseWidth - 10, 560);
        g.drawString(connectedText, getWidth() - connectedWidth - 10, 580);
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
                
                // mapping server int to the assigned strings
                int dirInt       = s[7];
                switch(dirInt) {
                    case 0: p.direction = "up"; break;
                    case 1: p.direction = "down"; break;
                    case 2: p.direction = "left"; break;
                    case 3: p.direction = "right"; break;
                }
                
                // sync the sprite frame
                p.spriteNum      = s[8];
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
        
        mainPlayer.collisionOn = false;
        oChecker.checkTile(mainPlayer);
        
        if (keyH.up) { 
        	if(mainPlayer.collisionOn == false) {
        		dy--;
        	}
        }
        if (keyH.down) {
        	if(mainPlayer.collisionOn == false) {
        		dy++;
        	}
        }
        if (keyH.left) {
        	if(mainPlayer.collisionOn == false) {
        		dx--;
        	}
        }
        if (keyH.right) {
        	if(mainPlayer.collisionOn == false) {
        		dx++;
        	}
        }

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
        Graphics2D g2 = (Graphics2D)g;
        String phase = (client != null) ? client.phase : "LOBBY";
        
        tileM.draw(g2);
        
        drawPlayers(g2);

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
        if (
            phase.equals("LOBBY")
            || phase.equals("COUNTDOWN")
        ) {
            drawServerInfo(g);
        }
    }
    
    

    private void drawPlayers(Graphics2D g) {
    	BufferedImage image = down1;
    	BufferedImage barriershield = barrier;
    	int temptilesize = tileSize;
    	
        for (Player p : players) {
        	if (p.isInvisible) {
                // if it's an enemy player who is invisible, skip drawing them entirely
                if (client != null && p.id != client.playerId) {
                    continue; 
                }
            }
        	
        	switch(p.id) {
        	case 0:
        		if(p.isFrozen) {
        			image = frozen;
        		}else {
        			switch(p.direction) {
                	case "up":
                		if(p.spriteNum == 1) {
                			image = up1;
                		}
                		if(p.spriteNum == 2) {
                			image = up1;
                		}
                		break;
                	case "down":
                		if(p.spriteNum == 1) {
                			image = down1;
                		}
                		if(p.spriteNum == 2) {
                			image = down2;
                		}
                		break;
                	case "left":
                		if(p.spriteNum == 1) {
                			image = left1;
                		}
                		if(p.spriteNum == 2) {
                			image = left2;
                		}
                		break;
                	case "right":
                		if(p.spriteNum == 1) {
                			image = right1;
                		}
                		if(p.spriteNum == 2) {
                			image = right2;
                		}
                		break;
                	}
        		}
        		
        		break;
        	// player 1
        	case 1:
        		if(p.isFrozen) {
        			image = frozen;
        		}else {
        			switch(p.direction) {
                	case "up":
                		if(p.spriteNum == 1) {
                			image = p1up1;
                		}
                		if(p.spriteNum == 2) {
                			image = p1up1;
                		}
                		
                		break;
                	case "down":
                		if(p.spriteNum == 1) {
                			image = p1down1;
                		}
                		if(p.spriteNum == 2) {
                			image = p1down1;
                		}
                		break;
                	case "left":
                		if(p.spriteNum == 1) {
                			image = p1left1;
                		}
                		if(p.spriteNum == 2) {
                			image = p1left1;
                		}
                		break;
                	case "right":
                		if(p.spriteNum == 1) {
                			image = p1right1;
                		}
                		if(p.spriteNum == 2) {
                			image = p1right1;
                		}
                		break;
                	}
        		}
        		break;
        	// player 2 
        	case 2: 
        		if(p.isFrozen) {
        			image = frozen;
        		}else {
        			switch(p.direction) {
                	case "up":
                		if(p.spriteNum == 1) {
                			image = p2up1;
                		}
                		if(p.spriteNum == 2) {
                			image = p2up1;
                		}
                		
                		break;
                	case "down":
                		if(p.spriteNum == 1) {
                			image = p2down1;
                		}
                		if(p.spriteNum == 2) {
                			image = p2down1;
                		}
                		break;
                	case "left":
                		if(p.spriteNum == 1) {
                			image = p2left1;
                		}
                		if(p.spriteNum == 2) {
                			image = p2left1;
                		}
                		break;
                	case "right":
                		if(p.spriteNum == 1) {
                			image = p2right1;
                		}
                		if(p.spriteNum == 2) {
                			image = p2right1;
                		}
                		break;
        			}
        		}
        		break;
        	case 3:
        		//temptilesize = 40;
        		image = p3down;
        		break;
        	
        	}
        	
        	Composite originalComposite = g.getComposite();
        	
        	// make player semi-transparent if it's the current client 
        	if (p.isInvisible && client != null && p.id == client.playerId) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            }
        	
        	g.drawImage(image, p.x, p.y, temptilesize, temptilesize,null);
        	
        	if(p.isIt) {
        		g.setColor(Color.RED);
        	}else {
        		g.setColor(Color.WHITE);
        	}
            g.drawString("P" + p.id, p.x + 8, p.y - 5);
            
            if(p.isImmune) {
            	g.drawImage(barriershield, p.x-5, p.y-5, 40,40,null);
            }
            
            g.setComposite(originalComposite);
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
    
    public void getPowerupImage(){
    	try {
    		speed = ImageIO.read(getClass().getResourceAsStream("/powerups/speed.png"));
    		freeze = ImageIO.read(getClass().getResourceAsStream("/powerups/freeze.png"));
    		shield = ImageIO.read(getClass().getResourceAsStream("/powerups/shield.png"));
    		ghost = ImageIO.read(getClass().getResourceAsStream("/powerups/ghost.png"));
    		barrier = ImageIO.read(getClass().getResourceAsStream("/powerups/barrier.png"));
    		trap = ImageIO.read(getClass().getResourceAsStream("/powerups/trap.png"));
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    private void drawPowerUps(Graphics g) {
    	if (client == null) return;
    	
    	BufferedImage powerup = speed;
    	BufferedImage traptile = trap;
    	
        for (PowerUp p : client.powerUps) {
            switch (p.type) {
                case SPEED: powerup = speed; break;
                case FREEZE: powerup = freeze; break;
                case SHIELD: powerup = shield; break;
                case GHOST: powerup = ghost; break;
            }
            
            g.drawImage(powerup, p.x, p.y, tileSize, tileSize,null); 
        }
        
        // draw traps
        for (Trap t : client.traps) {
        	g.drawImage(traptile, t.x, t.y, tileSize, tileSize,null); 
        }
    }
    
    public void placeTrap(Player player) {

       
                client.send("TRAP");
        
    }
    
    public void getPlayerImage(){
    	try {
    		// player0 sprites
    		up1 = ImageIO.read(getClass().getResourceAsStream("/player/up1.png"));
    		down1 = ImageIO.read(getClass().getResourceAsStream("/player/down1.png"));
    		down2 = ImageIO.read(getClass().getResourceAsStream("/player/down2.png"));
    		left1 = ImageIO.read(getClass().getResourceAsStream("/player/left1.png"));
    		left2 = ImageIO.read(getClass().getResourceAsStream("/player/left2.png"));
    		right1 = ImageIO.read(getClass().getResourceAsStream("/player/right1.png"));
    		right2 = ImageIO.read(getClass().getResourceAsStream("/player/right2.png"));
    		frozen = ImageIO.read(getClass().getResourceAsStream("/player/frozen.png"));
    		
    		// player1 sprites
    		p1up1 = ImageIO.read(getClass().getResourceAsStream("/p1/up1.png"));
    		p1down1 = ImageIO.read(getClass().getResourceAsStream("/p1/player.png"));
    		p1left1 = ImageIO.read(getClass().getResourceAsStream("/p1/left1.png"));
    		p1right1 = ImageIO.read(getClass().getResourceAsStream("/p1/right1.png"));
    		
    		// player1 sprites
    		p2up1 = ImageIO.read(getClass().getResourceAsStream("/p2/up1.png"));
    		p2down1 = ImageIO.read(getClass().getResourceAsStream("/p2/player.png"));
    		p2left1 = ImageIO.read(getClass().getResourceAsStream("/p2/left1.png"));
    		p2right1 = ImageIO.read(getClass().getResourceAsStream("/p2/right1.png"));
    		
    		// player1 sprites
    		p3down = ImageIO.read(getClass().getResourceAsStream("/p3/player4.png"));
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
}