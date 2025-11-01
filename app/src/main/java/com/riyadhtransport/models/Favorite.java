package com.riyadhtransport.models;

public class Favorite {
    private String name;
    private String type; // "station", "location"
    private double latitude;
    private double longitude;
    private String stationType; // "metro", "bus" for stations, null for locations

    public Favorite() {
    }

    public Favorite(String name, String type, double latitude, double longitude, String stationType) {
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.stationType = stationType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public boolean isStation() {
        return "station".equals(type);
    }
}
