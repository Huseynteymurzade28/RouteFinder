// Basic map initialization and API interaction will go here
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM fully loaded and parsed');

    const map = new ol.Map({
        target: 'map',
        layers: [
            new ol.layer.Tile({
                source: new ol.source.OSM()
            })
        ],
        view: new ol.View({
            center: ol.proj.fromLonLat([49.8671, 40.4093]), // Baku coordinates
            zoom: 12
        })
    });

    console.log('Map initialized');

    // Store precise click points and their nearest stations
    let startPoint = null; // { clickedCoordsLonLat: [lon, lat], nearestStationNode: NodeDTO }
    let endPoint = null;   // { clickedCoordsLonLat: [lon, lat], nearestStationNode: NodeDTO }
    let clickCount = 0;
    let allNodes = []; // Will be populated by fetchNodes

    const statusLabel = document.createElement('p');
    statusLabel.id = 'status-label';
    statusLabel.textContent = 'Select your precise starting point on the map.';
    const mapDiv = document.getElementById('map');
    mapDiv.parentNode.insertBefore(statusLabel, mapDiv.nextSibling);

    const infoPanel = document.getElementById('info-panel');
    const routeDetailsDiv = document.getElementById('route-details');

    const routeLayerSource = new ol.source.Vector();
    const routeLayer = new ol.layer.Vector({
        source: routeLayerSource,
        style: new ol.style.Style({
            stroke: new ol.style.Stroke({
                color: 'rgba(0, 123, 255, 0.8)', 
                width: 4 
            })
        })
    });
    map.addLayer(routeLayer);

    // Layer for precise click markers (start/end points chosen by user)
    const clickedPointsSource = new ol.source.Vector();
    const clickedPointsLayer = new ol.layer.Vector({
        source: clickedPointsSource,
        style: new ol.style.Style({
            image: new ol.style.Icon({
                anchor: [0.5, 1],
                src: 'https://openlayers.org/en/latest/examples/data/icon.png', // Using a standard OL icon
                scale: 0.8
            })
        })
    });
    map.addLayer(clickedPointsLayer);


    async function fetchNodes() {
        try {
            const response = await fetch('/api/map/nodes');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const nodes = await response.json();
            allNodes = nodes; 
            console.log('Nodes fetched:', allNodes);
            addStationMarkers(allNodes); 
        } catch (error) {
            console.error('Error fetching nodes:', error);
            if (infoPanel) {
                infoPanel.style.display = 'block';
                routeDetailsDiv.innerHTML = '<p style="color: red;">Error fetching map data. Please ensure the backend is running.</p>';
            }
        }
    }

    // Renamed to avoid confusion with clicked point markers
    function addStationMarkers(nodes) {
        const markerFeatures = nodes.map(node => {
            const feature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([node.position.longitude, node.position.latitude])),
                name: node.id,
                type: 'station' // Differentiate station markers
            });
            return feature;
        });

        const vectorSource = new ol.source.Vector({
            features: markerFeatures
        });

        const markerVectorLayer = new ol.layer.Vector({
            source: vectorSource,
            style: new ol.style.Style({
                image: new ol.style.Circle({
                    radius: 5, // Smaller radius for general stations
                    fill: new ol.style.Fill({color: 'rgba(0, 0, 255, 0.6)'}),
                    stroke: new ol.style.Stroke({color: 'white', width: 1})
                })
            })
        });
        map.addLayer(markerVectorLayer);
        console.log('Station markers added to map');
    }

    function findNearestNode(coordinate, nodesList) {
        let nearestNode = null;
        let minDistance = Infinity;
        const clickLonLat = ol.proj.toLonLat(coordinate); // Coordinate is in map projection
        const tolerance = 2000; // Increased tolerance to 2km, adjust as needed

        nodesList.forEach(node => {
            const nodeLonLat = [node.position.longitude, node.position.latitude];
            const distance = ol.sphere.getDistance(clickLonLat, nodeLonLat);

            if (distance < minDistance && distance < tolerance) {
                minDistance = distance;
                nearestNode = node;
            }
        });
        return nearestNode;
    }
    
    // No longer using selectedPointsSource/Layer for nearest stations, only for actual clicks.

    async function fetchAndDisplayRoute(currentStartPoint, currentEndPoint) {
        statusLabel.textContent = `Calculating route...`;
        routeDetailsDiv.innerHTML = `<p>Fetching route information...</p>`;
        infoPanel.style.display = 'block';
        routeLayerSource.clear(); 

        if (!currentStartPoint.nearestStationNode || !currentEndPoint.nearestStationNode) {
            // This case should ideally be caught before calling this function
            routeDetailsDiv.innerHTML = `<p style="color: red;">Error: Could not identify nearest stations for routing.</p>`;
            statusLabel.textContent = `Error in station identification.`;
            return;
        }

        try {
            // Backend route uses nearest station IDs
            const backendResponse = await fetch(`/api/map/route?startNodeId=${encodeURIComponent(currentStartPoint.nearestStationNode.id)}&endNodeId=${encodeURIComponent(currentEndPoint.nearestStationNode.id)}`);
            if (!backendResponse.ok) {
                const errorText = await backendResponse.text();
                throw new Error(`Backend error! status: ${backendResponse.status}, message: ${errorText || 'Failed to fetch transit route'}`);
            }
            const transitNodes = await backendResponse.json(); // These are the stations for the transit part
            console.log('Transit route from backend:', transitNodes);

            await drawDetailedStreetRoute(currentStartPoint, currentEndPoint, transitNodes);
            
            let routeInfoHtml = `<p><b>Route From:</b> Your selected start point</p>`;
            routeInfoHtml += `<p><b>Nearest Start Station:</b> ${currentStartPoint.nearestStationNode.id}</p>`;
            routeInfoHtml += `<p><b>To:</b> Your selected end point</p>`;
            routeInfoHtml += `<p><b>Nearest End Station:</b> ${currentEndPoint.nearestStationNode.id}</p>`;
            if (transitNodes && transitNodes.length > 0) {
                 routeInfoHtml += '<p><b>Transit Stations:</b> ' + transitNodes.map(node => node.id).join(' -> ') + '</p>';
            } else {
                 routeInfoHtml += '<p><em>No specific transit station path, or direct routing between nearest stations.</em></p>';
            }
            routeInfoHtml += '<p><em>Displaying street-level route. Note: Uses OSRM demo server.</em></p>';
            routeDetailsDiv.innerHTML = routeInfoHtml;
            statusLabel.textContent = `Street-level route displayed.`;

        } catch (error) {
            console.error('Error fetching or displaying route:', error);
            // Fallback: Try to route directly between clicked points if backend/transit part fails
            routeDetailsDiv.innerHTML = `<p style="color: orange;">Transit route failed: ${error.message}. Attempting direct street route between your points...</p>`;
            statusLabel.textContent = `Transit route failed. Trying direct...`;
            await drawDetailedStreetRoute(currentStartPoint, currentEndPoint, []); // Pass empty transitNodes for direct routing
        }
    }

    async function drawDetailedStreetRoute(actualStartPoint, actualEndPoint, transitNodes) {
        routeLayerSource.clear();
        const features = [];

        let osrmPoints = [];

        // 1. From actual start click to the first transit station (or nearest start station)
        const startClickCoords = `${actualStartPoint.clickedCoordsLonLat[0]},${actualStartPoint.clickedCoordsLonLat[1]}`;
        let firstTransitNodePos = actualStartPoint.nearestStationNode.position; // Default to nearest if no transitNodes
        if (transitNodes && transitNodes.length > 0) {
            firstTransitNodePos = transitNodes[0].position;
        }
        const firstLegToCoords = `${firstTransitNodePos.longitude},${firstTransitNodePos.latitude}`;
        osrmPoints.push(startClickCoords);
        osrmPoints.push(firstLegToCoords);


        // 2. Between transit stations (if any)
        if (transitNodes && transitNodes.length > 1) {
            for (let i = 0; i < transitNodes.length -1; i++) { // OSRM takes current and next, so add next
                 osrmPoints.push(`${transitNodes[i+1].position.longitude},${transitNodes[i+1].position.latitude}`);
            }
        }
        
        // 3. From last transit station (or nearest end station) to actual end click
        let lastTransitNodePos = actualEndPoint.nearestStationNode.position; // Default to nearest if no transitNodes
         if (transitNodes && transitNodes.length > 0) {
            lastTransitNodePos = transitNodes[transitNodes.length - 1].position;
        }
        // Ensure lastTransitNodePos is added if it's different from the last point in osrmPoints
        const lastOsrmPointStr = osrmPoints[osrmPoints.length -1];
        const lastTransitNodePosStr = `${lastTransitNodePos.longitude},${lastTransitNodePos.latitude}`;
        if (lastOsrmPointStr !== lastTransitNodePosStr) {
            osrmPoints.push(lastTransitNodePosStr);
        }

        const endClickCoords = `${actualEndPoint.clickedCoordsLonLat[0]},${actualEndPoint.clickedCoordsLonLat[1]}`;
        // Ensure endClickCoords is added if it's different from the last point in osrmPoints
        if (osrmPoints[osrmPoints.length -1] !== endClickCoords) {
             osrmPoints.push(endClickCoords);
        }
        
        // Remove duplicates that might occur if click is on a station
        osrmPoints = [...new Set(osrmPoints)];


        if (osrmPoints.length < 2) {
            console.warn("Not enough distinct points for OSRM routing.");
            routeDetailsDiv.innerHTML += '<p style="color: orange;">Could not form a valid path for street-level routing (points might be too close or identical).</p>'; // Corrected string
            return;
        }
        
        const osrmUrl = `http://router.project-osrm.org/route/v1/driving/${osrmPoints.join(';')}?overview=full&geometries=geojson`;
        console.log("OSRM URL:", osrmUrl);
        statusLabel.textContent = `Fetching detailed street path...`;

        try {
            const response = await fetch(osrmUrl);
            if (!response.ok) {
                throw new Error(`OSRM API error: ${response.status}. Cannot draw detailed path.`);
            }
            const data = await response.json();
            if (data.routes && data.routes.length > 0) {
                const geojsonFeature = new ol.format.GeoJSON().readFeature(data.routes[0].geometry, {
                    dataProjection: 'EPSG:4326',
                    featureProjection: map.getView().getProjection()
                });
                features.push(geojsonFeature);
                routeLayerSource.addFeatures(features);
                console.log('Detailed street route drawn on map.');
                statusLabel.textContent = `Street-level route displayed.`;
            } else {
                throw new Error('No route found by OSRM.');
            }
        } catch (error) {
            console.error('Error fetching or drawing OSRM main route:', error);
            routeDetailsDiv.innerHTML += `<p style="color: red;">Could not display detailed street map: ${error.message}</p>`;
            statusLabel.textContent = `Error displaying street map.`;
            // Fallback: draw straight lines between the osrmPoints if OSRM fails for the whole path
            const fallbackCoordsList = osrmPoints.map(pStr => {
                const [lon, lat] = pStr.split(',').map(Number);
                return ol.proj.fromLonLat([lon, lat]);
            });
            if (fallbackCoordsList.length >= 2) {
                const fallbackLine = new ol.geom.LineString(fallbackCoordsList);
                routeLayerSource.addFeature(new ol.Feature({ geometry: fallbackLine, name: `Complete Fallback Route` }));
                console.log('Drew complete fallback route with straight lines.');
            }
        }
    }


    map.on('click', function(evt) {
        const clickedMapCoords = evt.coordinate; // In map projection
        const clickedLonLat = ol.proj.toLonLat(clickedMapCoords); // For storing and OSRM
        const identifiedNearestStation = findNearestNode(clickedMapCoords, allNodes);

        if (!identifiedNearestStation) {
            alert('Your selected point is too far from any known transit station. Please click closer to the transit network.');
            return;
        }

        if (clickCount === 0) {
            startPoint = { clickedCoordsLonLat: clickedLonLat, nearestStationNode: identifiedNearestStation };
            clickCount++;
            statusLabel.textContent = `Start point selected. Nearest station: ${startPoint.nearestStationNode.id}. Select your precise end point.`;
            console.log("Start point:", startPoint);

            clickedPointsSource.clear(); 
            routeLayerSource.clear();  

            const startMarker = new ol.Feature({
                geometry: new ol.geom.Point(clickedMapCoords),
                name: 'User Start Point'
            });
            clickedPointsSource.addFeature(startMarker);

        } else if (clickCount === 1) {

            if (startPoint.nearestStationNode.id === identifiedNearestStation.id && 
                (Math.abs(startPoint.clickedCoordsLonLat[0] - clickedLonLat[0]) < 0.0001 && Math.abs(startPoint.clickedCoordsLonLat[1] - clickedLonLat[1]) < 0.0001) ) {
                alert('Your start and end points are effectively the same. Please select a different end point.');
                return;
            }

            endPoint = { clickedCoordsLonLat: clickedLonLat, nearestStationNode: identifiedNearestStation };
            console.log("End point:", endPoint);
            
            const endMarker = new ol.Feature({
                geometry: new ol.geom.Point(clickedMapCoords),
                name: 'User End Point'
            });
            clickedPointsSource.addFeature(endMarker); // Add end marker

            fetchAndDisplayRoute(startPoint, endPoint);
            
            clickCount = 2; 
        } else {
            alert('Route selection is complete. Please use "Clear Selection" to start a new route.');
        }
    });

    const clearButton = document.createElement('button');
    clearButton.id = 'clear-selection-button';
    clearButton.textContent = 'Clear Selection';
    statusLabel.parentNode.insertBefore(clearButton, statusLabel.nextSibling);

    clearButton.addEventListener('click', () => {
        startPoint = null;
        endPoint = null;
        clickCount = 0;
        clickedPointsSource.clear(); // Clear precise click markers
        routeLayerSource.clear(); 
        statusLabel.textContent = 'Select your precise starting point on the map.';
        if (infoPanel) {
            infoPanel.style.display = 'none';
            routeDetailsDiv.innerHTML = '';
        }
        console.log('Selection cleared.');
    });

    fetchNodes(); 
});
