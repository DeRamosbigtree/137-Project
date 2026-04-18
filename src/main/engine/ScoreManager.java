package main.engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import main.model.Player;

/**
 * ScoreManager tracks how long each player has been "it" and renders scores and winner info.
 * The player who is "it" the least amount of time wins.
 */
public class ScoreManager {
    private ArrayList<Player> players;
    private long lastUpdateTime;

    // constructor
    public ScoreManager(ArrayList<Player> players) {
        this.players = players;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // update time for the current "it" player
    public void updateScore(Player currentIt) {
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastUpdateTime;

        if (currentIt != null) {
            currentIt.timeAsIt += timePassed;
        }

        lastUpdateTime = currentTime;
    }

    // render each player's score at the bottom of the screen
    public void drawScores(Graphics g, int panelWidth, int panelHeight){
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));

        int yPos = panelHeight - 20;
        int spacing = panelWidth / players.size();

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            
            // convert milliseconds to seconds for display
            long secondsAsIt = p.timeAsIt / 1000; 
            
            int xPos = (i * spacing) + 50; 
            g.drawString("P" + p.id + " Time: " + secondsAsIt + "s", xPos, yPos);
        }
    }

    // display the winner (player with lowest time as "it") centered on screen
    public void drawWinner(Graphics g, int panelWidth, int panelHeight) {

        Player winner = players.get(0);

        // find player with minimum time spent as "it"
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
