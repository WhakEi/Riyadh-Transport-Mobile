package com.riyadhtransport.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.riyadhtransport.R;
import com.riyadhtransport.StationLinesActivity;
import com.riyadhtransport.adapters.StationAdapter;
import com.riyadhtransport.api.ApiClient;
import com.riyadhtransport.models.Station;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.riyadhtransport.utils.LocationHelper;
import java.util.HashMap;
import java.util.Map;

public class StationsFragment extends Fragment {
    
    private TextInputEditText searchInput;
    private RecyclerView stationsRecycler;
    private StationAdapter stationAdapter;
    private ProgressBar progressBar;
    private LocationHelper locationHelper;
    private double currentLat = 0;
    private double currentLng = 0;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stations, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        locationHelper = new LocationHelper(requireContext());

        // Initialize views
        searchInput = view.findViewById(R.id.search_stations);
        stationsRecycler = view.findViewById(R.id.stations_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // Setup RecyclerView
        stationAdapter = new StationAdapter(station -> {
            // Handle station click - show lines passing through this station
            searchStationLines(station.getDisplayName());
        });
        
        stationsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        stationsRecycler.setAdapter(stationAdapter);
        
        // Setup search
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                stationAdapter.filter(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Load nearby stations based on GPS location
        loadNearbyStations();
    }
    
    private void loadNearbyStations() {
        if (!LocationHelper.hasLocationPermission(requireContext())) {
            Toast.makeText(requireContext(),
                    R.string.error_permission,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLat = latitude;
                currentLng = longitude;
                fetchNearbyStations(latitude, longitude);
            }

            @Override
            public void onLocationError(String error) {
                Toast.makeText(requireContext(),
                        R.string.error_location_default,
                        Toast.LENGTH_SHORT).show();
                // Use Riyadh center as default
                fetchNearbyStations(24.7136, 46.6753);
            }
        });
    }

    public void fetchNearbyStations(double latitude, double longitude) {
        progressBar.setVisibility(View.VISIBLE);
        Map<String, Object> body = new HashMap<>();
        body.put("lat", latitude);
        body.put("lng", longitude);
        body.put("radius", 1.5); // 1.5 km radius

        ApiClient.getApiService().getNearbyStations(body).enqueue(new Callback<List<Station>>() {
            @Override
            public void onResponse(@NonNull Call<List<Station>> call, 
                                   @NonNull Response<List<Station>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    stationAdapter.setStations(response.body());
                } else {
                    Toast.makeText(requireContext(), 
                            R.string.error_network, 
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<Station>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), 
                        getString(R.string.error_network) + ": " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void searchStationLines(String stationName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("station_name", stationName);
        
        ApiClient.getApiService().searchStation(requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    
                    // Check if there's an error in the response
                    if (data.containsKey("error")) {
                        String errorMsg = (String) data.get("error");
                        Toast.makeText(requireContext(),
                                getString(R.string.error) + ": " + errorMsg,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    // Extract lines from response
                    List<String> metroLines = new ArrayList<>();
                    List<String> busLines = new ArrayList<>();
                    
                    if (data.containsKey("metro_lines")) {
                        Object metroObj = data.get("metro_lines");
                        if (metroObj instanceof List) {
                            List<?> rawList = (List<?>) metroObj;
                            for (Object item : rawList) {
                                if (item instanceof String) {
                                    metroLines.add((String) item);
                                }
                            }
                        }
                    }
                    
                    if (data.containsKey("bus_lines")) {
                        Object busObj = data.get("bus_lines");
                        if (busObj instanceof List) {
                            List<?> rawList = (List<?>) busObj;
                            for (Object item : rawList) {
                                if (item instanceof String) {
                                    busLines.add((String) item);
                                }
                            }
                        }
                    }
                    
                    // Open activity to show lines passing through this station
                    Intent intent = new Intent(requireContext(), StationLinesActivity.class);
                    intent.putExtra("station_name", stationName);
                    intent.putStringArrayListExtra("metro_lines", new ArrayList<>(metroLines));
                    intent.putStringArrayListExtra("bus_lines", new ArrayList<>(busLines));
                    startActivity(intent);
                } else {
                    String errorMsg = getString(R.string.error_network);
                    if (response.code() != 0) {
                        errorMsg += " (HTTP " + response.code() + ")";
                    }
                    Toast.makeText(requireContext(),
                            errorMsg,
                            Toast.LENGTH_LONG).show();
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
}
