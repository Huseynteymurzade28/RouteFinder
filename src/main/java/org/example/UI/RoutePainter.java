package org.example.UI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.stream.Collectors;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

public class RoutePainter implements Painter<JXMapViewer> {
    private final List<GeoPosition> track;
    private float alpha = 1.0f; // Default opacity
    private float strokeWidth = 4.0f; // Default stroke width

    public RoutePainter(List<GeoPosition> track) {
        this.track = track;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create(); // Create a copy of the graphics context to avoid side effects

        try {
            if (this.track == null || this.track.isEmpty()) {
                return; // Early exit if no track to draw
            }

            // Convert geo positions to screen positions
            List<Point2D> points = this.track.stream()
                    .map(geoPosition -> map.getTileFactory().geoToPixel(geoPosition, map.getZoom()))
                    .collect(Collectors.toList());

            if (points.isEmpty()) { // Should ideally not happen if track is not empty, but a safe check
                return;
            }

            // Set rendering hints for smoother lines and shapes
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Define YellowGreen color
            Color yellowGreen = new Color(154, 205, 50); // RGB for YellowGreen

            // Color for the route line, incorporating alpha for fade-in effect
            Color routeLineColorWithAlpha = new Color(yellowGreen.getRed(), yellowGreen.getGreen(), yellowGreen.getBlue(), (int) (this.alpha * 255));
            
            // Draw the route lines
            g2.setColor(routeLineColorWithAlpha);
            g2.setStroke(new BasicStroke(this.strokeWidth)); 
            for (int i = 0; i < points.size() - 1; i++) {
                Point2D p1 = points.get(i);
                Point2D p2 = points.get(i + 1);
                g2.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
            }

            // Draw a small circle at each point of the route for better visibility
            // Circles will be slightly larger than the line width and in a contrasting color.
            int pointDiameter = Math.max(4, (int) this.strokeWidth + 2); // e.g., if strokeWidth=5, diameter=7. Minimum diameter 4.
            
            // Color for the circles, incorporating alpha
            // Using the same yellowGreen for the circles
            Color circleColorWithAlpha = new Color(yellowGreen.getRed(), yellowGreen.getGreen(), yellowGreen.getBlue(), (int) (this.alpha * 255));
            g2.setColor(circleColorWithAlpha);
            
            // No special stroke needed for fillOval, it fills the shape.
            for (Point2D p : points) {
                g2.fillOval((int) p.getX() - pointDiameter / 2, (int) p.getY() - pointDiameter / 2, pointDiameter, pointDiameter);
            }
        } finally {
            g2.dispose(); // Always dispose of the copied graphics context to free resources
        }
    }
}
