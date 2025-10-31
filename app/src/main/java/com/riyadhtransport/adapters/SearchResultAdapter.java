package com.riyadhtransport.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.riyadhtransport.R;
import com.riyadhtransport.models.SearchResult;
import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ResultViewHolder> {
    
    private List<SearchResult> results;
    private OnResultClickListener clickListener;
    
    public interface OnResultClickListener {
        void onResultClick(SearchResult result);
    }
    
    public SearchResultAdapter(OnResultClickListener listener) {
        this.results = new ArrayList<>();
        this.clickListener = listener;
    }
    
    public void setResults(List<SearchResult> results) {
        this.results = results;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ResultViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.bind(result);
    }
    
    @Override
    public int getItemCount() {
        return results.size();
    }
    
    class ResultViewHolder extends RecyclerView.ViewHolder {
        private ImageView icon;
        private TextView name;
        private TextView description;
        
        ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.result_icon);
            name = itemView.findViewById(R.id.result_name);
            description = itemView.findViewById(R.id.result_description);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onResultClick(results.get(position));
                }
            });
        }
        
        void bind(SearchResult result) {
            name.setText(result.getName());
            description.setText(result.getDescription());
            
            // Set icon and tint based on type
            if (result.isStation()) {
                icon.setImageResource(R.drawable.ic_metro);
            } else {
                icon.setImageResource(android.R.drawable.ic_menu_mylocation);
            }
            
            // Set tint programmatically for compatibility
            int color = androidx.core.content.ContextCompat.getColor(
                    itemView.getContext(), R.color.colorPrimary);
            androidx.core.widget.ImageViewCompat.setImageTintList(icon,
                    android.content.res.ColorStateList.valueOf(color));
        }
    }
}
