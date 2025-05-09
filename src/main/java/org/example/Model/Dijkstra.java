package org.example.Model;

import java.util.*;

public class Dijkstra {
    private final Map<Node, List<Edge>> graph;

    public Dijkstra(Map<Node, List<Edge>> graph) {
        this.graph = graph;
    }

    public List<List<Node>> findShortestPath(Node start, Node end) {
        if (!graph.containsKey(start) || !graph.containsKey(end)) {
            return Collections.emptyList();
        }

        Map<Node, Double> dist = new HashMap<>();
        Map<Node, Node> prev = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        List<List<Node>> steps = new ArrayList<>();

        for (Node node : graph.keySet()) {
            dist.put(node, Double.POSITIVE_INFINITY);
        }
        dist.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            List<Node> stepSnapshot = new ArrayList<>(visited);
            steps.add(stepSnapshot);

            if (current.equals(end)) break;

            for (Edge edge : graph.getOrDefault(current, new ArrayList<>())) {
                Node neighbor = edge.getTo();
                double newDist = dist.get(current) + edge.getWeight();
                if (newDist < dist.get(neighbor)) {
                    dist.put(neighbor, newDist);
                    prev.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        List<Node> path = new ArrayList<>();
        Node current = end;
        while (current != null && prev.containsKey(current)) {
            path.add(0, current);
            current = prev.get(current);
        }
        if (current == start) path.add(0, start);
        steps.add(path);

        return steps;
    }
}
