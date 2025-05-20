package org.example.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Dijkstra {
    private final Map<Node, List<Edge>> originalGraph;

    public Dijkstra(Map<Node, List<Edge>> graph) {
        this.originalGraph = graph;
    }

    public List<List<Node>> findShortestPath(Node start, Node end) {
        // Work on a copy of the graph to avoid modifying the original
        Map<Node, List<Edge>> graph = new HashMap<>(this.originalGraph);

        // Temporarily add start/end nodes to the graph copy if they don't exist.
        if (start != null && !graph.containsKey(start)) {
            graph.put(start, new ArrayList<>());
        }
        if (end != null && !graph.containsKey(end)) {
            graph.put(end, new ArrayList<>());
        }

        Map<Node, Double> dist = new HashMap<>();
        Map<Node, Node> prev = new HashMap<>();
        // PriorityQueue stores nodes to visit, ordered by their current shortest distance from start
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        // Set to keep track of nodes whose shortest path has been finalized (visited)
        Set<Node> visitedFinal = new HashSet<>();
        // List to store snapshots of visited nodes during exploration (for animation/debugging)
        List<List<Node>> steps = new ArrayList<>();

        // Initialize distances for all nodes in our working graph copy
        for (Node node : graph.keySet()) {
            dist.put(node, Double.POSITIVE_INFINITY);
        }

        if (start == null || end == null) {
            steps.add(new ArrayList<>()); // Add an empty path if start or end is null
            return steps;
        }

        dist.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (visitedFinal.contains(current)) {
                continue;
            }
            visitedFinal.add(current);

            List<Node> currentExplorationStep = new ArrayList<>(visitedFinal);
            steps.add(currentExplorationStep);

            if (current.equals(end)) {
                break;
            }

            List<Edge> neighbors = graph.getOrDefault(current, Collections.emptyList());
            for (Edge edge : neighbors) {
                Node neighborNode = edge.getTo();
                if (visitedFinal.contains(neighborNode)) {
                    continue;
                }

                double newDist = dist.get(current) + edge.getWeight();
                // Ensure neighborNode is in dist map, which it should be if graph keys were used for init
                if (newDist < dist.getOrDefault(neighborNode, Double.POSITIVE_INFINITY)) {
                    dist.put(neighborNode, newDist);
                    prev.put(neighborNode, current);
                    queue.add(neighborNode);
                }
            }
        }

        LinkedList<Node> path = new LinkedList<>();
        // Check if 'end' node was reached and is present in the distance map
        if (dist.containsKey(end) && dist.get(end) != Double.POSITIVE_INFINITY) {
            Node crawl = end;
            while (crawl != null) {
                path.addFirst(crawl);
                if (crawl.equals(start)) {
                    break; // Path successfully reconstructed
                }
                crawl = prev.get(crawl); // Move to the previous node in the path
            }
            // If path is not empty and doesn't start with 'start', it means 'start' was not reached from 'end' via 'prev' map.
            // This indicates no valid path was found.
            if (!path.isEmpty() && !path.getFirst().equals(start)) {
                path.clear(); // Invalid path
            }
        }
        // Special case: if start and end are the same, and the node exists in the graph.
        // And no path was found above (e.g. isolated node).
        if (start.equals(end) && graph.containsKey(start) && path.isEmpty()) {
            path.addFirst(start);
        }

        steps.add(new ArrayList<>(path)); // Add the final reconstructed path

        return steps;
    }
}
