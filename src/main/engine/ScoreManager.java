package main.engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import main.model.Player;


public class ScoreManager {
    private ArrayList<Player> players;
    private long lastUpdateTime;

    public ScoreManager(ArrayList<Player> players) {
        this.players = players;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void updateScore(Player currentIt) {
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastUpdateTime;

        if (currentIt != null) {
            currentIt.timeAsIt += timePassed;
        }

        lastUpdateTime = currentTime;
    }

    public void drawScores(Graphics g, int panelWidth, int panelHeight){
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));

        int yPos = panelHeight - 20;
        int spacing = panelWidth / players.size();

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            
            // Convert milliseconds back to seconds for the UI
            long secondsAsIt = p.timeAsIt / 1000; 
            
            int xPos = (i * spacing) + 50; 
            g.drawString("P" + p.id + " Time: " + secondsAsIt + "s", xPos, yPos);
        }
    }

    public void drawWinner(Graphics g, int panelWidth, int panelHeight) {

        Player winner = players.get(0);

        for (Player p : players) {
            if (p.timeAsIt < winner.timeAsIt) { 
                winner = p;
            }
        }

        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        
        String winText = "WINNER: PLAYER " + winner.id + "!";
        int textWidth = g.getFontMetrics().stringWidth(winText);
        
        g.drawString(winText, (panelWidth - textWidth) / 2, panelHeight / 2 + 50);
    }

}
