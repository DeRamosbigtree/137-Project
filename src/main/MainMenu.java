package main;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import main.engine.GameLoop;
import main.engine.GamePanel;
import main.network.GameServer;
import java.net.InetAddress;

public class MainMenu extends JPanel implements MouseListener, KeyListener {

    private Rectangle hostButton;
    private Rectangle joinButton;
    private int screenSize = 900;

    private String message = "";
    private boolean enteringIP = false;
    private String ipInput = "";

    public MainMenu() {

        setPreferredSize(new Dimension(900, 600));
        setBackground(Color.BLACK);

        addMouseListener(this);
        addKeyListener(this);

        setFocusable(true);
        requestFocusInWindow();

        hostButton = new Rectangle(300, 250, 200, 50);
        joinButton = new Rectangle(300, 330, 200, 50);
    }
    
    private String getLocalIP() {

    try {
        return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
        return "localhost";
    }
}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // TITLE
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 42));

        String title = "4 PLAYER TAG GAME";

        int titleWidth = g.getFontMetrics().stringWidth(title);

        g.drawString(title, (900 - titleWidth) / 2, 150);

        // BUTTONS
        drawButton(g, hostButton, "HOST GAME");
        drawButton(g, joinButton, "JOIN GAME");

        // FOOTER
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 16));

        String footer = "Host starts server • Join connects using server IP";

        int footerWidth = g.getFontMetrics().stringWidth(footer);

        g.drawString(
                footer,
                (800 - footerWidth) / 2,
                560
        );

        // MESSAGE
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 18));

        int msgWidth = g.getFontMetrics().stringWidth(message);

        g.drawString(
                message,
                (900 - msgWidth) / 2,
                470
        );

        // IP INPUT
        if (enteringIP) {

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 22));

            String text = "Enter Server IP: " + ipInput;

            int width = g.getFontMetrics().stringWidth(text);

            g.drawString(
                    text,
                    (900 - width) / 2,
                    520
            );
        }
    }

    private void drawButton(Graphics g, Rectangle rect, String text) {

        g.setColor(Color.DARK_GRAY);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);

        g.setColor(Color.WHITE);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);

        g.setFont(new Font("Arial", Font.BOLD, 22));

        int textWidth = g.getFontMetrics().stringWidth(text);

        int textX = rect.x + (rect.width - textWidth) / 2;
        int textY = rect.y + 32;

        g.drawString(text, textX, textY);
    }

    private void hostGame() {

        new Thread(() -> {
            new GameServer();
        }).start();

        message = "Server started. Other players can now join.";

        repaint();

        String ip = getLocalIP();

        message = "Server started at: " + ip;

        repaint();

        startGame(ip);
    }

    private void joinGame() {

        enteringIP = true;

        message = "Type server IP and press ENTER";

        ipInput = "";

        repaint();

        requestFocusInWindow();
    }

    private void startGame(String host) {

        JFrame window = new JFrame("Tag Game");

        GamePanel panel = new GamePanel(host);

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        window.add(panel);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        new Thread(new GameLoop(panel)).start();

        SwingUtilities.getWindowAncestor(this).dispose();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        requestFocusInWindow();

        Point p = e.getPoint();

        if (hostButton.contains(p)) {
            hostGame();
        }

        if (joinButton.contains(p)) {
            joinGame();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

        if (!enteringIP) return;

        char c = e.getKeyChar();

        if (
            Character.isLetterOrDigit(c)
            || c == '.'
        ) {
            ipInput += c;
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (!enteringIP) return;

        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {

            if (!ipInput.isEmpty()) {
                ipInput = ipInput.substring(0, ipInput.length() - 1);
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {

            if (ipInput.isBlank()) {

                message = "Please enter a valid server IP.";
                repaint();

                return;
            }

            enteringIP = false;

            startGame(ipInput);
        }

        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {

        JFrame window = new JFrame("Tag Game");

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        window.add(new MainMenu());

        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}