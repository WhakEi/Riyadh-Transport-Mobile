package com.riyadhtransport;

import android.Manifest;
import android.view.View;
import android.content.res.Configuration;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.riyadhtransport.fragments.LinesFragment;
import com.riyadhtransport.fragments.RouteFragment;
import com.riyadhtransport.fragments.StationsFragment;
import com.riyadhtransport.utils.LocationHelper;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.util.Locale;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fabSettings;
    private LocationHelper locationHelper;
    private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;

    // Riyadh coordinates
    private static final GeoPoint RIYADH_CENTER = new GeoPoint(24.7136, 46.6753);

    // MapTiler API key (same as web frontend)
    private static final String MAPTILER_API_KEY = "OBFUSCATED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure OSMDroid
        Context ctx = getApplicationContext();
        org.osmdroid.config.Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());

        // Initialize ApiClient with context for Arabic locale detection
        com.riyadhtransport.api.ApiClient.init(this);

        setContentView(R.layout.activity_main);

        // Initialize location helper
        locationHelper = new LocationHelper(this);

        // Initialize views
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_page_container);
        fabSettings = findViewById(R.id.fab_settings);
        mapView = findViewById(R.id.map);
        NestedScrollView bottomSheet = findViewById(R.id.bottom_sheet);

        // Setup bottom sheet behavior to allow full expansion
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(500); // Default peek height
        bottomSheetBehavior.setHideable(false); // Don't allow hiding
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        // Allow dragging to fully expand - pull tab can reach bottom of screen
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setHalfExpandedRatio(0.5f);
        // Set expanded offset to allow pull tab to reach screen bottom
        // The offset is the distance from top when fully expanded
        // Setting to a small value (like the pull tab height ~20dp) allows maximum expansion
        bottomSheetBehavior.setExpandedOffset(20);

        // Setup map
        setupMap();

        // Setup ViewPager with tabs
        setupViewPager();

        // Setup FAB for settings
        fabSettings.setOnClickListener(v -> showSettingsDialog());

        // Request location permission if not granted
        if (!LocationHelper.hasLocationPermission(this)) {
            LocationHelper.requestLocationPermission(this);
        }
    }

    private void setupMap() {
        // Get current language
        String language = getCurrentLanguage();

        // Create MapTiler tile source with language support
        OnlineTileSourceBase mapTilerSource = new XYTileSource(
                "MapTiler",
                0, 20, 256, ".png",
                new String[]{
                    "https://api.maptiler.com/maps/streets-v2/256/"
                },
                "© MapTiler © OpenStreetMap contributors",
                new org.osmdroid.tileprovider.tilesource.TileSourcePolicy(
                        2,
                        org.osmdroid.tileprovider.tilesource.TileSourcePolicy.FLAG_NO_BULK
                                | org.osmdroid.tileprovider.tilesource.TileSourcePolicy.FLAG_NO_PREVENTIVE
                )
        ) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                return getBaseUrl()
                        + org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                        + "/" + org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                        + "/" + org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                        + ".png?key=" + MAPTILER_API_KEY + "&language=" + language;
            }
        };

        mapView.setTileSource(mapTilerSource);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(11.0);
        mapView.getController().setCenter(RIYADH_CENTER);

        // Add my location overlay
        if (LocationHelper.hasLocationPermission(this)) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            myLocationOverlay.enableMyLocation();
            mapView.getOverlays().add(myLocationOverlay);
        }
        // Add map tap listener
        setupMapTapListener();
    }

    private void setupMapTapListener() {
        org.osmdroid.views.overlay.Overlay tapOverlay = new org.osmdroid.views.overlay.Overlay() {
            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent e, MapView mapView) {
                org.osmdroid.api.IGeoPoint tappedPoint = mapView.getProjection().fromPixels((int) e.getX(), (int) e.getY());
                showMapTapDialog(tappedPoint.getLatitude(), tappedPoint.getLongitude());
                return true;
            }
        };
        mapView.getOverlays().add(tapOverlay);
    }

    private void showMapTapDialog(double latitude, double longitude) {
        String[] options = {
                getString(R.string.set_as_origin),
                getString(R.string.set_as_destination),
                getString(R.string.view_nearby_stations)
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.map_tap_title)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Set as origin
                            setAsOrigin(latitude, longitude);
                            break;
                        case 1: // Set as destination
                            setAsDestination(latitude, longitude);
                            break;
                        case 2: // View nearby stations
                            viewNearbyStations(latitude, longitude);
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setAsOrigin(double latitude, double longitude) {
        // Switch to route tab and set origin
        viewPager.setCurrentItem(0);
        RouteFragment routeFragment = getRouteFragment();
        if (routeFragment != null) {
            routeFragment.setStartLocation(latitude, longitude);
        }
        Toast.makeText(this, R.string.origin_set, Toast.LENGTH_SHORT).show();
    }

    private void setAsDestination(double latitude, double longitude) {
        // Switch to route tab and set destination
        viewPager.setCurrentItem(0);
        RouteFragment routeFragment = getRouteFragment();
        if (routeFragment != null) {
            routeFragment.setEndLocation(latitude, longitude);
        }
        Toast.makeText(this, R.string.destination_set, Toast.LENGTH_SHORT).show();
    }

    private void viewNearbyStations(double latitude, double longitude) {
        // Switch to stations tab and load nearby stations
        viewPager.setCurrentItem(1);
        StationsFragment stationsFragment = getStationsFragment();
        if (stationsFragment != null) {
            stationsFragment.fetchNearbyStations(latitude, longitude);
        }
        Toast.makeText(this, R.string.loading_nearby_stations, Toast.LENGTH_SHORT).show();
    }

    private RouteFragment getRouteFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f0");
        if (fragment instanceof RouteFragment) {
            return (RouteFragment) fragment;
        }
        return null;
    }

    private StationsFragment getStationsFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("f1");
        if (fragment instanceof StationsFragment) {
            return (StationsFragment) fragment;
        }
        return null;
    }

    private String getCurrentLanguage() {
        // Get app language from system locale
        Locale locale = getResources().getConfiguration().locale;
        String language = locale.getLanguage();

        // MapTiler supports language codes like "en", "ar", "fr", etc.
        // Return "ar" for Arabic, "en" for everything else (default)
        if ("ar".equals(language)) {
            return "ar";
        }
        return "en";
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.route_tab);
                    break;
                case 1:
                    tab.setText(R.string.stations_tab);
                    break;
                case 2:
                    tab.setText(R.string.lines_tab);
                    break;
            }
        }).attach();
    }

    private void getCurrentLocation() {
        if (!LocationHelper.hasLocationPermission(this)) {
            LocationHelper.requestLocationPermission(this);
            return;
        }

        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                GeoPoint location = new GeoPoint(latitude, longitude);
                mapView.getController().animateTo(location);
                mapView.getController().setZoom(15.0);
                Toast.makeText(MainActivity.this,
                        getString(R.string.finding_location),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        android.widget.RadioGroup languageGroup = dialogView.findViewById(R.id.language_radio_group);
        android.widget.RadioButton englishRadio = dialogView.findViewById(R.id.radio_english);
        android.widget.RadioButton arabicRadio = dialogView.findViewById(R.id.radio_arabic);

        // Set current language selection
        String currentLang = getCurrentLanguage();
        if ("ar".equals(currentLang)) {
            arabicRadio.setChecked(true);
        } else {
            englishRadio.setChecked(true);
        }

        // Handle language change
        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newLang = checkedId == R.id.radio_arabic ? "ar" : "en";
            if (!newLang.equals(currentLang)) {
                changeLanguage(newLang);
            }
        });

        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    private void changeLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Restart activity to apply changes
        recreate();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (myLocationOverlay != null) {
                    myLocationOverlay.enableMyLocation();
                } else {
                    myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
                    myLocationOverlay.enableMyLocation();
                    mapView.getOverlays().add(myLocationOverlay);
                }
            } else {
                Toast.makeText(this, R.string.error_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public MapView getMapView() {
        return mapView;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
    }

    // ViewPager Adapter
    private static class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new RouteFragment();
                case 1:
                    return new StationsFragment();
                case 2:
                    return new LinesFragment();
                default:
                    return new RouteFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
