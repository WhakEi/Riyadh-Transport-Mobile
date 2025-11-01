package com.riyadhtransport.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.riyadhtransport.LineStationsActivity;
import com.riyadhtransport.R;
import com.riyadhtransport.adapters.LineAdapter;
import com.riyadhtransport.api.ApiClient;
import com.riyadhtransport.models.Line;
import com.riyadhtransport.utils.LineColorHelper;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinesFragment extends Fragment {
    
    private static final String PREFS_NAME = "LinesCache";
    private static final long CACHE_DURATION = 7 * 24 * 60 * 60 * 1000L; // 1 week in milliseconds
    
    private TextInputEditText searchInput;
    private RecyclerView linesRecycler;
    private LineAdapter lineAdapter;
    private ProgressBar progressBar;
    private boolean linesLoaded = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lines, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        searchInput = view.findViewById(R.id.search_lines);
        linesRecycler = view.findViewById(R.id.lines_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // Setup RecyclerView
        lineAdapter = new LineAdapter(line -> showLineDetails(line));
        linesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        linesRecycler.setAdapter(lineAdapter);
        
        // Setup search
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lineAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load lines data (with caching)
        if (!linesLoaded) {
            loadLines();
        }
    }
    
    private void loadLines() {
        // Check cache first
        List<Line> cachedLines = loadFromCache();
        if (cachedLines != null && !cachedLines.isEmpty()) {
            lineAdapter.setLines(cachedLines);
            linesLoaded = true;
            return;
        }
        
        // Cache miss or expired - fetch from API
        fetchLinesFromApi();
    }
    
    private void fetchLinesFromApi() {
        progressBar.setVisibility(View.VISIBLE);
        List<Line> allLines = new ArrayList<>();

        // Load metro lines
        ApiClient.getApiService().getMetroLines().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String linesStr = response.body().get("lines").getAsString();
                    String[] metroLines = linesStr.split(",");

                    for (String lineId : metroLines) {
                        String lineName = LineColorHelper.getMetroLineName(requireContext(), lineId);
                        allLines.add(new Line(lineId, lineName, "metro"));
                    }

                    // After metro lines loaded, load bus lines
                    loadBusLines(allLines);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        getString(R.string.error_network),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBusLines(List<Line> allLines) {
        ApiClient.getApiService().getBusLines().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String linesStr = response.body().get("lines").getAsString();
                    String[] busLines = linesStr.split(",");

                    for (String lineId : busLines) {
                        allLines.add(new Line(lineId, getString(R.string.bus) + " " + lineId, "bus"));
                    }

                    // Update adapter and save to cache
                    lineAdapter.setLines(allLines);
                    linesLoaded = true;
                    saveToCache(allLines);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                // Still show metro lines if bus lines fail
                lineAdapter.setLines(allLines);
                linesLoaded = true;
                if (!allLines.isEmpty()) {
                    saveToCache(allLines);
                }
            }
        });
    }
    
    private String getCacheKey(String baseName) {
        // Create language-specific cache keys
        String language = com.riyadhtransport.utils.LocaleHelper.getLanguageCode(requireContext());
        return baseName + "_" + language;
    }
    
    private List<Line> loadFromCache() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String keyLines = getCacheKey("cached_lines");
        String keyTimestamp = getCacheKey("cache_timestamp");
        
        long timestamp = prefs.getLong(keyTimestamp, 0);
        long currentTime = System.currentTimeMillis();
        
        // Check if cache is still valid (within 1 week)
        if (currentTime - timestamp > CACHE_DURATION) {
            return null;
        }
        
        String json = prefs.getString(keyLines, null);
        if (json == null) {
            return null;
        }
        
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Line>>(){}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            return null;
        }
    }
    
    private void saveToCache(List<Line> lines) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String keyLines = getCacheKey("cached_lines");
        String keyTimestamp = getCacheKey("cache_timestamp");
        
        Gson gson = new Gson();
        String json = gson.toJson(lines);
        
        prefs.edit()
            .putString(keyLines, json)
            .putLong(keyTimestamp, System.currentTimeMillis())
            .apply();
    }
    
    public static void clearCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private void showLineDetails(Line line) {
        // Fetch line data from backend
        if (line.isMetro()) {
            loadMetroLineDetails(line);
        } else {
            loadBusLineDetails(line);
        }
    }

    private void loadMetroLineDetails(Line line) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("line", line.getId());

        ApiClient.getApiService().viewMetro(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    List<String> stations = new ArrayList<>();
                    if (data.has("stations")) {
                        data.getAsJsonArray("stations").forEach(element ->
                                stations.add(element.getAsString()));
                    }
                    showStationsList(line, stations);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        getString(R.string.error_network),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBusLineDetails(Line line) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("line", line.getId());

        ApiClient.getApiService().viewBus(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();

                    // Bus lines have directions as keys
                    List<String> directions = new ArrayList<>();
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : data.entrySet()) {
                        directions.add(entry.getKey());
                    }

                    if (directions.size() == 1) {
                        // Ring route - single direction
                        List<String> stations = new ArrayList<>();
                        data.getAsJsonArray(directions.get(0)).forEach(element ->
                                stations.add(element.getAsString()));
                        showStationsList(line, stations);
                    } else if (directions.size() >= 2) {
                        // Bi-directional route - show direction selector
                        showDirectionSelector(line, data, directions);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        getString(R.string.error_network),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDirectionSelector(Line line, JsonObject lineData, List<String> directions) {
        String dir1 = directions.get(0);
        String dir2 = directions.get(1);

        String[] options = {
                dir1 + " → " + dir2,
                dir2 + " → " + dir1
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_direction)
                .setItems(options, (dialog, which) -> {
                    String selectedDirection = which == 0 ? dir1 : dir2;
                    List<String> stations = new ArrayList<>();
                    lineData.getAsJsonArray(selectedDirection).forEach(element ->
                            stations.add(element.getAsString()));
                    
                    // Open activity with direction info
                    Intent intent = new Intent(requireContext(), LineStationsActivity.class);
                    intent.putExtra("line_id", line.getId());
                    intent.putExtra("line_name", line.getName());
                    intent.putExtra("line_type", line.getType());
                    intent.putExtra("direction", selectedDirection);
                    intent.putStringArrayListExtra("stations", new ArrayList<>(stations));
                    startActivity(intent);
                })
                .show();
    }

    private void showStationsList(Line line, List<String> stations) {
        // Open dedicated activity to show stations list
        Intent intent = new Intent(requireContext(), LineStationsActivity.class);
        intent.putExtra("line_id", line.getId());
        intent.putExtra("line_name", line.getName());
        intent.putExtra("line_type", line.getType());
        intent.putStringArrayListExtra("stations", new ArrayList<>(stations));
        startActivity(intent);
    }
}
