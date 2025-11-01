package com.riyadhtransport;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.riyadhtransport.adapters.FavoritesAdapter;
import com.riyadhtransport.models.Favorite;
import com.riyadhtransport.utils.FavoritesManager;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    
    private RecyclerView favoritesRecycler;
    private TextView emptyView;
    private FavoritesAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        
        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.favorites);
        }
        
        // Initialize views
        favoritesRecycler = findViewById(R.id.favorites_recycler);
        emptyView = findViewById(R.id.empty_view);
        
        // Setup RecyclerView
        adapter = new FavoritesAdapter(this::onFavoriteClick, this::onFavoriteRemove);
        favoritesRecycler.setLayoutManager(new LinearLayoutManager(this));
        favoritesRecycler.setAdapter(adapter);
        
        loadFavorites();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }
    
    private void loadFavorites() {
        List<Favorite> favorites = FavoritesManager.getFavorites(this);
        adapter.setFavorites(favorites);
        
        if (favorites.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            favoritesRecycler.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            favoritesRecycler.setVisibility(View.VISIBLE);
        }
    }
    
    private void onFavoriteClick(Favorite favorite) {
        // Set as destination in route fragment and go back to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("set_destination", true);
        intent.putExtra("destination_name", favorite.getName());
        intent.putExtra("destination_lat", favorite.getLatitude());
        intent.putExtra("destination_lng", favorite.getLongitude());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    
    private void onFavoriteRemove(Favorite favorite) {
        FavoritesManager.removeFavorite(this, favorite);
        loadFavorites();
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
