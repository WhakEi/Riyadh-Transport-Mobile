package com.riyadhtransport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.riyadhtransport.adapters.SearchResultAdapter;
import com.riyadhtransport.api.ApiClient;
import com.riyadhtransport.models.NominatimResult;
import com.riyadhtransport.models.SearchResult;
import com.riyadhtransport.models.Station;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchLocationActivity extends AppCompatActivity {
    
    public static final String EXTRA_SEARCH_TYPE = "search_type";
    public static final String EXTRA_RESULT_NAME = "result_name";
    public static final String EXTRA_RESULT_LAT = "result_lat";
    public static final String EXTRA_RESULT_LNG = "result_lng";
    public static final String EXTRA_IS_STATION = "is_station";
    
    public static final int REQUEST_SEARCH_START = 1;
    public static final int REQUEST_SEARCH_END = 2;
    
    private TextInputEditText searchInput;
    private RecyclerView resultsRecycler;
    private ProgressBar progressBar;
    private SearchResultAdapter adapter;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private List<Station> allStations = new ArrayList<>();
    private String searchType;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_location);
        
        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Get search type from intent
        searchType = getIntent().getStringExtra(EXTRA_SEARCH_TYPE);
        if (searchType == null) {
            searchType = "origin";
        }
        
        // Set title based on search type
        if (getSupportActionBar() != null) {
            if ("origin".equals(searchType)) {
                getSupportActionBar().setTitle(R.string.start_location);
            } else {
                getSupportActionBar().setTitle(R.string.end_location);
            }
        }
        
        // Initialize views
        searchInput = findViewById(R.id.search_input);
        resultsRecycler = findViewById(R.id.results_recycler);
        progressBar = findViewById(R.id.progress_bar);
        
        // Setup RecyclerView
        adapter = new SearchResultAdapter(this::onResultClick);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(this));
        resultsRecycler.setAdapter(adapter);
        
        // Focus on search input
        searchInput.requestFocus();
        
        // Setup search listener with debounce
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // Schedule new search after 300ms delay
                searchRunnable = () -> performSearch(s.toString());
                searchHandler.postDelayed(searchRunnable, 300);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Load all stations first
        loadStations();
    }
    
    private void loadStations() {
        ApiClient.getApiService().getStations().enqueue(new Callback<List<Station>>() {
            @Override
            public void onResponse(@NonNull Call<List<Station>> call,
                                   @NonNull Response<List<Station>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allStations = response.body();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<Station>> call, @NonNull Throwable t) {
                // Silently fail - Nominatim will still work
            }
        });
    }
    
    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            adapter.setResults(new ArrayList<>());
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Search in both stations and Nominatim
        List<SearchResult> combinedResults = new ArrayList<>();
        
        // Search in local stations
        for (Station station : allStations) {
            if (station.getDisplayName().toLowerCase().contains(query.toLowerCase())) {
                SearchResult result = new SearchResult();
                result.setName(station.getDisplayName());
                result.setDescription(getString(R.string.metro_station)); // or bus_stop based on type
                result.setLatitude(station.getLatitude());
                result.setLongitude(station.getLongitude());
                result.setStation(true);
                combinedResults.add(result);
            }
        }
        
        // Search in Nominatim
        searchNominatim(query, combinedResults);
    }
    
    private void searchNominatim(String query, List<SearchResult> existingResults) {
        String viewbox = "46.5,24.5,47.0,25.0"; // Riyadh bounding box
        
        // Get current language for Nominatim results
        String language = com.riyadhtransport.utils.LocaleHelper.getLanguageCode(this);
        
        ApiClient.getNominatimService().search(
                query + ", Riyadh",
                "json",
                10,
                1,
                viewbox,
                language
        ).enqueue(new Callback<List<NominatimResult>>() {
            @Override
            public void onResponse(@NonNull Call<List<NominatimResult>> call,
                                   @NonNull Response<List<NominatimResult>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    // Add Nominatim results
                    for (NominatimResult nominatim : response.body()) {
                        // Ensure we have valid data before creating result
                        String displayName = nominatim.getDisplayName();
                        String type = nominatim.getType();
                        if (displayName != null && !displayName.isEmpty()) {
                            SearchResult result = new SearchResult();
                            result.setName(displayName);
                            result.setDescription(type != null ? type : "Location");
                            result.setLatitude(nominatim.getLatitudeAsDouble());
                            result.setLongitude(nominatim.getLongitudeAsDouble());
                            result.setStation(false);
                            existingResults.add(result);
                        }
                    }
                }
                
                // Update adapter with combined results
                adapter.setResults(existingResults);
            }
            
            @Override
            public void onFailure(@NonNull Call<List<NominatimResult>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                // Show station results even if Nominatim fails
                adapter.setResults(existingResults);
            }
        });
    }
    
    private void onResultClick(SearchResult result) {
        // Return result to calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_RESULT_NAME, result.getName());
        resultIntent.putExtra(EXTRA_RESULT_LAT, result.getLatitude());
        resultIntent.putExtra(EXTRA_RESULT_LNG, result.getLongitude());
        resultIntent.putExtra(EXTRA_IS_STATION, result.isStation());
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
