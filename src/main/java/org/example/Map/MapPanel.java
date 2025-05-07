package org.example.Map;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;

import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;

import javax.swing.event.MouseInputListener;

import javax.swing.*;
import java.awt.*;

public class MapPanel extends JPanel {
    public MapPanel() {
        setLayout(new BorderLayout());

        OSMTileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);

        JXMapViewer mapViewer = new JXMapViewer();
        mapViewer.setTileFactory(tileFactory);

        GeoPosition Baku = new GeoPosition(40.43495040, 49.86762320);
        mapViewer.setZoom(4);
        mapViewer.setAddressLocation(Baku);
        mapViewer.addMouseListener(new CenterMapListener(mapViewer));

// Sürükleyerek gezinebilme
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);

// Fare tekerleği ile yakınlaştırma
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        add(mapViewer, BorderLayout.CENTER);
    }
}
