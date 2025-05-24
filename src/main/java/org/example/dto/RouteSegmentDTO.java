package org.example.dto;

public class RouteSegmentDTO {
    private NodeDTO fromNode;
    private NodeDTO toNode;
    private String transportType;
    private double time; // in minutes
    private double distance; // in km

    // Constructors
    public RouteSegmentDTO() {
    }

    public RouteSegmentDTO(NodeDTO fromNode, NodeDTO toNode, String transportType, double time, double distance) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.transportType = transportType;
        this.time = time;
        this.distance = distance;
    }

    // Getters and Setters
    public NodeDTO getFromNode() {
        return fromNode;
    }

    public void setFromNode(NodeDTO fromNode) {
        this.fromNode = fromNode;
    }

    public NodeDTO getToNode() {
        return toNode;
    }

    public void setToNode(NodeDTO toNode) {
        this.toNode = toNode;
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

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
