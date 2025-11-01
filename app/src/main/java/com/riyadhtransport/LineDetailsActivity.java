package com.riyadhtransport;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.riyadhtransport.api.ApiClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.JsonObject;

public class LineDetailsActivity extends AppCompatActivity {
    
    private TextView lineNameText;
    private RecyclerView stationsRecycler;
    private String lineNumber;
    private String lineType; // "metro" or "bus"
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_details);
        
        // Get line info from intent
        lineNumber = getIntent().getStringExtra("line_number");
        lineType = getIntent().getStringExtra("line_type");
        
        if (lineNumber == null) {
            Toast.makeText(this, R.string.error_no_line_specified, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize views
        lineNameText = findViewById(R.id.line_name);
        stationsRecycler = findViewById(R.id.stations_recycler);
        
        // Set line name
        lineNameText.setText(lineType + " " + lineNumber);
        
        // Setup RecyclerView
        stationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        
        // Load line details
        loadLineDetails();
    }
    
    private void loadLineDetails() {
        JsonObject body = new JsonObject();
        body.addProperty("line", lineNumber);
        
        Call<JsonObject> call;
        if ("metro".equalsIgnoreCase(lineType)) {
            call = ApiClient.getApiService().viewMetro(body);
        } else {
            call = ApiClient.getApiService().viewBus(body);
        }
        
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Handle line data
                    Toast.makeText(LineDetailsActivity.this, 
                            R.string.info_line_details_loaded, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LineDetailsActivity.this, 
                            R.string.error_failed_line_details, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(LineDetailsActivity.this, 
                        getString(R.string.error) + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
