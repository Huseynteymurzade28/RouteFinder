package org.example.Map;

import javax.swing.*;

public class MapFrame extends JFrame {
    public MapFrame() {
        setTitle("Route Finder");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new MapPanel());  // Harita panelini ekle
        setVisible(true);
    }
}
