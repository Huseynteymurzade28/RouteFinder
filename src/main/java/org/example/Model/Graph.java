package org.example.Model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {
    private final Map<Node, List<Edge>> adjacencyMap = new HashMap<>();

    public void addNode(Node node) {
        adjacencyMap.putIfAbsent(node, new ArrayList<>());
    }

    // Modified to accept Edge object directly, or adjust parameters as needed
    public void addEdge(Edge edge) { 
        // Ensure both nodes are in the graph before adding an edge
        addNode(edge.getFrom());
        addNode(edge.getTo());
        adjacencyMap.get(edge.getFrom()).add(edge);
    }

    public List<Edge> getNeighbors(Node node) {
        return adjacencyMap.getOrDefault(node, new ArrayList<>());
    }

    public Set<Node> getNodes() {
        return adjacencyMap.keySet();
    }
}