package com.riyadhtransport.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.riyadhtransport.R;
import com.riyadhtransport.models.Favorite;
import java.util.ArrayList;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {
    
    private List<Favorite> favorites;
    private OnFavoriteClickListener clickListener;
    private OnFavoriteRemoveListener removeListener;
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Favorite favorite);
    }
    
    public interface OnFavoriteRemoveListener {
        void onFavoriteRemove(Favorite favorite);
    }
    
    public FavoritesAdapter(OnFavoriteClickListener clickListener, OnFavoriteRemoveListener removeListener) {
        this.favorites = new ArrayList<>();
        this.clickListener = clickListener;
        this.removeListener = removeListener;
    }
    
    public void setFavorites(List<Favorite> favorites) {
        this.favorites = favorites;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Favorite favorite = favorites.get(position);
        holder.bind(favorite, clickListener, removeListener);
    }
    
    @Override
    public int getItemCount() {
        return favorites.size();
    }
    
    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView favoriteName;
        TextView favoriteType;
        ImageButton removeButton;
        
        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            favoriteName = itemView.findViewById(R.id.favorite_name);
            favoriteType = itemView.findViewById(R.id.favorite_type);
            removeButton = itemView.findViewById(R.id.remove_button);
        }
        
        void bind(Favorite favorite, OnFavoriteClickListener clickListener, OnFavoriteRemoveListener removeListener) {
            favoriteName.setText(favorite.getName());
            
            String type;
            if (favorite.isStation()) {
                if ("metro".equalsIgnoreCase(favorite.getStationType())) {
                    type = itemView.getContext().getString(R.string.metro_station);
                } else {
                    type = itemView.getContext().getString(R.string.bus_stop);
                }
            } else {
                type = itemView.getContext().getString(R.string.search_location);
            }
            favoriteType.setText(type);
            
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onFavoriteClick(favorite);
                }
            });
            
            removeButton.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onFavoriteRemove(favorite);
                }
            });
        }
    }
}
