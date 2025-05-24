package org.example.dto;

public class NodeDTO {
    private String id;
    private GeoPositionDTO position;

    public NodeDTO(String id, GeoPositionDTO position) {
        this.id = id;
        this.position = position;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GeoPositionDTO getPosition() {
        return position;
    }

    public void setPosition(GeoPositionDTO position) {
        this.position = position;
    }
}
