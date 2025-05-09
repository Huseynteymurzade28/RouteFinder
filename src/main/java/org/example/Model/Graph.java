package org.example.Model;

import org.jxmapviewer.viewer.GeoPosition;

import java.util.*;

public class Graph {
    private final Map<Node, List<Edge>> adjacencyMap = new HashMap<>();

    public void addNode(Node node) {
        adjacencyMap.putIfAbsent(node, new ArrayList<>());
    }

    public void addEdge(Node from, Node to, double weight) {
        adjacencyMap.get(from).add(new Edge(from, to, weight));
    }

    public List<Edge> getNeighbors(Node node) {
        return adjacencyMap.getOrDefault(node, new ArrayList<>());
    }

    public Set<Node> getNodes() {
        return adjacencyMap.keySet();
    }
}