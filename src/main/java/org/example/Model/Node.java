package org.example.Model;

import org.jxmapviewer.viewer.GeoPosition;

public class Node {
    private final String id;
    private final GeoPosition position;
    private String type; // Added type for bus, metro, train, etc.

    public Node(String id, double lat, double lon, String type) {
        this.id = id;
        this.position = new GeoPosition(lat, lon);
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public GeoPosition getPosition() {
        return position;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
