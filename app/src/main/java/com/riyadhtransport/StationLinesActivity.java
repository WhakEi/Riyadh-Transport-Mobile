package com.riyadhtransport;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.riyadhtransport.adapters.LineAdapter;
import com.riyadhtransport.models.Line;
import com.riyadhtransport.utils.LineColorHelper;
import java.util.ArrayList;
import java.util.List;

public class StationLinesActivity extends AppCompatActivity {
    
    private TextView stationNameView;
    private RecyclerView linesRecycler;
    private LineAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_lines);
        
        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Get station info from intent
        String stationName = getIntent().getStringExtra("station_name");
        ArrayList<String> metroLines = getIntent().getStringArrayListExtra("metro_lines");
        ArrayList<String> busLines = getIntent().getStringArrayListExtra("bus_lines");
        
        // Initialize views
        stationNameView = findViewById(R.id.station_name);
        linesRecycler = findViewById(R.id.lines_list);
        
        // Set station name
        stationNameView.setText(stationName);
        
        // Setup RecyclerView
        adapter = new LineAdapter(this::onLineClick);
        linesRecycler.setLayoutManager(new LinearLayoutManager(this));
        linesRecycler.setAdapter(adapter);
        
        // Build lines list
        List<Line> allLines = new ArrayList<>();
        
        if (metroLines != null) {
            for (String lineId : metroLines) {
                String lineName = LineColorHelper.getMetroLineName(this, lineId);
                allLines.add(new Line(lineId, lineName, "metro"));
            }
        }
        
        if (busLines != null) {
            for (String lineId : busLines) {
                allLines.add(new Line(lineId, getString(R.string.bus) + " " + lineId, "bus"));
            }
        }
        
        adapter.setLines(allLines);
    }
    
    private void onLineClick(Line line) {
        // Open LineStationsActivity to show stations on this line
        Intent intent = new Intent(this, LineStationsActivity.class);
        intent.putExtra("line_id", line.getId());
        intent.putExtra("line_name", line.getName());
        intent.putExtra("line_type", line.getType());
        startActivity(intent);
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
