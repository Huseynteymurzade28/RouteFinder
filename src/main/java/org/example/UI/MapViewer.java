package org.example.UI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // Added import
import org.example.Model.Dijkstra;
import org.example.Model.Edge;
import org.example.Model.Node;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MapViewer {
    private final Map<Node, List<Edge>> graph = new HashMap<>();
    private final List<Node> nodes = new ArrayList<>();
    private Node startNode = null;
    private Node endNode = null;
    private List<GeoPosition> selectedRoute = new ArrayList<>();
    private final JXMapViewer mapViewer = new JXMapViewer();
    private int clickCount = 0;
    private RoutePainter shortestPathPainter;
    private JButton clearButton;
    private JButton helpButton;
    private JLabel statusLabel;
    private JPanel recommendationPanel;
    private JTextArea recommendationTextArea;
    private final static double EARTH_RADIUS_KM = 6371.0;

    @JsonIgnoreProperties(ignoreUnknown = true) // Added annotation
    public static class Segment {
        public String from;
        public String to;
        public String tip;
        public String hat;
        public String aciklama;

        public Segment() {}
    }

    public void start() {
        JFrame frame = new JFrame("Route Finder - Baku");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);

        UIManager.put("Button.background", new ColorUIResource(60, 63, 65));
        UIManager.put("Button.foreground", new ColorUIResource(187, 187, 187));
        UIManager.put("Label.foreground", new ColorUIResource(255, 255, 255));
        UIManager.put("Panel.background", new ColorUIResource(43, 43, 43));

        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(8);
        mapViewer.setTileFactory(tileFactory);

        mapViewer.setZoom(6);
        mapViewer.setAddressLocation(new GeoPosition(40.4093, 49.8671));

        JPanel controlPanel = new JPanel();
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        clearButton = new JButton("Temizle");
        helpButton = new JButton("Yardım");
        statusLabel = new JLabel("Başlangıç noktasını seçmek için haritaya tıklayın");

        addHoverEffect(clearButton);
        addHoverEffect(helpButton);

        clearButton.addActionListener(e -> resetSelection());

        helpButton.addActionListener(e -> JOptionPane.showMessageDialog(frame,
                "Kullanım:\n" +
                        "1. Haritada bir noktaya tıklayarak başlangıç noktasını seçin\n" +
                        "2. Başka bir noktaya tıklayarak bitiş noktasını seçin\n" +
                        "3. Yol otomatik olarak hesaplanacaktır\n" +
                        "4. Yeni bir yol seçmek için 'Temizle' düğmesine basın\n" +
                        "- Fare tekerleğiyle yakınlaştırabilir/uzaklaştırabilirsiniz\n" +
                        "- Haritayı sürükleyerek hareket ettirebilirsiniz",
                "Yardım", JOptionPane.INFORMATION_MESSAGE));

        controlPanel.add(clearButton);
        controlPanel.add(helpButton);
        controlPanel.add(statusLabel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(mapViewer, BorderLayout.CENTER);

        recommendationPanel = new JPanel();
        recommendationPanel.setLayout(new BorderLayout());
        recommendationPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        recommendationPanel.setBackground(new Color(43, 43, 43));

        JLabel recommendationLabel = new JLabel("Önerilen Otobüs veya Metro:");
        recommendationLabel.setForeground(Color.WHITE);
        recommendationTextArea = new JTextArea(5, 30);
        recommendationTextArea.setEditable(false);
        recommendationTextArea.setLineWrap(true);
        recommendationTextArea.setWrapStyleWord(true);
        recommendationTextArea.setBackground(new Color(60, 63, 65));
        recommendationTextArea.setForeground(new Color(187, 187, 187));

        recommendationPanel.add(recommendationLabel, BorderLayout.NORTH);
        recommendationPanel.add(new JScrollPane(recommendationTextArea), BorderLayout.CENTER);
        recommendationPanel.setVisible(false);

        mainPanel.add(recommendationPanel, BorderLayout.SOUTH);

        setupMapNavigation();

        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    handleMapClick(e.getPoint());
                }
            }
        });

        createMockGraph();
        updateMapPainters();

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void setupMapNavigation() {
        MouseAdapter panMouseAdapter = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(panMouseAdapter);
        mapViewer.addMouseMotionListener(panMouseAdapter);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));
    }

    private void handleMapClick(Point point) {
        GeoPosition clickedPosition = mapViewer.convertPointToGeoPosition(point);
        if (clickedPosition == null) return;

        Node identifiedNode = findNearestNode(clickedPosition, 0.005); // ~500m tolerance

        switch (clickCount) {
            case 0: // Selecting start node
                if (identifiedNode != null) {
                    startNode = identifiedNode;
                    statusLabel.setText("Başlangıç noktası seçildi (" + startNode.getId() + "). Şimdi bitiş noktasını seçin.");
                    clickCount++;
                } else {
                    JOptionPane.showMessageDialog(null, "Lütfen haritada tanımlı bir istasyona yakın bir nokta seçin.", "Başlangıç Noktası Seçilemedi", JOptionPane.WARNING_MESSAGE);
                    startNode = null; // Ensure startNode is not set
                }
                break;
            case 1: // Selecting end node
                if (identifiedNode != null) {
                    if (identifiedNode.equals(startNode)) {
                        JOptionPane.showMessageDialog(null, "Başlangıç ve bitiş noktaları aynı olamaz.", "Hata", JOptionPane.WARNING_MESSAGE);
                        endNode = null; // Do not set endNode
                    } else {
                        endNode = identifiedNode;
                        statusLabel.setText("Bitiş noktası seçildi (" + endNode.getId() + "). Rota hesaplanıyor...");
                        findAndDrawRoute();
                        clickCount = 2;
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Lütfen haritada tanımlı bir istasyona yakın bir nokta seçin.", "Bitiş Noktası Seçilemedi", JOptionPane.WARNING_MESSAGE);
                    endNode = null; // Ensure endNode is not set
                }
                break;
            default: // Path already found or selection process complete
                JOptionPane.showMessageDialog(null, "Yeni bir rota seçmek için önce 'Temizle' düğmesine basın.", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
                break;
        }
        updateMapPainters(); // Update map to show selected nodes
    }

    private Node findNearestNode(GeoPosition position, double maxDistance) {
        Node nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (Node node : nodes) {
            double distance = calculateHaversineDistance(
                    position.getLatitude(), position.getLongitude(),
                    node.getPosition().getLatitude(), node.getPosition().getLongitude()
            );
            if (distance < minDistance && distance < maxDistance) {
                minDistance = distance;
                nearest = node;
            }
        }
        return nearest;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private void resetSelection() {
        startNode = null;
        endNode = null;
        selectedRoute.clear();
        clickCount = 0;
        shortestPathPainter = null;
        recommendationPanel.setVisible(false);
        statusLabel.setText("Başlangıç noktasını seçmek için haritaya tıklayın");
        updateMapPainters();
    }

    private void createMockGraph() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream stopsStream = getClass().getResourceAsStream("/StopsAndStations.json");
            if (stopsStream == null) throw new RuntimeException("StopsAndStations.json not found in resources");
            List<Map<String, Object>> stopsAndStations = objectMapper.readValue(
                    stopsStream, new TypeReference<List<Map<String, Object>>>() {}
            );

            Map<String, Node> nodeMapByName = new HashMap<>();
            for (Map<String, Object> station : stopsAndStations) {
                String name = (String) station.get("name");
                double latitude = (double) station.get("latitude");
                double longitude = (double) station.get("longitude");
                Node node = new Node(name, latitude, longitude);
                nodes.add(node);
                nodeMapByName.put(name, node);
                graph.put(node, new ArrayList<>());
            }

            InputStream transportsStream = getClass().getResourceAsStream("/Transports.json");
            if (transportsStream == null) throw new RuntimeException("Transports.json not found in resources");
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(transportsStream);
            com.fasterxml.jackson.databind.JsonNode segmentsNode = root.get("segments");
            if (segmentsNode != null && segmentsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode segmentNode : segmentsNode) {
                    String fromName = segmentNode.get("from").asText();
                    String toName = segmentNode.get("to").asText();
                    Node fromNode = nodeMapByName.get(fromName);
                    Node toNode = nodeMapByName.get(toName);

                    if (fromNode != null && toNode != null) {
                        double distance = calculateHaversineDistance(
                                fromNode.getPosition().getLatitude(), fromNode.getPosition().getLongitude(),
                                toNode.getPosition().getLatitude(), toNode.getPosition().getLongitude()
                        );
                        // Add edge from -> to
                        graph.get(fromNode).add(new Edge(fromNode, toNode, distance));
                        // Add reverse edge to -> from, assuming segments are bidirectional
                        graph.get(toNode).add(new Edge(toNode, fromNode, distance));
                    }
                }
            }

        } catch (java.io.IOException e) { // More specific exception
            JOptionPane.showMessageDialog(null, "JSON verileri okunurken bir G/Ç hatası oluştu: " + e.getMessage(),
                    "Veri Yükleme Hatası", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) { // Catch runtime exceptions (e.g., file not found)
            JOptionPane.showMessageDialog(null, "JSON verileri yüklenirken bir çalışma zamanı hatası oluştu: " + e.getMessage(),
                    "Veri Yükleme Hatası", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMapPainters() {
        List<Painter<JXMapViewer>> painters = new ArrayList<>();

        WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<>();
        Set<Waypoint> waypoints = new HashSet<>();
        for (Node node : nodes) {
            waypoints.add(new DefaultWaypoint(node.getPosition()));
        }
        waypointPainter.setWaypoints(waypoints);

        waypointPainter.setRenderer((g, map, waypoint) -> {
            Point2D point = map.getTileFactory().geoToPixel(waypoint.getPosition(), map.getZoom());
            Node node = nodes.stream()
                    .filter(n -> n.getPosition().equals(waypoint.getPosition()))
                    .findFirst().orElse(null);

            if (node != null) {
                int diameter = 10;
                Color nodeColor = Color.BLUE;
                if (node.equals(startNode)) {
                    nodeColor = Color.GREEN;
                    diameter = 14;
                } else if (node.equals(endNode)) {
                    nodeColor = Color.ORANGE;
                    diameter = 14;
                }
                g.setColor(nodeColor);
                g.fillOval((int) point.getX() - diameter / 2, (int) point.getY() - diameter / 2, diameter, diameter);
                g.setColor(Color.BLACK);
                g.drawOval((int) point.getX() - diameter / 2, (int) point.getY() - diameter / 2, diameter, diameter);
            }
        });
        painters.add(waypointPainter);

        if (shortestPathPainter != null) {
            painters.add(shortestPathPainter);
        }

        CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(compoundPainter);
        mapViewer.repaint();
    }

    private void findAndDrawRoute() {
        if (startNode == null || endNode == null) {
            return;
        }

        Dijkstra dijkstra = new Dijkstra(graph);
        List<List<Node>> steps = dijkstra.findShortestPath(startNode, endNode);
        List<Node> shortestPathNodes = null;
        if (steps != null && !steps.isEmpty()) {
            shortestPathNodes = steps.get(steps.size() - 1);
        }

        if (shortestPathNodes == null || shortestPathNodes.size() < 2) {
            JOptionPane.showMessageDialog(null, "Bu noktalar arasında rota bulunamadı.", "Rota Bulunamadı", JOptionPane.WARNING_MESSAGE);
            statusLabel.setText("Rota bulunamadı. Lütfen tekrar deneyin.");
            endNode = null; // Allow user to re-select end node
            clickCount = 1; // Reset click count to allow selecting end node again
            shortestPathPainter = null;
            updateMapPainters();
            return;
        }

        // Added diagnostic logging
        System.out.println("Dijkstra returned path with " + shortestPathNodes.size() + " nodes:");
        for (int i = 0; i < shortestPathNodes.size(); i++) {
            Node node = shortestPathNodes.get(i);
            System.out.println("  " + i + ". " + node.getId() + " (Lat: " + node.getPosition().getLatitude() + ", Lon: " + node.getPosition().getLongitude() + ")");
        }
        // End of added diagnostic logging

        selectedRoute = shortestPathNodes.stream()
                .map(Node::getPosition)
                .collect(Collectors.toList());

        shortestPathPainter = new RoutePainter(selectedRoute); // Removed Color.RED
        shortestPathPainter.setStrokeWidth(5);

        animateRouteFadeIn();

        double totalDistance = calculatePathDistance(shortestPathNodes);
        // Added diagnostic logging
        System.out.println("Calculated total distance by calculatePathDistance method: " + totalDistance + " km");
        // End of added diagnostic logging
        statusLabel.setText(String.format("Rota bulundu! Toplam mesafe: %.2f km", totalDistance));
        zoomToFitRoute(selectedRoute);
        displayRecommendations(shortestPathNodes);
    }

    private void zoomToFitRoute(List<GeoPosition> route) {
        if (route == null || route.isEmpty()) return;

        // Convert List to Set for zoomToBestFit method
        Set<GeoPosition> geoPositions = new HashSet<>(route);

        // Zoom the map to fit all GeoPositions in the route
        // The 0.9 argument means that 90% of the map view will be used for the route,
        // leaving a small margin.
        mapViewer.zoomToBestFit(geoPositions, 0.9);
    }

    private void animateRouteFadeIn() {
        if (shortestPathPainter == null) return;

        javax.swing.Timer timer = new javax.swing.Timer(50, null);
        final float[] alpha = {0.0f};
        timer.addActionListener(e -> {
            alpha[0] += 0.1f;
            if (alpha[0] >= 1.0f) {
                alpha[0] = 1.0f;
                timer.stop();
            }
            shortestPathPainter.setAlpha(alpha[0]);
            updateMapPainters();
        });
        timer.start();
    }

    private double calculatePathDistance(List<Node> path) {
        double distance = 0;
        if (path == null || path.size() < 2) {
            return 0.0;
        }
        System.out.println("Calculating path distance for " + path.size() + " nodes:"); // Log for this method
        for (int i = 0; i < path.size() - 1; i++) {
            Node current = path.get(i);
            Node next = path.get(i + 1);
            Optional<Edge> edge = graph.getOrDefault(current, Collections.emptyList()).stream()
                    .filter(e -> e.getTo().equals(next))
                    .findFirst();
            if (edge.isPresent()) {
                double edgeWeight = edge.get().getWeight();
                distance += edgeWeight;
                System.out.println("  Segment " + i + ": " + current.getId() + " -> " + next.getId() + ", Weight: " + edgeWeight + " km (from graph edge)");
            } else {
                // This block is problematic if Dijkstra is supposed to return paths based on existing graph edges.
                System.err.println("Warning: Edge not found in graph for path segment: " + current.getId() + " -> " + next.getId() + ". Calculating Haversine directly.");
                double haversineDist = calculateHaversineDistance(
                        current.getPosition().getLatitude(), current.getPosition().getLongitude(),
                        next.getPosition().getLatitude(), next.getPosition().getLongitude()
                );
                distance += haversineDist;
                System.out.println("  Segment " + i + ": " + current.getId() + " -> " + next.getId() + ", Weight: " + haversineDist + " km (calculated Haversine - edge missing in graph!)");
            }
        }
        return distance;
    }

    private void displayRecommendations(List<Node> path) {
        if (path == null || path.isEmpty()) {
            recommendationPanel.setVisible(false);
            return;
        }
        List<Segment> segments = loadRouteSegments();
        StringBuilder sb = new StringBuilder("Önerilen ulaşım araçları:\n");
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i).getId();
            String to   = path.get(i + 1).getId();
            Segment match = segments.stream()
                    .filter(s -> s.from.equals(from) && s.to.equals(to))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                sb.append("- ").append(match.tip.toUpperCase())
                  .append(match.hat != null ? " (Hat " + match.hat + ")" : "")
                  .append(": ").append(match.aciklama).append("\n");
            }
        }
        if (sb.toString().equals("Önerilen ulaşım araçları:\n")) {
             recommendationTextArea.setText("Bu rota için özel ulaşım segment bilgisi bulunamadı.");
        } else {
            recommendationTextArea.setText(sb.toString());
        }
        recommendationPanel.setVisible(true);
    }

    private void addHoverEffect(JButton button) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(75, 110, 175));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(60, 63, 65));
            }
        });
    }

    private List<Segment> loadRouteSegments() {
        try (InputStream is = getClass().getResourceAsStream("/Transports.json")) {
            if (is == null) throw new RuntimeException("Transports.json bulunamadı!");
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(is);
            com.fasterxml.jackson.databind.JsonNode arr  = root.get("segments");
            return mapper.convertValue(arr, new TypeReference<List<Segment>>() {});
        } catch (java.io.IOException e) { // Catch specific IOException
            // Wrap IOException in a RuntimeException to avoid changing method signature
            // and provide more context.
            throw new RuntimeException("Transports.json okunurken G/Ç hatası oluştu!", e);
        } catch (Exception e) { // Catch other potential exceptions during JSON processing
            // Wrap other exceptions in a RuntimeException
            throw new RuntimeException("Transports.json işlenirken bir hata oluştu!", e);
        }
    }
}
