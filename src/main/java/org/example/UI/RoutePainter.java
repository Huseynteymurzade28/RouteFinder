package org.example.UI;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.stream.Collectors;

public class RoutePainter implements Painter<JXMapViewer> {
    private final List<GeoPosition> track;
    private final Color color;
    private float alpha = 1.0f; // Default opacity

    public RoutePainter(List<GeoPosition> track, Color color) {
        this.track = track;
        this.color = color;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        g = (Graphics2D) g.create();

        if (track == null || track.isEmpty()) {
            return;
        }

        // Convert geo positions to screen positions
        List<Point2D> points = track.stream()
                .map(geoPosition -> map.getTileFactory().geoToPixel(geoPosition, map.getZoom()))
                .collect(Collectors.toList());

        // Set rendering hints for smoother lines
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(4));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));

        // Draw the route
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D p1 = points.get(i);
            Point2D p2 = points.get(i + 1);
            g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
        }

        g.dispose();
    }
}
