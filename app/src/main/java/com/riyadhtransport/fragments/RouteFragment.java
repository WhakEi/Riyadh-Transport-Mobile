package com.riyadhtransport.fragments;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.ProgressBar;
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
import com.riyadhtransport.SearchLocationActivity;
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
    private ProgressBar progressBar;
    private LocationHelper locationHelper;
    private double currentLat = 0;
    private double currentLng = 0;
    private double startLat = 0, startLng = 0, endLat = 0, endLng = 0;
    private String startName = "", endName = "";
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
        progressBar = view.findViewById(R.id.progress_bar);
        
        // Setup RecyclerView
        segmentAdapter = new RouteSegmentAdapter();
        routeSegmentsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        routeSegmentsRecycler.setAdapter(segmentAdapter);
        
        // Setup listeners
        findRouteButton.setOnClickListener(v -> findRoute());
        useLocationButton.setOnClickListener(v -> useMyLocation());
        
        // Open search activity on click
        startInput.setFocusable(false);
        startInput.setOnClickListener(v -> openSearchActivity(SearchLocationActivity.REQUEST_SEARCH_START));
        
        endInput.setFocusable(false);
        endInput.setOnClickListener(v -> openSearchActivity(SearchLocationActivity.REQUEST_SEARCH_END));

        // Load stations for map route drawing (not for autocomplete)
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

                    // Build station map for route drawing
                    for (Station station : allStations) {
                        String name = station.getDisplayName();
                        stationMap.put(name, station);
                    }
                    // Note: We don't set up autocomplete adapter here because
                    // SearchLocationActivity handles the search UI
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
    
    private void openSearchActivity(int requestCode) {
        Intent intent = new Intent(requireContext(), SearchLocationActivity.class);
        if (requestCode == SearchLocationActivity.REQUEST_SEARCH_START) {
            intent.putExtra(SearchLocationActivity.EXTRA_SEARCH_TYPE, "origin");
        } else {
            intent.putExtra(SearchLocationActivity.EXTRA_SEARCH_TYPE, "destination");
        }
        startActivityForResult(intent, requestCode);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            String name = data.getStringExtra(SearchLocationActivity.EXTRA_RESULT_NAME);
            double lat = data.getDoubleExtra(SearchLocationActivity.EXTRA_RESULT_LAT, 0);
            double lng = data.getDoubleExtra(SearchLocationActivity.EXTRA_RESULT_LNG, 0);
            
            if (requestCode == SearchLocationActivity.REQUEST_SEARCH_START) {
                startInput.setText(name);
                startLat = lat;
                startLng = lng;
                startName = name;
            } else if (requestCode == SearchLocationActivity.REQUEST_SEARCH_END) {
                endInput.setText(name);
                endLat = lat;
                endLng = lng;
                endName = name;
            }
        }
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
            startInput.setText(getString(R.string.my_location));
            startLat = currentLat;
            startLng = currentLng;
            startName = getString(R.string.my_location);
        }
    }
    
    private void findRoute() {
        if (startName.isEmpty() || endName.isEmpty()) {
            Toast.makeText(requireContext(), 
                    R.string.error_select_locations, 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (startLat == 0 || startLng == 0 || endLat == 0 || endLng == 0) {
            Toast.makeText(requireContext(), 
                    R.string.error_invalid_locations, 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        findRouteFromCoordinates(startLat, startLng, endLat, endLng);
        }

    private void findRouteFromCoordinates(double startLat, double startLng,
                                          double endLat, double endLng) {
        progressBar.setVisibility(View.VISIBLE);
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
                progressBar.setVisibility(View.GONE);
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
                                getString(R.string.error) + ": " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            R.string.error_failed_route,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
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
                    getString(R.string.error) + ": " + e.getMessage(),
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
                        Object latObj = fromMap.get("lat");
                        Object lngObj = fromMap.get("lng");
                        if (latObj instanceof Number && lngObj instanceof Number) {
                            double lat = ((Number) latObj).doubleValue();
                            double lng = ((Number) lngObj).doubleValue();
                            points.add(new GeoPoint(lat, lng));
                        }
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
                        Object latObj = toMap.get("lat");
                        Object lngObj = toMap.get("lng");
                        if (latObj instanceof Number && lngObj instanceof Number) {
                            double lat = ((Number) latObj).doubleValue();
                            double lng = ((Number) lngObj).doubleValue();
                            points.add(new GeoPoint(lat, lng));
                        }
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
    }
    
    // Public methods for setting locations from map tap
    public void setStartLocation(double latitude, double longitude) {
        String locationText = String.format("Location (%.4f, %.4f)", latitude, longitude);
        startInput.setText(locationText);
        startLat = latitude;
        startLng = longitude;
        startName = locationText;
    }

    public void setEndLocation(double latitude, double longitude) {
        String locationText = String.format("Location (%.4f, %.4f)", latitude, longitude);
        endInput.setText(locationText);
        endLat = latitude;
        endLng = longitude;
        endName = locationText;
    }
}
