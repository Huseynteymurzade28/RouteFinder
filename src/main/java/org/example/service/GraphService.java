package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Model.Edge;
import org.example.Model.Node; // Assuming this is your existing Node model
import org.example.Model.Dijkstra; // Added import
import org.example.dto.GeoPositionDTO;
import org.example.dto.NodeDTO;
import org.example.dto.RouteSegmentDTO; // Added import
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
                String type = (String) station.get("type"); // Read station type
                Node node = new Node(name, latitude, longitude, type); // Pass type to constructor
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
                    String transportTypeJson = segmentNode.get("tip").asText();
                    double time = segmentNode.get("sure_dk").asDouble();

                    Node fromNode = nodeMapByName.get(fromName);
                    Node toNode = nodeMapByName.get(toName);

                    String transportType;
                    switch (transportTypeJson.toLowerCase()) {
                        case "yurume":
                            transportType = "walking";
                            break;
                        case "otobus":
                            transportType = "bus";
                            break;
                        case "metro":
                            transportType = "metro";
                            break;
                        // Assuming "train" is not in current JSON, but can be added
                        case "train":
                            transportType = "train";
                            break;
                        case "taksi":
                             transportType = "taxi"; // Or handle as a special case if not part of public transport
                             break;
                        default:
                            transportType = "unknown"; // Or skip if type is not recognized
                            break;
                    }


                    if (fromNode != null && toNode != null) {
                        double distance = calculateHaversineDistance(
                                fromNode.getPosition().getLatitude(), fromNode.getPosition().getLongitude(),
                                toNode.getPosition().getLatitude(), toNode.getPosition().getLongitude()
                        );
                        // Add edge with transport type and time
                        this.graph.get(fromNode).add(new Edge(fromNode, toNode, distance, transportType, time));
                        // Assuming bidirectional for now, adjust if not all transport types are bidirectional
                        this.graph.get(toNode).add(new Edge(toNode, fromNode, distance, transportType, time));
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

    public List<RouteSegmentDTO> findShortestPath(String startNodeId, String endNodeId) {
        Node startNode = nodes.stream().filter(node -> node.getId().equals(startNodeId)).findFirst().orElse(null);
        Node endNode = nodes.stream().filter(node -> node.getId().equals(endNodeId)).findFirst().orElse(null);

        if (startNode == null || endNode == null) {
            return new ArrayList<>();
        }

        Dijkstra dijkstra = new Dijkstra(this.graph); // graph is Map<Node, List<Edge>>
        List<List<Node>> allSteps = dijkstra.findShortestPath(startNode, endNode);

        if (allSteps.isEmpty()) {
            return new ArrayList<>(); // No path found or error in Dijkstra
        }
        // Assuming Dijkstra.findShortestPath returns List<List<Node>> where the last list is the shortest path
        List<Node> shortestPathNodes = allSteps.get(allSteps.size() - 1); 

        if (shortestPathNodes.isEmpty() || shortestPathNodes.size() < 2) { // Path needs at least two nodes for a segment
            return new ArrayList<>();
        }

        List<RouteSegmentDTO> routeSegments = new ArrayList<>();
        for (int i = 0; i < shortestPathNodes.size() - 1; i++) {
            Node fromPathNode = shortestPathNodes.get(i);
            Node toPathNode = shortestPathNodes.get(i + 1);

            // Find the edge in the graph that connects fromPathNode to toPathNode
            Edge connectingEdge = graph.get(fromPathNode).stream()
                    .filter(edge -> edge.getTo().equals(toPathNode))
                    .findFirst()
                    .orElse(null); // Or throw an exception if an edge is expected but not found

            if (connectingEdge != null) {
                NodeDTO fromNodeDTO = convertToNodeDTO(fromPathNode);
                NodeDTO toNodeDTO = convertToNodeDTO(toPathNode);
                routeSegments.add(new RouteSegmentDTO(
                        fromNodeDTO,
                        toNodeDTO,
                        connectingEdge.getTransportType(),
                        connectingEdge.getTime(),
                        connectingEdge.getWeight() // Corrected to use getWeight()
                ));
            } else {
                // Handle case where no direct edge exists in the graph for a path segment from Dijkstra
                // This might indicate an issue with the graph data or Dijkstra's output
                // For now, we can skip this segment or log a warning
                System.err.println("Warning: No direct edge found between " + fromPathNode.getId() + " and " + toPathNode.getId());
            }
        }
        return routeSegments;
    }

    private NodeDTO convertToNodeDTO(Node node) {
        GeoPositionDTO geoPositionDTO = new GeoPositionDTO(
                node.getPosition().getLatitude(),
                node.getPosition().getLongitude()
        );
        return new NodeDTO(node.getId(), geoPositionDTO, node.getType()); // Include type in DTO
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
