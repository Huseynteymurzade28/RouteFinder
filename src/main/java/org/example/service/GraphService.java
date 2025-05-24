package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Model.Edge;
import org.example.Model.Node; // Assuming this is your existing Node model
import org.example.Model.Dijkstra; // Added import
import org.example.dto.GeoPositionDTO;
import org.example.dto.NodeDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GraphService {

    private final List<Node> nodes = new ArrayList<>();
    private final Map<Node, List<Edge>> graph = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final static double EARTH_RADIUS_KM = 6371.0;

    @PostConstruct
    private void initializeGraph() throws IOException {
        loadNodes();
        loadEdges();
    }

    private void loadNodes() throws IOException {
        try (InputStream stopsStream = new ClassPathResource("/StopsAndStations.json").getInputStream()) {
            if (stopsStream == null) {
                throw new RuntimeException("StopsAndStations.json not found in resources");
            }
            List<Map<String, Object>> stopsAndStations = objectMapper.readValue(
                    stopsStream, new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> station : stopsAndStations) {
                String name = (String) station.get("name");
                double latitude = ((Number) station.get("latitude")).doubleValue();
                double longitude = ((Number) station.get("longitude")).doubleValue();
                Node node = new Node(name, latitude, longitude);
                this.nodes.add(node);
                this.graph.put(node, new ArrayList<>());
            }
        }
    }

    private void loadEdges() throws IOException {
        Map<String, Node> nodeMapByName = new HashMap<>();
        for (Node node : this.nodes) {
            nodeMapByName.put(node.getId(), node);
        }

        try (InputStream transportsStream = new ClassPathResource("/Transports.json").getInputStream()) {
            if (transportsStream == null) {
                throw new RuntimeException("Transports.json not found in resources");
            }
            JsonNode root = objectMapper.readTree(transportsStream);
            JsonNode segmentsNode = root.get("segments");
            if (segmentsNode != null && segmentsNode.isArray()) {
                for (JsonNode segmentNode : segmentsNode) {
                    String fromName = segmentNode.get("from").asText();
                    String toName = segmentNode.get("to").asText();
                    Node fromNode = nodeMapByName.get(fromName);
                    Node toNode = nodeMapByName.get(toName);

                    if (fromNode != null && toNode != null) {
                        double distance = calculateHaversineDistance(
                                fromNode.getPosition().getLatitude(), fromNode.getPosition().getLongitude(),
                                toNode.getPosition().getLatitude(), toNode.getPosition().getLongitude()
                        );
                        this.graph.get(fromNode).add(new Edge(fromNode, toNode, distance));
                        this.graph.get(toNode).add(new Edge(toNode, fromNode, distance)); // Assuming bidirectional
                    }
                }
            }
        }
    }

    public List<NodeDTO> getAllNodes() {
        return nodes.stream()
                .map(this::convertToNodeDTO)
                .collect(Collectors.toList());
    }
    
    public Map<Node, List<Edge>> getGraph() {
        return graph;
    }

    public List<Node> getInternalNodes() {
        return nodes;
    }

    public List<NodeDTO> findShortestPath(String startNodeId, String endNodeId) {
        Node startNode = nodes.stream().filter(node -> node.getId().equals(startNodeId)).findFirst().orElse(null);
        Node endNode = nodes.stream().filter(node -> node.getId().equals(endNodeId)).findFirst().orElse(null);

        if (startNode == null || endNode == null) {
            return new ArrayList<>();
        }

        Dijkstra dijkstra = new Dijkstra(this.graph);
        List<List<Node>> paths = dijkstra.findShortestPath(startNode, endNode);

        if (paths.isEmpty() || paths.get(paths.size() - 1).isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Node> shortestPath = paths.get(paths.size() - 1);

        return shortestPath.stream()
                .map(this::convertToNodeDTO)
                .collect(Collectors.toList());
    }

    private NodeDTO convertToNodeDTO(Node node) {
        GeoPositionDTO geoPositionDTO = new GeoPositionDTO(
                node.getPosition().getLatitude(),
                node.getPosition().getLongitude()
        );
        return new NodeDTO(node.getId(), geoPositionDTO);
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
}
