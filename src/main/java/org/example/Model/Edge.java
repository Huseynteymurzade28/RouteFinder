package org.example.Model;

public class Edge {
    private final Node from;
    private final Node to;
    private final double weight; // Could represent distance or a combination of factors
    private String transportType; // e.g., "bus", "metro", "train", "walking"
    private double time; // e.g., in minutes

    public Edge(Node from, Node to, double weight, String transportType, double time) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.transportType = transportType;
        this.time = time;
    }

    public Node getFrom() {
        return from;
    }

    public Node getTo() {
        return to;
    }

    public double getWeight() {
        return weight;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }
}
