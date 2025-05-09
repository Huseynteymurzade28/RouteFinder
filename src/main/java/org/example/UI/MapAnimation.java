package org.example.UI;

import org.example.Model.Node;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapAnimation sınıfı, Dijkstra algoritmasının adımlarını görselleştirmek için kullanılır.
 */
public class MapAnimation {
    private final JXMapViewer mapViewer;
    private final List<List<Node>> steps;
    private final List<GeoPosition> finalPath;
    private int currentStep = 0;
    private javax.swing.Timer animationTimer; // her adım arasında 1 saniye bekleme
    private final int ANIMATION_DELAY = 1000;
    private List<Painter<JXMapViewer>> basePainters; // Temel harita elemanlarını saklar
    private final JLabel statusLabel;
    private final JButton playPauseButton;
    private final JButton nextButton;
    private final JButton prevButton;
    private final JButton resetButton;
    private final JPanel controlPanel;
    private boolean isPlaying = false;

    /**
     * MapAnimation sınıfının yapıcısı
     *
     * @param mapViewer Animasyonun gösterileceği harita görüntüleyici
     * @param steps Dijkstra algoritmasının adımları
     * @param finalPath Son bulunan yol
     * @param basePainters Temel harita elemanları (düğümler, kenarlar vs.)
     */
    public MapAnimation(JXMapViewer mapViewer, List<List<Node>> steps, List<GeoPosition> finalPath,
                        List<Painter<JXMapViewer>> basePainters) {
        this.mapViewer = mapViewer;
        this.steps = steps;
        this.finalPath = finalPath;
        this.basePainters = basePainters;

        // Kontrol paneli oluşturma
        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        // Durum etiketi
        statusLabel = new JLabel("Adım 0/" + steps.size());

        // Kontrol düğmeleri
        playPauseButton = new JButton("►");
        nextButton = new JButton("▶");
        prevButton = new JButton("◀");
        resetButton = new JButton("⟲");

        // Tüm bileşenleri panele ekle
        controlPanel.add(resetButton);
        controlPanel.add(prevButton);
        controlPanel.add(playPauseButton);
        controlPanel.add(nextButton);
        controlPanel.add(statusLabel);

        // Düğmelere olay dinleyicileri ekle
        setupListeners();
    }

    /**
     * Kontrol düğmelerine olay dinleyicilerini ekler
     */
    private void setupListeners() {
        playPauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPlaying) {
                    pauseAnimation();
                } else {
                    playAnimation();
                }
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseAnimation();
                showNextStep();
            }
        });

        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseAnimation();
                showPreviousStep();
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetAnimation();
            }
        });
    }

    /**
     * Animasyonu başlatır
     */
    public void playAnimation() {
        isPlaying = true;
        playPauseButton.setText("❚❚");

        if (animationTimer != null) {
            animationTimer.stop();
        }

        animationTimer = new javax.swing.Timer(ANIMATION_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentStep < steps.size()) {
                    visualizeStep(currentStep);
                    currentStep++;
                    updateStatusLabel();
                } else {
                    pauseAnimation();
                }
            }
        });

        animationTimer.start();
    }

    /**
     * Animasyonu duraklatır
     */
    public void pauseAnimation() {
        isPlaying = false;
        playPauseButton.setText("►");

        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    /**
     * Bir sonraki adımı gösterir
     */
    public void showNextStep() {
        if (currentStep < steps.size()) {
            visualizeStep(currentStep);
            currentStep++;
            updateStatusLabel();
        }
    }

    /**
     * Bir önceki adımı gösterir
     */
    public void showPreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            visualizeStep(currentStep);
            updateStatusLabel();
        }
    }

    /**
     * Animasyonu sıfırlar
     */
    public void resetAnimation() {
        pauseAnimation();
        currentStep = 0;
        updateStatusLabel();

        // Temel duruma geri dön
        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(basePainters);
        mapViewer.setOverlayPainter(painter);
    }

    /**
     * Durum etiketini günceller
     */
    private void updateStatusLabel() {
        statusLabel.setText("Adım " + currentStep + "/" + steps.size());
    }

    /**
     * Belirtilen adımı görselleştirir
     *
     * @param stepIndex Görselleştirilecek adımın indeksi
     */
    private void visualizeStep(int stepIndex) {
        if (stepIndex < 0 || stepIndex >= steps.size()) {
            return;
        }

        List<Node> currentStepNodes = steps.get(stepIndex);
        if (currentStepNodes == null) {
            return;
        }

        List<Painter<JXMapViewer>> painters = new ArrayList<>(basePainters);

        if (stepIndex < steps.size() - 1) {
            WaypointPainter<Waypoint> visitedPainter = createWaypointPainter(currentStepNodes, Color.ORANGE);
            painters.add(visitedPainter);
            String description = "Ziyaret edilen düğümler: " +
                    currentStepNodes.stream().map(Node::getId).collect(Collectors.joining(", "));
            statusLabel.setText("Adım " + stepIndex + "/" + (steps.size() - 1) + ": " + description);
        } else {
            if (!finalPath.isEmpty()) {
                RoutePainter routePainter = new RoutePainter(finalPath, Color.RED);
                painters.add(routePainter);
                statusLabel.setText("Adım " + stepIndex + "/" + (steps.size() - 1) + ": En kısa yol bulundu!");
            }
        }

        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(painter);
        mapViewer.repaint();
    }

    /**
     * Belirtilen düğümler için bir WaypointPainter oluşturur
     *
     * @param nodes Görselleştirilecek düğümler
     * @param color Düğümlerin rengi
     * @return WaypointPainter
     */
    private WaypointPainter<Waypoint> createWaypointPainter(List<Node> nodes, Color color) {
        Set<Waypoint> waypoints = new HashSet<>();

        for (Node node : nodes) {
            waypoints.add(new DefaultWaypoint(node.getPosition()));
        }

        WaypointPainter<Waypoint> painter = new WaypointPainter<>();
        painter.setWaypoints(waypoints);

        painter.setRenderer((g, map, waypoint) -> {
            Point2D point = map.getTileFactory().geoToPixel(waypoint.getPosition(), map.getZoom());

            g.setColor(color);
            g.fillOval((int) point.getX() - 8, (int) point.getY() - 8, 16, 16);
            g.setColor(Color.BLACK);
            g.drawOval((int) point.getX() - 8, (int) point.getY() - 8, 16, 16);

            // Düğüm ID'sini bul
            Node node = findNodeByPosition(nodes, waypoint.getPosition());
            if (node != null) {
                g.setColor(Color.BLACK);
                g.drawString(node.getId(), (int) point.getX() + 10, (int) point.getY() + 4);
            }
        });

        return painter;
    }

    /**
     * Belirtilen konuma sahip düğümü bulur
     *
     * @param nodes Aranacak düğüm listesi
     * @param position Aranacak konum
     * @return Düğüm veya null
     */
    private Node findNodeByPosition(List<Node> nodes, GeoPosition position) {
        for (Node node : nodes) {
            if (node.getPosition().equals(position)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Animasyon kontrol panelini döndürür
     *
     * @return Kontrol paneli
     */
    public JPanel getControlPanel() {
        return controlPanel;
    }

    /**
     * Rotaları çizmek için yardımcı sınıf
     */
    public static class RoutePainter implements Painter<JXMapViewer> {
        private final List<GeoPosition> track;
        private final Color color;

        public RoutePainter(List<GeoPosition> track, Color color) {
            this.track = track;
            this.color = color;
        }

        @Override
        public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
            g = (Graphics2D) g.create();

            if (track == null || track.isEmpty()) {
                return;
            }

            // Geo pozisyonları ekran pozisyonlarına dönüştür
            List<Point2D> points = track.stream()
                    .map(geoPosition -> map.getTileFactory().geoToPixel(geoPosition, map.getZoom()))
                    .collect(Collectors.toList());

            // Daha pürüzsüz çizgiler için rendering ipuçları ayarla
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(4));
            g.setColor(color);

            // Yolu çiz
            for (int i = 0; i < points.size() - 1; i++) {
                Point2D p1 = points.get(i);
                Point2D p2 = points.get(i + 1);
                g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            }

            g.dispose();
        }
    }
}
