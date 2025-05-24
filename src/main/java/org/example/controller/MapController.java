package org.example.controller;

import java.util.List;

import org.example.dto.NodeDTO;
import org.example.dto.RouteSegmentDTO; // Added import
import org.example.service.GraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private final GraphService graphService;

    @Autowired
    public MapController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/nodes")
    public List<NodeDTO> getAllNodes() {
        return graphService.getAllNodes();
    }

    @GetMapping("/route")
    public List<RouteSegmentDTO> getRoute(@RequestParam String startNodeId, @RequestParam String endNodeId) {
        return graphService.findShortestPath(startNodeId, endNodeId);
    }
}
