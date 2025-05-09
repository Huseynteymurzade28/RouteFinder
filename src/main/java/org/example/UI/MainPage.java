package org.example.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

public class MainPage extends JFrame {
    private float alpha = 0f; // For fade-in effect
    private Timer fadeTimer;
    private Image backgroundImage;

    public MainPage() {
        setTitle("Welcome to Baku Route Finder");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setUndecorated(true); // For custom window shape
        setShape(new RoundRectangle2D.Double(0, 0, 1000, 700, 30, 30)); // Rounded corners

        // Load background image (using resource path)
        try {
            // Option 1: Load from resources folder (recommended)
            URL imageUrl = getClass().getResource("/Assets/AppBackground.jpg");
            if (imageUrl != null) {
                backgroundImage = new ImageIcon(imageUrl).getImage();
            } else {
                // Option 2: Fallback to absolute path (for testing)
                backgroundImage = new ImageIcon("src/main/java/org/example/Assets/AppBackground.jpg").getImage();
                System.out.println("Using absolute path fallback");
            }
        } catch (Exception e) {
            System.out.println("Error loading image: " + e.getMessage());
            // Fallback to solid color if image fails to load
            backgroundImage = null;
        }

        // Main content panel with custom painting
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Paint background
                if (backgroundImage != null) {
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    // Fallback gradient background
                    Paint gradient = new GradientPaint(0, 0, new Color(18, 137, 167),
                            getWidth(), getHeight(), new Color(29, 209, 161));
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }

                // Apply fade-in effect
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            }
        };
        mainPanel.setLayout(new BorderLayout());
        setContentPane(mainPanel);

        // Semi-transparent overlay panel
        JPanel overlayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Semi-transparent rounded rectangle
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

                // Border
                g2d.setColor(new Color(255, 255, 255, 220));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 30, 30);
                g2d.dispose();
            }
        };
        overlayPanel.setOpaque(false);
        overlayPanel.setLayout(new BorderLayout());
        overlayPanel.setPreferredSize(new Dimension(600, 300));
        overlayPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        mainPanel.add(overlayPanel, BorderLayout.CENTER);

        // Welcome label
        JLabel welcomeLabel = new JLabel("Baku Rota Bulucu'ya Hoşgeldiniz!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 32));
        welcomeLabel.setForeground(new Color(44, 62, 80));
        overlayPanel.add(welcomeLabel, BorderLayout.CENTER);

        // Start button
        JButton startButton = new JButton("BAŞLA") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Button background
                Paint gradient = new GradientPaint(0, 0, new Color(46, 204, 113),
                        0, getHeight(), new Color(39, 174, 96));
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

                // Button text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(getText(), x, y);

                // Hover effect
                if (getModel().isRollover()) {
                    g2d.setColor(new Color(255, 255, 255, 50));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                }
                g2d.dispose();
            }
        };
        startButton.setContentAreaFilled(false);
        startButton.setBorderPainted(false);
        startButton.setFocusPainted(false);
        startButton.setPreferredSize(new Dimension(180, 50));
        startButton.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        overlayPanel.add(startButton, BorderLayout.SOUTH);

        // Close button (X)
        JButton closeButton = new JButton("✕");
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setForeground(new Color(255, 255, 255, 180));
        closeButton.setFont(new Font("Arial", Font.PLAIN, 20));
        closeButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        closeButton.addActionListener(e -> System.exit(0));

        // Close button hover effect
        closeButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(new Color(255, 255, 255, 180));
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setOpaque(false);
        topPanel.add(closeButton);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Button action
        startButton.addActionListener((ActionEvent e) -> {
            animateClose(() -> {
                MapViewer mapViewer = new MapViewer();
                mapViewer.start();
                dispose();
            });
        });

        // Enable window dragging
        enableDrag();

        // Start fade-in animation
        fadeIn();
    }

    private void fadeIn() {
        fadeTimer = new Timer(30, e -> {
            alpha += 0.05f;
            if (alpha >= 1f) {
                alpha = 1f;
                fadeTimer.stop();
            }
            repaint();
        });
        fadeTimer.start();
    }

    private void animateClose(Runnable onComplete) {
        Timer timer = new Timer(20, e -> {
            alpha -= 0.05f;
            if (alpha <= 0) {
                alpha = 0;
                ((Timer)e.getSource()).stop();
                onComplete.run();
            }
            setOpacity(alpha);
        });
        timer.start();
    }

    private void enableDrag() {
        final Point[] initialClick = new Point[1];

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick[0] = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                int thisX = getLocation().x;
                int thisY = getLocation().y;
                int xMoved = e.getX() - initialClick[0].x;
                int yMoved = e.getY() - initialClick[0].y;
                setLocation(thisX + xMoved, thisY + yMoved);
            }
        });
    }
}