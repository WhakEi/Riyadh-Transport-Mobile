package com.riyadhtransport.models;

public class SearchResult {
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private boolean isStation;
    
    public SearchResult() {
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public boolean isStation() {
        return isStation;
    }
    
    public void setStation(boolean station) {
        isStation = station;
    }
}
