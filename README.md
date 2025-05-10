# RouteFinder

## Project Overview

Route Finder is a Java application that visualizes and calculates the shortest path between locations on a map using Dijkstra's algorithm. The application focuses on the city of Baku, Azerbaijan, and provides an interactive user interface for exploring routes between different points.


## Features

- Interactive OpenStreetMap-based visualization
- Click-to-select start and end points for route calculation
- Dijkstra's algorithm implementation for finding the shortest path
- Visual highlighting of the shortest route with a bold pink color
- Animated route display
- Distance calculation in kilometers
- Zoom and pan functionality
- Support for both predefined points of interest and custom user-selected locations

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven for dependency management

### Dependencies

- JXMapViewer2: Map visualization
- Jackson: JSON data processing
- Swing: UI components

### Installation

1. Clone the repository
```bash
git clone https://github.com/yourusername/route-finder.git
cd route-finder
```

2. Build the project with Maven
```bash
mvn clean install
```

3. Run the application
```bash
mvn exec:java -Dexec.mainClass="org.example.Main"
```

## Usage

1. **Launch the application**: The map will center on Baku, Azerbaijan
2. **Select starting point**: Click on any location to set it as the starting point (marked in green)
3. **Select destination**: Click on another location to set it as the destination (marked in red)
4. **View the route**: The application will automatically calculate and display the shortest path with a bold pink line
5. **Additional controls**:
    - Use mouse wheel to zoom in/out
    - Drag the map to pan
    - Click "Temizle" (Clear) to reset and select a new route
    - Click "Yardım" (Help) for usage instructions

## How It Works

### Map Visualization

The application uses JXMapViewer2 to render OpenStreetMap tiles and provide basic map navigation functionality. Custom painters are implemented to visualize nodes, edges, and routes.

### Data Structure

- **Nodes**: Represent points on the map (intersections, landmarks, etc.)
- **Edges**: Connections between nodes with weights representing distances
- **Graph**: A collection of nodes and edges representing the road network

```java
private final Map<Node, List<Edge>> graph = new HashMap<>();
private final List<Node> nodes = new ArrayList<>();
```

### Dijkstra's Algorithm Implementation

The application implements Dijkstra's algorithm for finding the shortest path between two points on the map. The algorithm works as follows:

1. Initialize distances of all nodes to infinity except the start node (distance = 0)
2. Create a priority queue and add all nodes with their distances
3. While the priority queue is not empty:
    - Extract the node with the minimum distance
    - For each adjacent node, update its distance if a shorter path is found
4. Reconstruct the path from the start node to the end node

Key components of the implementation:

```java
public class Dijkstra {
    private final Map<Node, List<Edge>> graph;

    public Dijkstra(Map<Node, List<Edge>> graph) {
        this.graph = graph;
    }

    public List<List<Node>> findShortestPath(Node start, Node end) {
        // Algorithm implementation
        // ...
    }
}
```

### Distance Calculation

The application uses the Haversine formula to calculate accurate distances between geographical coordinates:

```java
private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
    // Convert degrees to radians
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    // Convert latitude to radians
    lat1 = Math.toRadians(lat1);
    lat2 = Math.toRadians(lat2);

    // Haversine formula
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    // Distance in kilometers
    return EARTH_RADIUS_KM * c;
}
```

### Route Visualization

The shortest path is visually highlighted on the map using a custom `RoutePainter` class that draws a bold pink line connecting the nodes in the path. The visualization includes:

- Bold pink lines for the route segments
- Highlighted nodes along the path
- Distinct markers for start and end points
- Animation effects to make the route stand out

```java
public class RoutePainter implements Painter<JXMapViewer> {
    private final List<GeoPosition> route;
    private final Color color;
    private float strokeWidth = 4.0f;
    private float alpha = 1.0f;
    
    // Implementation details...
}
```

## Architecture

The application follows a Model-View-Controller (MVC) architecture:

- **Model**: The graph data structure and Dijkstra algorithm implementation (`Node`, `Edge`, `Dijkstra` classes)
- **View**: The map visualization and UI components (`MapViewer`, `RoutePainter` classes)
- **Controller**: User interaction handling and application logic

### Key Classes

- `MapViewer`: Main UI component managing the map display and user interactions
- `Node`: Represents a point on the map with coordinates
- `Edge`: Represents a connection between two nodes with a weight (distance)
- `Dijkstra`: Implementation of Dijkstra's algorithm for path finding
- `RoutePainter`: Custom painter for visualizing routes on the map
- `MapAnimation`: Handles animations for route display

## Customization

### Adding Custom Locations

The application loads location data from a JSON file. You can add your own locations by modifying the `StopsAndStations.json` file in the resources directory.

### Changing Visual Styles

You can customize the appearance of nodes, routes, and UI elements by modifying the relevant painter classes and UI settings in the code.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- OpenStreetMap for map data
- JXMapViewer2 for the mapping library
- All contributors who have helped improve this project