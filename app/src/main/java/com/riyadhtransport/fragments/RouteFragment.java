package com.riyadhtransport.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import com.riyadhtransport.models.Station;
import com.riyadhtransport.MainActivity;
import com.riyadhtransport.R;
import com.riyadhtransport.adapters.RouteSegmentAdapter;
import com.riyadhtransport.api.ApiClient;
import com.riyadhtransport.models.Route;
import com.riyadhtransport.models.RouteSegment;
import com.riyadhtransport.utils.LocationHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

public class RouteFragment extends Fragment {
    
    private AutoCompleteTextView startInput;
    private AutoCompleteTextView endInput;
    private Button findRouteButton;
    private Button useLocationButton;
    private LinearLayout routeDetailsContainer;
    private RecyclerView routeSegmentsRecycler;
    private RouteSegmentAdapter segmentAdapter;
    private LocationHelper locationHelper;
    private double currentLat = 0;
    private double currentLng = 0;
    private List<Station> allStations = new ArrayList<>();
    private Map<String, Station> stationMap = new HashMap<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_route, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        locationHelper = new LocationHelper(requireContext());
        
        // Initialize views
        startInput = view.findViewById(R.id.start_input);
        endInput = view.findViewById(R.id.end_input);
        findRouteButton = view.findViewById(R.id.find_route_button);
        useLocationButton = view.findViewById(R.id.use_location_button);
        routeDetailsContainer = view.findViewById(R.id.route_details_container);
        routeSegmentsRecycler = view.findViewById(R.id.route_segments_recycler);
        
        // Setup RecyclerView
        segmentAdapter = new RouteSegmentAdapter();
        routeSegmentsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        routeSegmentsRecycler.setAdapter(segmentAdapter);
        
        // Setup listeners
        findRouteButton.setOnClickListener(v -> findRoute());
        useLocationButton.setOnClickListener(v -> useMyLocation());

        loadStations();
        
        // Get current location
        getCurrentLocation();
    }

    private void loadStations() {
        ApiClient.getApiService().getStations().enqueue(new Callback<List<Station>>() {
            @Override
            public void onResponse(@NonNull Call<List<Station>> call,
                                   @NonNull Response<List<Station>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allStations = response.body();

                    // Build station name list and map
                    List<String> stationNames = new ArrayList<>();
                    for (Station station : allStations) {
                        String name = station.getDisplayName();
                        stationNames.add(name);
                        stationMap.put(name, station);
                    }

                    // Setup autocomplete adapters
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            stationNames
                    );
                    startInput.setAdapter(adapter);
                    endInput.setAdapter(adapter);
                } else {
                    Toast.makeText(requireContext(),
                            R.string.error_network,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Station>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        getString(R.string.error_network) + ": " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void getCurrentLocation() {
        if (!LocationHelper.hasLocationPermission(requireContext())) {
            startInput.setHint(R.string.enter_destination);
            return;
        }
        
        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLat = latitude;
                currentLng = longitude;
                startInput.setHint(R.string.use_my_location);
            }
            
            @Override
            public void onLocationError(String error) {
                startInput.setHint(R.string.enter_destination);
            }
        });
    }
    
    private void useMyLocation() {
        if (currentLat == 0 && currentLng == 0) {
            getCurrentLocation();
            Toast.makeText(requireContext(), R.string.finding_location, Toast.LENGTH_SHORT).show();
        } else {
            startInput.setText(String.format("My Location (%.4f, %.4f)", currentLat, currentLng));
        }
    }
    
    private void findRoute() {
        String start = startInput.getText() != null ? startInput.getText().toString() : "";
        String end = endInput.getText() != null ? endInput.getText().toString() : "";
        
        if (start.isEmpty() || end.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter both start and end locations", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Parse coordinates from text if present
        double startLat = 0, startLng = 0, endLat = 0, endLng = 0;
        boolean startIsCoord = false, endIsCoord = false;

        // Check if start is a coordinate (from map tap or "My Location")
        if (start.contains("Location (") || start.contains("My Location")) {
            startLat = currentLat;
            startLng = currentLng;
            startIsCoord = true;
        }

        // Check if end is a coordinate (from map tap)
        if (end.contains("Location (")) {
            // Parse coordinates from text like "Location (24.1234, 46.5678)"
            try {
                String coords = end.substring(end.indexOf("(") + 1, end.indexOf(")"));
                String[] parts = coords.split(",");
                endLat = Double.parseDouble(parts[0].trim());
                endLng = Double.parseDouble(parts[1].trim());
                endIsCoord = true;
            } catch (Exception e) {
                // Failed to parse, treat as station name
            }
        }

        // Get coordinates based on input types
        if (startIsCoord && endIsCoord) {
            // Both coordinates
            findRouteFromCoordinates(startLat, startLng, endLat, endLng);
        } else if (startIsCoord && !endIsCoord) {
            // Start is coordinate, end is station
            Station endStation = stationMap.get(end);
            if (endStation != null) {
                findRouteFromCoordinates(startLat, startLng,
                        endStation.getLatitude(), endStation.getLongitude());
            } else {
                Toast.makeText(requireContext(),
                        "Please select a valid station for destination",
                        Toast.LENGTH_SHORT).show();
            }
            }
            else if (!startIsCoord && endIsCoord) {
            // Start is station, end is coordinate
            Station startStation = stationMap.get(start);
            if (startStation != null) {
                findRouteFromCoordinates(startStation.getLatitude(), startStation.getLongitude(),
                        endLat, endLng);
            } else {
                Toast.makeText(requireContext(),
                        "Please select a valid station for origin",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Both might be station names or location names
            Station startStation = stationMap.get(start);
            Station endStation = stationMap.get(end);

            if (startStation != null && endStation != null) {
                // Both are known stations
                findRouteFromCoordinates(startStation.getLatitude(), startStation.getLongitude(),
                        endStation.getLatitude(), endStation.getLongitude());
            } else if (startStation == null && endStation != null) {
                // Start is unknown, try Nominatim
                searchLocationAndFindRoute(start, true, endStation.getLatitude(), endStation.getLongitude());
            } else if (startStation != null && endStation == null) {
                // End is unknown, try Nominatim
                searchLocationAndFindRoute(end, false, startStation.getLatitude(), startStation.getLongitude());
            } else {
                // Both unknown, search start first
                searchBothLocationsAndFindRoute(start, end);
            }
        }
        }
    
    private void searchLocationAndFindRoute(String locationName, boolean isStart, double otherLat, double otherLng) {
        // Riyadh bounding box: viewbox format is: min_lon,min_lat,max_lon,max_lat
        String viewbox = "46.5,24.5,47.0,25.0";

        ApiClient.getNominatimService().search(
                locationName + ", Riyadh",
                "json",
                5,
                1,
                viewbox
        ).enqueue(new Callback<List<com.riyadhtransport.models.NominatimResult>>() {
            @Override
            public void onResponse(@NonNull Call<List<com.riyadhtransport.models.NominatimResult>> call,
                                   @NonNull Response<List<com.riyadhtransport.models.NominatimResult>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    com.riyadhtransport.models.NominatimResult result = response.body().get(0);
                    double lat = result.getLatitudeAsDouble();
                    double lng = result.getLongitudeAsDouble();

                    if (isStart) {
                        findRouteFromCoordinates(lat, lng, otherLat, otherLng);
                    } else {
                        findRouteFromCoordinates(otherLat, otherLng, lat, lng);
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Location not found: " + locationName,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<com.riyadhtransport.models.NominatimResult>> call,
                                  @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        "Failed to search location: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchBothLocationsAndFindRoute(String startName, String endName) {
        String viewbox = "46.5,24.5,47.0,25.0";

        // Search start location
        ApiClient.getNominatimService().search(
                startName + ", Riyadh",
                "json",
                5,
                1,
                viewbox
        ).enqueue(new Callback<List<com.riyadhtransport.models.NominatimResult>>() {
            @Override
            public void onResponse(@NonNull Call<List<com.riyadhtransport.models.NominatimResult>> call,
                                   @NonNull Response<List<com.riyadhtransport.models.NominatimResult>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    com.riyadhtransport.models.NominatimResult startResult = response.body().get(0);
                    double startLat = startResult.getLatitudeAsDouble();
                    double startLng = startResult.getLongitudeAsDouble();

                    // Now search end location
                    ApiClient.getNominatimService().search(
                            endName + ", Riyadh",
                            "json",
                            5,
                            1,
                            viewbox
                    ).enqueue(new Callback<List<com.riyadhtransport.models.NominatimResult>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<com.riyadhtransport.models.NominatimResult>> call2,
                                               @NonNull Response<List<com.riyadhtransport.models.NominatimResult>> response2) {
                            if (response2.isSuccessful() && response2.body() != null && !response2.body().isEmpty()) {
                                com.riyadhtransport.models.NominatimResult endResult = response2.body().get(0);
                                double endLat = endResult.getLatitudeAsDouble();
                                double endLng = endResult.getLongitudeAsDouble();

                                findRouteFromCoordinates(startLat, startLng, endLat, endLng);
                            } else {
                                Toast.makeText(requireContext(),
                                        "Destination not found: " + endName,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<com.riyadhtransport.models.NominatimResult>> call2,
                                              @NonNull Throwable t) {
                            Toast.makeText(requireContext(),
                                    "Failed to search destination: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(requireContext(),
                            "Origin not found: " + startName,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<com.riyadhtransport.models.NominatimResult>> call,
                                  @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        "Failed to search origin: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void findRouteFromCoordinates(double startLat, double startLng,
                                          double endLat, double endLng) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("start_lat", startLat);
        requestBody.put("start_lng", startLng);
        requestBody.put("end_lat", endLat);
        requestBody.put("end_lng", endLng);

        ApiClient.getApiService().findRouteFromCoordinates(requestBody)
                .enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseBody = response.body();

                    if (responseBody.containsKey("routes")) {
                        List<Map<String, Object>> routes =
                                (List<Map<String, Object>>) responseBody.get("routes");

                        if (routes != null && !routes.isEmpty()) {
                            Map<String, Object> route = routes.get(0);
                            displayRoute(route);
                        } else {
                            Toast.makeText(requireContext(),
                                    R.string.no_route_found,
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else if (responseBody.containsKey("error")) {
                        String error = (String) responseBody.get("error");
                        Toast.makeText(requireContext(),
                                "Error: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Failed to find route",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        getString(R.string.error_network) + ": " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayRoute(Map<String, Object> route) {
        try {
            // Parse route segments
            Gson gson = new Gson();
            String json = gson.toJson(route);
            Route routeObj = gson.fromJson(json, Route.class);

            if (routeObj != null && routeObj.getSegments() != null) {
                segmentAdapter.setSegments(routeObj.getSegments());
                routeDetailsContainer.setVisibility(View.VISIBLE);

                // Draw route on map
                drawRouteOnMap(routeObj);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Error displaying route: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void drawRouteOnMap(Route route) {
        if (getActivity() == null || !(getActivity() instanceof MainActivity)) {
            return;
        }

        MainActivity mainActivity = (MainActivity) getActivity();
        MapView mapView = mainActivity.getMapView();

        if (mapView == null) {
            return;
        }

        // Clear existing route overlays (keep location overlay)
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Polyline);

        // Draw each segment
        for (RouteSegment segment : route.getSegments()) {
            Polyline line = new Polyline();
            List<GeoPoint> points = new ArrayList<>();

            // Get segment coordinates based on type
            if (segment.isWalking()) {
                // Walking segment - dotted gray line (matching frontend walk color)
                line.setColor(Color.parseColor("#6c757d"));
                line.getPaint().setStrokeWidth(12f); // Increased width for visibility
                line.getPaint().setStyle(Paint.Style.STROKE);
                line.getPaint().setStrokeCap(Paint.Cap.ROUND);
                // Dash pattern: 20px dash, 10px gap
                line.getPaint().setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));

                // Add start and end points for walking
                // Note: You may need to parse the from/to objects to get coordinates
                addSegmentPoints(segment, points);

            } else if (segment.isMetro()) {
                // Metro segment - color based on line (matching frontend colors)
                int lineColor = com.riyadhtransport.utils.LineColorHelper.getMetroLineColor(
                        requireContext(), segment.getLine());
                line.setColor(lineColor);
                line.getPaint().setStrokeWidth(10f);

                // Add all station points for metro
                addSegmentPoints(segment, points);

            } else if (segment.isBus()) {
                // Bus segment - green line (matching frontend bus color)
                line.setColor(Color.parseColor("#18a034"));
                line.getPaint().setStrokeWidth(10f);

                // Add all station points for bus
                addSegmentPoints(segment, points);
            }

            if (!points.isEmpty()) {
                line.setPoints(points);
                mapView.getOverlays().add(line);
            }
        }

        mapView.invalidate();

        // Zoom to show the entire route
        if (!route.getSegments().isEmpty()) {
            zoomToRoute(mapView, route);
        }
    }

    private void addSegmentPoints(RouteSegment segment, List<GeoPoint> points) {
        if (segment.isWalking()) {
            // For walking segments, use from/to coordinates
            try {
                if (segment.getFrom() != null) {
                    if (segment.getFrom() instanceof Map) {
                        Map<String, Object> fromMap = (Map<String, Object>) segment.getFrom();
                        double lat = ((Number) fromMap.get("lat")).doubleValue();
                        double lng = ((Number) fromMap.get("lng")).doubleValue();
                        points.add(new GeoPoint(lat, lng));
                    } else if (segment.getFrom() instanceof String) {
                        // It's a station name
                        Station station = stationMap.get((String) segment.getFrom());
                        if (station != null) {
                            points.add(new GeoPoint(station.getLatitude(), station.getLongitude()));
                        }
                    }
                }
                
                if (segment.getTo() != null) {
                    if (segment.getTo() instanceof Map) {
                        Map<String, Object> toMap = (Map<String, Object>) segment.getTo();
                        double lat = ((Number) toMap.get("lat")).doubleValue();
                        double lng = ((Number) toMap.get("lng")).doubleValue();
                        points.add(new GeoPoint(lat, lng));
                    } else if (segment.getTo() instanceof String) {
                        // It's a station name
                        Station station = stationMap.get((String) segment.getTo());
                        if (station != null) {
                            points.add(new GeoPoint(station.getLatitude(), station.getLongitude()));
                        }
                    }
                }
            } catch (Exception e) {
                // If parsing fails, fall back to stations list if available
                if (segment.getStations() != null && !segment.getStations().isEmpty()) {
                    for (String stationName : segment.getStations()) {
                        Station station = stationMap.get(stationName);
                        if (station != null) {
                            points.add(new GeoPoint(station.getLatitude(), station.getLongitude()));
                        }
                    }
                }
            }
        } else {
            // For metro/bus segments, use stations list
            if (segment.getStations() != null && !segment.getStations().isEmpty()) {
                for (String stationName : segment.getStations()) {
                    Station station = stationMap.get(stationName);
                    if (station != null) {
                        points.add(new GeoPoint(station.getLatitude(), station.getLongitude()));
                    }
                }
            }
        }
    }

    private void zoomToRoute(MapView mapView, Route route) {
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;

        for (RouteSegment segment : route.getSegments()) {
            if (segment.getStations() != null) {
                for (String stationName : segment.getStations()) {
                    Station station = stationMap.get(stationName);
                    if (station != null) {
                        double lat = station.getLatitude();
                        double lon = station.getLongitude();

                        minLat = Math.min(minLat, lat);
                        maxLat = Math.max(maxLat, lat);
                        minLon = Math.min(minLon, lon);
                        maxLon = Math.max(maxLon, lon);
                    }
                }
            }
        }

        if (minLat != 90) {
            // Calculate center and zoom
            double centerLat = (minLat + maxLat) / 2;
            double centerLon = (minLon + maxLon) / 2;

            mapView.getController().setCenter(new GeoPoint(centerLat, centerLon));

            // Calculate appropriate zoom level based on bounds
            double latDiff = maxLat - minLat;
            double lonDiff = maxLon - minLon;
            double maxDiff = Math.max(latDiff, lonDiff);

            int zoomLevel = 15; // Default
            if (maxDiff > 0.1) zoomLevel = 12;
            else if (maxDiff > 0.05) zoomLevel = 13;
            else if (maxDiff > 0.02) zoomLevel = 14;

            mapView.getController().setZoom((double) zoomLevel);
        }
    }   // Public methods for setting locations from map tap
    public void setStartLocation(double latitude, double longitude) {
        currentLat = latitude;
        currentLng = longitude;
        startInput.setText(String.format("Location (%.4f, %.4f)", latitude, longitude));
    }

    public void setEndLocation(double latitude, double longitude) {
        endInput.setText(String.format("Location (%.4f, %.4f)", latitude, longitude));
    }
}
