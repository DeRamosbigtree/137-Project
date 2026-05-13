package main;

import javax.swing.JFrame;
import main.engine.GameLoop;
import main.engine.GamePanel;

public class Game {
    public static void main(String[] args) {

        JFrame window = new JFrame("Tag Game");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel panel = new GamePanel("localhost");
        window.add(panel);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        new Thread(new GameLoop(panel)).start();
    }
}