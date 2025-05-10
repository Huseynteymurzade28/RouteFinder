package org.example.UI;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
    private List<WaypointPainter<Waypoint>> nodePainters = new ArrayList<>();
    private RoutePainter routePainter;
    private JButton clearButton;
    private JButton helpButton;
    private JLabel statusLabel;
    private JPanel recommendationPanel;
    private JTextArea recommendationTextArea;
    private final static double EARTH_RADIUS_KM = 6371.0;
    @JsonIgnoreProperties(ignoreUnknown = true)// Earth radius in kilometers
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

        // Apply modern UI theme
        UIManager.put("Button.background", new ColorUIResource(60, 63, 65));
        UIManager.put("Button.foreground", new ColorUIResource(187, 187, 187));
        UIManager.put("Label.foreground", new ColorUIResource(255, 255, 255));
        UIManager.put("Panel.background", new ColorUIResource(43, 43, 43));

        // Set up map configuration
        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(8); // Increase thread pool for better performance
        mapViewer.setTileFactory(tileFactory);

        // Set initial view
        mapViewer.setZoom(6);
        mapViewer.setAddressLocation(new GeoPosition(40.4093, 49.8671)); // Baku center

        // Create a panel for controls
        JPanel controlPanel = new JPanel();
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        clearButton = new JButton("Temizle");
        helpButton = new JButton("Yardım");
        statusLabel = new JLabel("Başlangıç noktasını seçmek için haritaya tıklayın");

        // Add hover effect to buttons
        addHoverEffect(clearButton);
        addHoverEffect(helpButton);

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetSelection();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame,
                        "Kullanım:\n" +
                                "1. Haritada bir noktaya tıklayarak başlangıç noktasını seçin\n" +
                                "2. Başka bir noktaya tıklayarak bitiş noktasını seçin\n" +
                                "3. Yol otomatik olarak hesaplanacaktır\n" +
                                "4. Yeni bir yol seçmek için 'Temizle' düğmesine basın\n" +
                                "- Fare tekerleğiyle yakınlaştırabilir/uzaklaştırabilirsiniz\n" +
                                "- Haritayı sürükleyerek hareket ettirebilirsiniz",
                        "Yardım", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        controlPanel.add(clearButton);
        controlPanel.add(helpButton);
        controlPanel.add(statusLabel);

        // Main panel layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(mapViewer, BorderLayout.CENTER);

        // Create a panel for recommendations
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
        recommendationPanel.setVisible(false); // Initially hidden

        mainPanel.add(recommendationPanel, BorderLayout.SOUTH);

        // Enable map navigation
        setupMapNavigation();

        // Setup mouse click handler for selecting points
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    handleMapClick(e.getPoint());
                }
            }
        });

        createMockGraph();
        displayNodesOnMap();

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void setupMapNavigation() {
        // Add interactions for panning and zooming
        MouseAdapter panMouseAdapter = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(panMouseAdapter);
        mapViewer.addMouseMotionListener(panMouseAdapter);

        // Add mouse wheel zoom
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));
    }

    private void handleMapClick(Point point) {
        GeoPosition clickedPosition = mapViewer.convertPointToGeoPosition(point);

        if (clickedPosition == null) {
            JOptionPane.showMessageDialog(null, "Geçersiz tıklama. Lütfen haritada bir noktaya tıklayın.",
                    "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Node existingNode = findNearestNode(clickedPosition, 0.005); // ~500m tolerance

        if (clickCount == 0) {
            if (existingNode != null) {
                startNode = existingNode;
            } else {
                startNode = new Node("UserStart", clickedPosition.getLatitude(), clickedPosition.getLongitude());
                addNodeToGraph(startNode);
            }

            statusLabel.setText("Başlangıç noktası seçildi. Şimdi bitiş noktasını seçin.");
            highlightNode(startNode, Color.GREEN);
            clickCount++;

        } else if (clickCount == 1) {
            if (existingNode != null) {
                endNode = existingNode;
            } else {
                endNode = new Node("UserEnd", clickedPosition.getLatitude(), clickedPosition.getLongitude());
                addNodeToGraph(endNode);
            }

            if (startNode.equals(endNode)) {
                JOptionPane.showMessageDialog(null, "Başlangıç ve bitiş noktaları aynı olamaz.",
                        "Hata", JOptionPane.WARNING_MESSAGE);
                return;
            }

            statusLabel.setText("Bitiş noktası seçildi. Rota hesaplanıyor...");
            highlightNode(endNode, Color.RED);

            findAndDrawRoute();
            clickCount = 2;
        } else {
            JOptionPane.showMessageDialog(null, "Yeni bir rota seçmek için önce 'Temizle' düğmesine basın.",
                    "Bilgi", JOptionPane.INFORMATION_MESSAGE);
        }
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

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Simple Euclidean distance - not accurate for geographic coordinates
        return Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2));
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // Convert latitude to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance in kilometers
        return EARTH_RADIUS_KM * c;
    }

    private void addNodeToGraph(Node newNode) {
        nodes.add(newNode);

        // Connect to nearby nodes
        List<Edge> edges = new ArrayList<>();
        for (Node existingNode : nodes) {
            if (existingNode != newNode) {
                double distance = calculateHaversineDistance(
                        newNode.getPosition().getLatitude(), newNode.getPosition().getLongitude(),
                        existingNode.getPosition().getLatitude(), existingNode.getPosition().getLongitude()
                );

                if (distance < 2.0) { // ~2km connection distance
                    // Add bidirectional edges with real distance in kilometers
                    edges.add(new Edge(newNode, existingNode, distance));

                    // Add reverse edge to existing node
                    List<Edge> existingEdges = graph.getOrDefault(existingNode, new ArrayList<>());
                    existingEdges.add(new Edge(existingNode, newNode, distance));
                    graph.put(existingNode, existingEdges);
                }
            }
        }

        graph.put(newNode, edges);
        displayNodesOnMap();
    }

    private void highlightNode(Node node, Color color) {
        Set<Waypoint> waypoints = new HashSet<>();
        waypoints.add(new DefaultWaypoint(node.getPosition()));

        WaypointPainter<Waypoint> painter = new WaypointPainter<>();
        painter.setWaypoints(waypoints);

        // Create a custom renderer for the waypoint
        painter.setRenderer(new WaypointRenderer<Waypoint>() {
            @Override
            public void paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint waypoint) {
                Point2D point = map.getTileFactory().geoToPixel(
                        waypoint.getPosition(), map.getZoom());

                g.setColor(color);
                g.fillOval((int) point.getX() - 10, (int) point.getY() - 10, 20, 20);
                g.setColor(Color.BLACK);
                g.drawOval((int) point.getX() - 10, (int) point.getY() - 10, 20, 20);
            }
        });

        nodePainters.add(painter);
        updateMapPainters();
    }

    private void resetSelection() {
        startNode = null;
        endNode = null;
        selectedRoute.clear();
        clickCount = 0;
        nodePainters.clear();
        routePainter = null;
        recommendationPanel.setVisible(false);
        statusLabel.setText("Başlangıç noktasını seçmek için haritaya tıklayın");
        updateMapPainters();
    }

    private void createMockGraph() {
        try {
            // Load JSON data
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> stopsAndStations = objectMapper.readValue(
                    getClass().getResourceAsStream("/StopsAndStations.json"),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            // Create nodes
            Map<Integer, Node> nodeMap = new HashMap<>();
            for (Map<String, Object> station : stopsAndStations) {
                int id = (int) station.get("id");
                String name = (String) station.get("name");
                double latitude = (double) station.get("latitude");
                double longitude = (double) station.get("longitude");

                Node node = new Node(name, latitude, longitude);
                nodes.add(node);
                nodeMap.put(id, node);
            }

            // Create edges using Haversine distance
            for (int i = 0; i < nodes.size() - 1; i++) {
                Node current = nodes.get(i);

                // Connect to nearby nodes within a reasonable distance
                for (int j = 0; j < nodes.size(); j++) {
                    if (i != j) {
                        Node other = nodes.get(j);
                        double distance = calculateHaversineDistance(
                                current.getPosition().getLatitude(), current.getPosition().getLongitude(),
                                other.getPosition().getLatitude(), other.getPosition().getLongitude()
                        );

                        // Only connect nodes that are within 2km of each other
                        if (distance < 2.0) {
                            // Add bidirectional edges with real distance in kilometers
                            graph.computeIfAbsent(current, k -> new ArrayList<>())
                                    .add(new Edge(current, other, distance));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "JSON verileri yüklenirken bir hata oluştu.",
                    "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayNodesOnMap() {
        Set<Waypoint> waypoints = new HashSet<>();

        for (Node node : nodes) {
            waypoints.add(new DefaultWaypoint(node.getPosition()));
        }

        WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(waypoints);

        // Create a custom renderer for the waypoints
        waypointPainter.setRenderer(new WaypointRenderer<Waypoint>() {
            @Override
            public void paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint waypoint) {
                Point2D point = map.getTileFactory().geoToPixel(
                        waypoint.getPosition(), map.getZoom());

                // Find the node associated with this waypoint
                Node node = nodes.stream()
                        .filter(n -> n.getPosition().equals(waypoint.getPosition()))
                        .findFirst()
                        .orElse(null);

                if (node != null) {
                    // Draw a circle for the node
                    g.setColor(Color.BLUE);
                    g.fillOval((int) point.getX() - 5, (int) point.getY() - 5, 10, 10);
                    g.setColor(Color.BLACK);
                    g.drawOval((int) point.getX() - 5, (int) point.getY() - 5, 10, 10);

                    // Draw the node name
                    g.setColor(Color.BLACK);
                    g.drawString(node.getId(), (int) point.getX() + 8, (int) point.getY() + 4);
                }
            }
        });

        // Add the waypoint painter to the map
        List<Painter<JXMapViewer>> painters = new ArrayList<>();
        painters.add(waypointPainter);

        // Add edges
        for (Node node : graph.keySet()) {
            List<Edge> edges = graph.get(node);
            for (Edge edge : edges) {
                List<GeoPosition> edgePath = new ArrayList<>();
                edgePath.add(edge.getFrom().getPosition());
                edgePath.add(edge.getTo().getPosition());

                RoutePainter edgePainter = new RoutePainter(edgePath, Color.GRAY);
                painters.add(edgePainter);
            }
        }

        // Add previously created painters
        painters.addAll(nodePainters);
        if (routePainter != null) {
            painters.add(routePainter);
        }

        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(painter);
    }

    private void updateMapPainters() {
        List<Painter<JXMapViewer>> painters = new ArrayList<>();

        // Add waypoint painter for all nodes
        Set<Waypoint> waypoints = new HashSet<>();
        for (Node node : nodes) {
            waypoints.add(new DefaultWaypoint(node.getPosition()));
        }

        WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(waypoints);
        waypointPainter.setRenderer(new WaypointRenderer<Waypoint>() {
            @Override
            public void paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint waypoint) {
                Point2D point = map.getTileFactory().geoToPixel(
                        waypoint.getPosition(), map.getZoom());

                Node node = nodes.stream()
                        .filter(n -> n.getPosition().equals(waypoint.getPosition()))
                        .findFirst()
                        .orElse(null);

                if (node != null) {
                    g.setColor(Color.BLUE);
                    g.fillOval((int) point.getX() - 5, (int) point.getY() - 5, 10, 10);
                    g.setColor(Color.BLACK);
                    g.drawOval((int) point.getX() - 5, (int) point.getY() - 5, 10, 10);
                    g.drawString(node.getId(), (int) point.getX() + 8, (int) point.getY() + 4);
                }
            }
        });
        painters.add(waypointPainter);

        // Add edges
        for (Node node : graph.keySet()) {
            List<Edge> edges = graph.get(node);
            for (Edge edge : edges) {
                List<GeoPosition> edgePath = new ArrayList<>();
                edgePath.add(edge.getFrom().getPosition());
                edgePath.add(edge.getTo().getPosition());

                RoutePainter edgePainter = new RoutePainter(edgePath, Color.GRAY);
                painters.add(edgePainter);
            }
        }

        // Add highlight painters
        painters.addAll(nodePainters);
        if (routePainter != null) {
            painters.add(routePainter);
        }

        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>(painters);
        mapViewer.setOverlayPainter(painter);
    }

    private void findAndDrawRoute() {
        if (startNode == null || endNode == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "Başlangıç veya bitiş noktası seçilmedi.",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        Dijkstra dijkstra = new Dijkstra(graph);
        List<List<Node>> steps = dijkstra.findShortestPath(startNode, endNode);

        if (steps.isEmpty() || steps.get(steps.size() - 1).isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Bu noktalar arasında rota bulunamadı. Lütfen başka noktalar seçin.",
                    "Rota Bulunamadı",
                    JOptionPane.WARNING_MESSAGE
            );
            statusLabel.setText("Rota bulunamadı. Lütfen tekrar deneyin.");
            return;
        }

        List<Node> shortestPath = steps.get(steps.size() - 1);
        selectedRoute = shortestPath.stream()
                .map(Node::getPosition)
                .collect(Collectors.toList());

        // Display blue route first (background)
        RoutePainter blueRoutePainter = new RoutePainter(selectedRoute, Color.BLUE);
        mapViewer.setOverlayPainter(blueRoutePainter);

        // Setup animated route painter with red color
        routePainter = new RoutePainter(selectedRoute, Color.RED);
        animateRouteFadeIn();

        // Calculate actual path distance with proper Haversine formula
        double totalDistance = calculatePathDistance(shortestPath);
        String distanceString = String.format("Rota bulundu! Toplam mesafe: %.2f km", totalDistance);

        // Zoom to fit the route
        zoomToFitRoute(selectedRoute);

        // Display route information
        statusLabel.setText(distanceString);

        // Display recommended buses or metros
        displayRecommendations(shortestPath);

        // Create map animation controller
        MapAnimation mapAnimation = new MapAnimation(
                mapViewer,
                steps,
                selectedRoute,
                new ArrayList<>(nodePainters)
        );

        JPanel animationPanel = mapAnimation.getControlPanel();
        animationPanel.setVisible(true);
    }

    private void zoomToFitRoute(List<GeoPosition> route) {
        if (route == null || route.isEmpty()) return;

        // Find bounds of the route
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (GeoPosition pos : route) {
            minLat = Math.min(minLat, pos.getLatitude());
            maxLat = Math.max(maxLat, pos.getLatitude());
            minLon = Math.min(minLon, pos.getLongitude());
            maxLon = Math.max(maxLon, pos.getLongitude());
        }

        // Add some padding
        double latPadding = (maxLat - minLat) * 0.2;
        double lonPadding = (maxLon - minLon) * 0.2;

        // Create rectangle with the route bounds
        Rectangle2D rect = new Rectangle2D.Double(
                minLon - lonPadding,
                minLat - latPadding,
                (maxLon - minLon) + 2 * lonPadding,
                (maxLat - minLat) + 2 * latPadding
        );

        // Calculate appropriate zoom level
        int zoom = mapViewer.getZoom();
        mapViewer.setZoom(zoom);

        // Center the map on the middle of the route
        GeoPosition center = new GeoPosition(
                (minLat + maxLat) / 2,
                (minLon + maxLon) / 2
        );
        mapViewer.setAddressLocation(center);
    }

    private void animateRouteFadeIn() {
        Timer timer = new Timer(50, null);
        final float[] alpha = {0.0f};
        timer.addActionListener(e -> {
            alpha[0] += 0.1f;
            if (alpha[0] >= 1.0f) {
                alpha[0] = 1.0f;
                timer.stop();
            }
            routePainter.setAlpha(alpha[0]);
            updateMapPainters();
        });
        timer.start();
    }

    private double calculatePathDistance(List<Node> path) {
        double distance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Node current = path.get(i);
            Node next = path.get(i + 1);

            // Find the edge between these nodes
            Optional<Edge> edge = graph.get(current).stream()
                    .filter(e -> e.getTo().equals(next))
                    .findFirst();

            if (edge.isPresent()) {
                distance += edge.get().getWeight();
            } else {
                // Fallback in case the edge isn't found
                distance += calculateHaversineDistance(
                        current.getPosition().getLatitude(),
                        current.getPosition().getLongitude(),
                        next.getPosition().getLatitude(),
                        next.getPosition().getLongitude()
                );
            }
        }
        return distance;
    }

    // MapViewer sınıfının içinde, eski displayRecommendations yerine aşağıyı koy:
    private void displayRecommendations(List<Node> path) {
        if (path == null || path.isEmpty()) {
            recommendationPanel.setVisible(false);
            return;
        }

        List<Segment> segments = loadRouteSegments(); // JSON’dan segmentleri yükle
        StringBuilder sb = new StringBuilder("Önerilen ulaşım araçları:\n");

        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i).getId();
            String to   = path.get(i + 1).getId();

            // from → to arası segmenti bul
            Segment match = segments.stream()
                    .filter(s -> s.from.equals(from) && s.to.equals(to))
                    .findFirst()
                    .orElse(null);

            if (match != null) {
                sb.append("- ")
                        .append(match.tip.toUpperCase())
                        .append(match.hat != null ? " (Hat " + match.hat + ")" : "")
                        .append(": ")
                        .append(match.aciklama)
                        .append("\n");
            } else {
                sb.append("- Bilinmeyen ulaşım: ")
                        .append(from).append(" → ").append(to)
                        .append("\n");
            }
        }

        recommendationTextArea.setText(sb.toString());
        recommendationPanel.setVisible(true);
    }



    private void addHoverEffect(JButton button) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(75, 110, 175));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(60, 63, 65));
            }
        });
    }
    private List<Segment> loadRouteSegments() {
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("Transports.json")) {
            if (is == null) {
                throw new RuntimeException("Transports.json bulunamadı!");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode arr  = root.get("segments");

            return mapper.convertValue(arr, new TypeReference<List<Segment>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Transports.json okunamadı!", e);
        }
    }

}
