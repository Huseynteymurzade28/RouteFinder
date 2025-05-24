package org.example.dto;

public class NodeDTO {
    private String id;
    private GeoPositionDTO position;
    private String type; // Added type for bus, metro, train, etc.

    public NodeDTO(String id, GeoPositionDTO position, String type) {
        this.id = id;
        this.position = position;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
