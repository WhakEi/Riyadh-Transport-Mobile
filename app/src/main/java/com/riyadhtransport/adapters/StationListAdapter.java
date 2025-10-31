package com.riyadhtransport.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.riyadhtransport.R;
import java.util.ArrayList;
import java.util.List;

public class StationListAdapter extends RecyclerView.Adapter<StationListAdapter.StationViewHolder> {

    private List<String> stations;
    private OnStationClickListener clickListener;

    public interface OnStationClickListener {
        void onStationClick(String stationName);
    }

    public StationListAdapter(OnStationClickListener listener) {
        this.stations = new ArrayList<>();
        this.clickListener = listener;
    }

    public void setStations(List<String> stations) {
        this.stations = stations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_station, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        String station = stations.get(position);
        holder.bind(station, position + 1);
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    class StationViewHolder extends RecyclerView.ViewHolder {
        private TextView stationName;

        StationViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onStationClick(stations.get(position));
                }
            });
        }

        void bind(String station, int number) {
            stationName.setText(station);
        }
    }
}
