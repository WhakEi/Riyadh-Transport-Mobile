package com.riyadhtransport;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.riyadhtransport.adapters.StationListAdapter;
import com.riyadhtransport.api.ApiClient;
import com.riyadhtransport.utils.LineColorHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LineStationsActivity extends AppCompatActivity {
    
    private RecyclerView stationsRecycler;
    private StationListAdapter adapter;
    private ProgressBar progressBar;
    private TextView titleView;
    private String lineId;
    private String lineName;
    private String lineType;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_stations);
        
        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Get line info from intent
        lineId = getIntent().getStringExtra("line_id");
        lineName = getIntent().getStringExtra("line_name");
        lineType = getIntent().getStringExtra("line_type");
        String direction = getIntent().getStringExtra("direction");
        
        // Initialize views
        titleView = findViewById(R.id.line_title);
        stationsRecycler = findViewById(R.id.stations_list);
        progressBar = findViewById(R.id.progress_bar);
        
        // Set title
        titleView.setText(lineName);
        if (direction != null && !direction.isEmpty()) {
            titleView.setText(lineName + " - " + direction);
        }
        
        // Set title color based on line type
        if ("metro".equals(lineType)) {
            int color = LineColorHelper.getMetroLineColor(this, lineId);
            titleView.setTextColor(color);
        } else {
            int color = LineColorHelper.getBusLineColor(this);
            titleView.setTextColor(color);
        }
        
        // Setup RecyclerView
        adapter = new StationListAdapter(this::onStationClick);
        stationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        stationsRecycler.setAdapter(adapter);
        
        // Load stations from intent or fetch from API
        ArrayList<String> stations = getIntent().getStringArrayListExtra("stations");
        if (stations != null && !stations.isEmpty()) {
            adapter.setStations(stations);
            progressBar.setVisibility(View.GONE);
        } else {
            loadLineStations();
        }
    }
    
    private void loadLineStations() {
        progressBar.setVisibility(View.VISIBLE);
        
        if ("metro".equals(lineType)) {
            loadMetroStations();
        } else {
            loadBusStations();
        }
    }
    
    private void loadMetroStations() {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("line", lineId);
        
        ApiClient.getApiService().viewMetro(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    List<String> stations = new ArrayList<>();
                    if (data.has("stations")) {
                        JsonArray stationsArray = data.getAsJsonArray("stations");
                        for (int i = 0; i < stationsArray.size(); i++) {
                            stations.add(stationsArray.get(i).getAsString());
                        }
                    }
                    adapter.setStations(stations);
                } else {
                    Toast.makeText(LineStationsActivity.this,
                            R.string.error_network,
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LineStationsActivity.this,
                        getString(R.string.error_network) + ": " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadBusStations() {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("line", lineId);
        
        ApiClient.getApiService().viewBus(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    String direction = getIntent().getStringExtra("direction");
                    
                    if (direction != null && data.has(direction)) {
                        List<String> stations = new ArrayList<>();
                        JsonArray stationsArray = data.getAsJsonArray(direction);
                        for (int i = 0; i < stationsArray.size(); i++) {
                            stations.add(stationsArray.get(i).getAsString());
                        }
                        adapter.setStations(stations);
                    }
                } else {
                    Toast.makeText(LineStationsActivity.this,
                            R.string.error_network,
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LineStationsActivity.this,
                        getString(R.string.error_network) + ": " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void onStationClick(String stationName) {
        // Call /searchstation endpoint to get lines passing through this station
        searchStation(stationName);
    }
    
    private void searchStation(String stationName) {
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
                        Toast.makeText(LineStationsActivity.this,
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
                    Intent intent = new Intent(LineStationsActivity.this, StationLinesActivity.class);
                    intent.putExtra("station_name", stationName);
                    intent.putStringArrayListExtra("metro_lines", new ArrayList<>(metroLines));
                    intent.putStringArrayListExtra("bus_lines", new ArrayList<>(busLines));
                    startActivity(intent);
                } else {
                    String errorMsg = getString(R.string.error_network);
                    if (response.code() != 0) {
                        errorMsg += " (HTTP " + response.code() + ")";
                    }
                    Toast.makeText(LineStationsActivity.this,
                            errorMsg,
                            Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(LineStationsActivity.this,
                        getString(R.string.error_network) + ": " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
