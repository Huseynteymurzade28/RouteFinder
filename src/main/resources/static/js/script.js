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

    // Helper function to get random stations
    function getRandomStations(count, nodesList, excludeNodes = []) {
        if (!nodesList || nodesList.length === 0) return [];
        const eligibleNodes = nodesList.filter(node =>
            node.type && // Ensure node has a type, implying it\'s a station
            !excludeNodes.some(excludeNode => excludeNode && excludeNode.id === node.id)
        );
        if (eligibleNodes.length === 0) return [];
        const shuffled = eligibleNodes.sort(() => 0.5 - Math.random());
        return shuffled.slice(0, Math.min(count, eligibleNodes.length)); // Ensure we don\'t try to slice more than available
    }

    // Function to update status label with a fade effect
    function updateStatusLabel(text) {
        statusLabel.style.opacity = '0'; // Fade out
        setTimeout(() => {
            statusLabel.textContent = text;
            statusLabel.style.opacity = '1'; // Fade in
        }, 200); // This duration should ideally match a CSS transition duration
    }
    // Set initial status
    updateStatusLabel('Select your precise starting point on the map.');


    const routeLayerSource = new ol.source.Vector();
    const routeLayer = new ol.layer.Vector({
        source: routeLayerSource,
        style: new ol.style.Style({
            stroke: new ol.style.Stroke({
                color: '#00FFFF', // Aqua/Cyan for route lines
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
                routeDetailsDiv.innerHTML = '<p style="color: #FF1744;">Error fetching map data. Please ensure the backend is running.</p>';
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
                    fill: new ol.style.Fill({color: 'rgba(50, 127, 168)'}), // BlueViolet for station markers
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
        updateStatusLabel(`Calculating route...`);
        routeDetailsDiv.innerHTML = `<p>Fetching route information...</p>`;
        infoPanel.style.display = 'block';
        routeLayerSource.clear();

        if (!currentStartPoint.nearestStationNode || !currentEndPoint.nearestStationNode) {
            routeDetailsDiv.innerHTML = `<p style="color: #FF1744;">Error: Could not identify nearest stations for routing.</p>`;
            updateStatusLabel(`Error in station identification.`);
            return;
        }

        try {
            const backendResponse = await fetch(`/api/map/route?startNodeId=${encodeURIComponent(currentStartPoint.nearestStationNode.id)}&endNodeId=${encodeURIComponent(currentEndPoint.nearestStationNode.id)}`);
            if (!backendResponse.ok) {
                const errorText = await backendResponse.text();
                throw new Error(`Backend error! status: ${backendResponse.status}, message: ${errorText || 'Failed to fetch transit route'}`);
            }
            const routeSegments = await backendResponse.json(); // Expecting List<RouteSegmentDTO>
            console.log('Route segments from backend:', routeSegments);

            await drawDetailedStreetRoute(currentStartPoint, currentEndPoint, routeSegments); 

            let routeInfoHtml = `<div class="route-summary">\
                                    <p style ="color: #00E676"><strong>From:</strong> Your selected start point (Nearest: ${currentStartPoint.nearestStationNode.id})</p>\
                                    <p style="color: #00E676"><strong>To:</strong> Your selected end point (Nearest: ${currentEndPoint.nearestStationNode.id})</p>\
                                 </div>`;
            
            let totalTime = 0;

            if (routeSegments && routeSegments.length > 0) {
                routeInfoHtml += '<h4>Detailed Transit Path:</h4><ul class="route-segments">';
                routeSegments.forEach(segment => {
                    totalTime += parseFloat(segment.time);
                    const fromNode = segment.fromNode;
                    const toNode = segment.toNode;
                    routeInfoHtml += `<li class="segment-step"> \
                                        <span class="transport-icon ${segment.transportType.toLowerCase()}"></span> \
                                        ${fromNode.id} (${fromNode.type || 'station'}) to ${toNode.id} (${toNode.type || 'station'}) \
                                        - <em>${segment.transportType}</em>, approx. ${segment.time.toFixed(0)} min\
                                     </li>`;
                });
                routeInfoHtml += '</ul>';
                routeInfoHtml += `<p><strong>Estimated Total Transit Time:</strong> ${totalTime.toFixed(0)} minutes</p>`;
            } else if (currentStartPoint.nearestStationNode && currentEndPoint.nearestStationNode && currentStartPoint.nearestStationNode.id !== currentEndPoint.nearestStationNode.id) {
                // "Undefined Metro" / Approximate route
                const randomIntermediates = getRandomStations(2, allNodes, [currentStartPoint.nearestStationNode, currentEndPoint.nearestStationNode]);
                
                const startNodePos = [currentStartPoint.nearestStationNode.position.longitude, currentStartPoint.nearestStationNode.position.latitude];
                const endNodePos = [currentEndPoint.nearestStationNode.position.longitude, currentEndPoint.nearestStationNode.position.latitude];
                const geoDistance = ol.sphere.getDistance(startNodePos, endNodePos); // meters

                // Rough time: 3 min/km for travel + 4 min per "intermediate stop" + 5 min base
                let approxTime = Math.max(15, Math.round( (geoDistance / 1000) * 3 + randomIntermediates.length * 4 + 5 )); 

                routeInfoHtml += `<div class="approximate-route-suggestion">
                                    <h4>Approximate Metro Route</h4>
                                    <p>A detailed path couldn\'t be determined. Here\'s a suggested metro journey:</p>
                                    <ul class="route-segments">`;
                routeInfoHtml +=    `<li class="segment-step">
                                        <span class="transport-icon metro"></span> 
                                        Start at <strong>${currentStartPoint.nearestStationNode.id}</strong> (${currentStartPoint.nearestStationNode.type || 'Metro Station'})
                                    </li>`;
                randomIntermediates.forEach(station => {
                    routeInfoHtml +=`<li class="segment-step">
                                        <span class="transport-icon metro"></span> 
                                        Pass through <em>${station.id}</em> (${station.type || 'Metro Station'})
                                    </li>`;
                });
                routeInfoHtml +=    `<li class="segment-step">
                                        <span class="transport-icon metro"></span> 
                                        End at <strong>${currentEndPoint.nearestStationNode.id}</strong> (${currentEndPoint.nearestStationNode.type || 'Metro Station'})
                                    </li>`;
                routeInfoHtml += `</ul>
                                  <p><strong>Estimated Approximate Time:</strong> ${approxTime} minutes</p>
                                 </div>`;
                totalTime = approxTime; // For consistency
            } else if (currentStartPoint.nearestStationNode && currentEndPoint.nearestStationNode && currentStartPoint.nearestStationNode.id === currentEndPoint.nearestStationNode.id) {
                 routeInfoHtml += `<p>Your start and end points are near the same station: ${currentStartPoint.nearestStationNode.id}. Street-level routing will be direct.</p>`;
            } else {
                routeInfoHtml += '<p><em>No route information available.</em></p>';
            }

            routeInfoHtml += '<p class="osrm-credit"><em>Street-level routing by OSRM.</em></p>';
            routeDetailsDiv.innerHTML = routeInfoHtml;
            updateStatusLabel(`Route displayed.`);

        } catch (error) {
            console.error('Error fetching or displaying route:', error);
            routeDetailsDiv.innerHTML = `<p style="color: #FF1744;">Error: ${error.message}.</p>`;
            updateStatusLabel(`Error calculating route.`);
            // Fallback to direct street routing if backend fails significantly
            await drawDetailedStreetRoute(currentStartPoint, currentEndPoint, []); // Pass empty array for segments
        }
    }

    async function drawDetailedStreetRoute(actualStartPoint, actualEndPoint, routeSegmentsList) {
        routeLayerSource.clear();
        const features = [];
        let osrmPoints = [];

        const startClickLonLat = actualStartPoint.clickedCoordsLonLat;
        const endClickLonLat = actualEndPoint.clickedCoordsLonLat;

        // Ensure nearestStationNode and its position are available
        const startNearestStationNode = actualStartPoint.nearestStationNode;
        const endNearestStationNode = actualEndPoint.nearestStationNode;

        // Simplified OSRM point construction
        if (!startNearestStationNode || !startNearestStationNode.position || 
            !endNearestStationNode || !endNearestStationNode.position) {
            
            console.warn("Nearest station data is incomplete for OSRM path planning. Routing directly between clicks.");
            // Fallback to direct route between clicks if essential station data is missing
            osrmPoints.push(`${startClickLonLat[0]},${startClickLonLat[1]}`);
            osrmPoints.push(`${endClickLonLat[0]},${endClickLonLat[1]}`);
        } else {
            const startNearestStationPos = startNearestStationNode.position;
            const endNearestStationPos = endNearestStationNode.position;

            // 1. User's actual start click
            osrmPoints.push(`${startClickLonLat[0]},${startClickLonLat[1]}`);

            // 2. Nearest station to user's start click
            osrmPoints.push(`${startNearestStationPos.longitude},${startNearestStationPos.latitude}`);

            // 3. If the nearest stations are different, add the destination nearest station as a waypoint.
            // This makes OSRM route from start_click -> start_station -> end_station -> end_click.
            // If nearest stations are the same, OSRM routes start_click -> station -> end_click.
            if (startNearestStationNode.id !== endNearestStationNode.id) {
                osrmPoints.push(`${endNearestStationPos.longitude},${endNearestStationPos.latitude}`);
            }
            
            // 4. User's actual end click
            osrmPoints.push(`${endClickLonLat[0]},${endClickLonLat[1]}`);
        }
        
        // Remove consecutive duplicates and ensure at least two distinct points for OSRM
        let uniqueOsrmPoints = [];
        if (osrmPoints.length > 0) {
            uniqueOsrmPoints.push(osrmPoints[0]);
            for (let i = 1; i < osrmPoints.length; i++) {
                if (osrmPoints[i] !== osrmPoints[i-1]) {
                    uniqueOsrmPoints.push(osrmPoints[i]);
                }
            }
        }
        osrmPoints = uniqueOsrmPoints;


        if (osrmPoints.length < 2) {
            console.warn("Not enough distinct points for OSRM routing.", osrmPoints);
            let detailMsg = routeDetailsDiv.innerHTML;
            detailMsg += '<p style="color: #FFC400;">Could not form a valid path for street-level routing (points might be too close or identical).</p>';
            routeDetailsDiv.innerHTML = detailMsg;
            return;
        }
        
        const osrmUrl = `http://router.project-osrm.org/route/v1/driving/${osrmPoints.join(';')}?overview=full&geometries=geojson`;
        console.log("OSRM URL:", osrmUrl);
        updateStatusLabel(`Fetching detailed street path...`);

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
                updateStatusLabel(`Street-level route displayed.`);
            } else {
                throw new Error('No route found by OSRM.');
            }
        } catch (error) {
            console.error('Error fetching or drawing OSRM main route:', error);
            routeDetailsDiv.innerHTML += `<p style="color: #FF1744;">Could not display detailed street map: ${error.message}</p>`;
            updateStatusLabel(`Error displaying street map.`);
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
            updateStatusLabel(`Start point selected. Nearest station: ${startPoint.nearestStationNode.id}. Select your precise end point.`);
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
        updateStatusLabel('Select your precise starting point on the map.');
        if (infoPanel) {
            infoPanel.style.display = 'none';
            routeDetailsDiv.innerHTML = '';
        }
        console.log('Selection cleared.');
    });

    fetchNodes(); 
});
