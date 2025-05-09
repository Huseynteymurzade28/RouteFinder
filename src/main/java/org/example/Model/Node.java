package org.example.Model;

import org.jxmapviewer.viewer.GeoPosition;

public class Node {
    private final String id;
    private final GeoPosition position;

    public Node(String id, double lat, double lon) {
        this.id = id;
        this.position = new GeoPosition(lat, lon);
    }

    public String getId() {
        return id;
    }

    public GeoPosition getPosition() {
        return position;
    }
}
