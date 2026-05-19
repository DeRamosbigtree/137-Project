package main;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import javax.imageio.ImageIO;
import javax.swing.*;
import main.engine.GameLoop;
import main.engine.GamePanel;
import main.network.GameServer;

public class MainMenu extends JPanel implements MouseMotionListener, MouseListener, KeyListener {

    private Rectangle hostButton;
    private Rectangle joinButton;
    private int screenSize = 1000;
    
    private BufferedImage hostBtn, hostBtnHover;
    private BufferedImage joinBtn, joinBtnHover;
    private BufferedImage background;
    
    

    private String message = "";
    private boolean enteringIP = false;
    private String ipInput = "";

    public MainMenu() {

        setPreferredSize(new Dimension(1200, 720));
        setBackground(Color.BLACK);

        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);

        setFocusable(true);
        requestFocusInWindow();

        hostButton = new Rectangle(450, 250, 300, 100);
        joinButton = new Rectangle(450, 330, 300, 100);
        
        getImage();
    }
    
    private String getLocalIP() {

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
    // alternative method to get local IP via wifi
    // private String getLocalIP() {
    //     try {
    //         java.net.DatagramSocket socket = new java.net.DatagramSocket();

    //         socket.connect(
    //             java.net.InetAddress.getByName("8.8.8.8"),
    //             10002
    //         );

    //         String ip = socket.getLocalAddress().getHostAddress();

    //         socket.close();

    //         return ip;

    //     } catch (Exception e) {
    //         return "localhost";
    //     }
    // }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        
        Point mousePos = getMousePosition();

        // BUTTONS
        drawButton(g, hostButton, mousePos, "HOST");
        drawButton(g, joinButton, mousePos, "JOIN");

        // FOOTER
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 16));

        String footer = "Host starts server • Join connects using server IP";

        int footerWidth = g.getFontMetrics().stringWidth(footer);

        g.drawString(
                footer,
                (1200 - footerWidth) / 2,
                560
        );

        // MESSAGE
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 18));

        int msgWidth = g.getFontMetrics().stringWidth(message);

        g.drawString(
                message,
                (1200 - msgWidth) / 2,
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
                    (1200 - width) / 2,
                    520
            );
        }
    }
    
    private void drawButton(Graphics g, Rectangle rect, Point mousePos, String text) {
    	
    	if(text=="HOST") {
    		if (mousePos != null && rect.contains(mousePos)) {
                g.drawImage(hostBtnHover, rect.x, rect.y, rect.width, rect.height, null);
            } else {
                g.drawImage(hostBtn, rect.x, rect.y, rect.width, rect.height, null);
            }
    	}else if(text=="JOIN"){
    		if (mousePos != null && joinButton.contains(mousePos)) {
                g.drawImage(joinBtnHover, joinButton.x, joinButton.y, joinButton.width, joinButton.height, null);
            } else {
                g.drawImage(joinBtn, joinButton.x, joinButton.y, joinButton.width, joinButton.height, null);
            }
    	}
    	
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

        // GamePanel panel = new GamePanel(host);
        boolean isHost = host.equals(getLocalIP());

        GamePanel panel = new GamePanel(host, isHost);

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
    
    @Override
    public void mouseMoved(MouseEvent e) {
        repaint(); // Tells the panel to re-check if the mouse is inside a button rectangle
    }

    @Override
    public void mouseDragged(MouseEvent e) {}

    public static void main(String[] args) {

        JFrame window = new JFrame("Tag Game");

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        window.add(new MainMenu());

        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
    
    public void getImage() {
    	try {
            hostBtn = ImageIO.read(getClass().getResourceAsStream("/res/other/hostgame.png"));
            hostBtnHover   = ImageIO.read(getClass().getResourceAsStream("/res/other/hostgamehover.png"));
            joinBtn = ImageIO.read(getClass().getResourceAsStream("/res/other/joingame.png"));
            joinBtnHover   = ImageIO.read(getClass().getResourceAsStream("/res/other/joingamehover.png"));
            background   = ImageIO.read(getClass().getResourceAsStream("/res/other/mainmenuplaceholder.png"));
        } catch (Exception e) {
            System.err.println("Error loading menu button images!");
            e.printStackTrace();
        }
    }
}