package com.riyadhtransport.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.riyadhtransport.models.Favorite;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoritesManager {
    private static final String PREFS_NAME = "FavoritesData";
    private static final String KEY_FAVORITES = "favorites_list";

    public static List<Favorite> getFavorites(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_FAVORITES, null);
        
        if (json == null) {
            return new ArrayList<>();
        }
        
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Favorite>>(){}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveFavorites(Context context, List<Favorite> favorites) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(favorites);
        prefs.edit().putString(KEY_FAVORITES, json).apply();
    }

    public static void addFavorite(Context context, Favorite favorite) {
        List<Favorite> favorites = getFavorites(context);
        // Check if already exists
        for (Favorite f : favorites) {
            if (f.getName().equals(favorite.getName()) && 
                f.getLatitude() == favorite.getLatitude() && 
                f.getLongitude() == favorite.getLongitude()) {
                return; // Already exists
            }
        }
        favorites.add(favorite);
        saveFavorites(context, favorites);
    }

    public static void removeFavorite(Context context, Favorite favorite) {
        List<Favorite> favorites = getFavorites(context);
        favorites.removeIf(f -> 
            f.getName().equals(favorite.getName()) && 
            f.getLatitude() == favorite.getLatitude() && 
            f.getLongitude() == favorite.getLongitude()
        );
        saveFavorites(context, favorites);
    }

    public static boolean isFavorite(Context context, String name, double latitude, double longitude) {
        List<Favorite> favorites = getFavorites(context);
        for (Favorite f : favorites) {
            if (f.getName().equals(name) && 
                f.getLatitude() == latitude && 
                f.getLongitude() == longitude) {
                return true;
            }
        }
        return false;
    }
}
