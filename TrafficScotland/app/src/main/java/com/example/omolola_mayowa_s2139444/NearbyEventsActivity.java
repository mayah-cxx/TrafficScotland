package com.example.omolola_mayowa_s2139444;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.omolola_mayowa_s2139444.adapter.TrafficAdapter;
import com.example.omolola_mayowa_s2139444.model.TrafficEvent;
import com.example.omolola_mayowa_s2139444.parser.TrafficParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NearbyEventsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final long REFRESH_INTERVAL = 60 * 1000;

    private TextView txtNearbyStatus;
    private ListView listNearbyEvents;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private List<TrafficEvent> allEvents = new ArrayList<>();

    private Location userLocation;
    private boolean trafficEventsLoaded = false;
    private boolean permissionDenied = false;

    private TrafficAdapter adapter;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler refreshHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadTrafficEvents();
            refreshHandler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_events);

        txtNearbyStatus = findViewById(R.id.txtNearbyStatus);
        listNearbyEvents = findViewById(R.id.listNearbyEvents);

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        loadTrafficEvents();
        checkLocationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();

        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(
                refreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    protected void onStop() {
        super.onStop();

        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // Check whether location permission has been granted.
    private boolean hasLocationPermission() {

        boolean finePermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarsePermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        return finePermission || coarsePermission;
    }

    private void checkLocationPermission() {

        if (hasLocationPermission()) {
            permissionDenied = false;
            getUserLocation();

        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );
        }
    }

    // Handle the user's permission decision.
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            if (hasLocationPermission()) {

                permissionDenied = false;
                getUserLocation();

            } else {

                permissionDenied = true;
                showPermissionDeniedMessage();
            }
        }
    }

    // Show a clear explanation when location permission is denied.
    private void showPermissionDeniedMessage() {

        txtNearbyStatus.setText(
                "Location permission is required to display nearby traffic events. "
                        + "Please enable location access in your app settings."
        );

        listNearbyEvents.setVisibility(ListView.GONE);

        new AlertDialog.Builder(this)
                .setTitle("Location Permission Required")
                .setMessage(
                        "Traffic Scotland needs access to your device's "
                                + "location to calculate distances to nearby "
                                + "incidents and roadworks."
                )
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    openAppSettings();
                })
                .setNegativeButton("Not Now", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    // Open this application's Android permission settings.
    private void openAppSettings() {

        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null)
        );

        startActivity(intent);
    }

    // Check again when the user returns from Android settings.
    @Override
    protected void onResume() {
        super.onResume();

        if (txtNearbyStatus != null
                && permissionDenied
                && hasLocationPermission()) {

            permissionDenied = false;
            getUserLocation();
        }
    }

    // Retrieve the device's location.
    private void getUserLocation() {

        if (!hasLocationPermission()) {
            permissionDenied = true;
            showPermissionDeniedMessage();
            return;
        }

        txtNearbyStatus.setText("Finding your location...");

        try {

            Location bestLocation = null;

            List<String> providers =
                    locationManager.getProviders(true);

            for (String provider : providers) {

                Location location =
                        locationManager.getLastKnownLocation(provider);

                if (location != null &&
                        (bestLocation == null ||
                                location.getTime() > bestLocation.getTime())) {

                    bestLocation = location;
                }
            }

            if (bestLocation != null) {

                userLocation = bestLocation;
                tryDisplayNearbyEvents();
                return;
            }

            requestNewLocation();

        } catch (SecurityException e) {

            permissionDenied = true;
            showPermissionDeniedMessage();
        }
    }

    // Request a fresh location from the device.
    private void requestNewLocation() {

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(
                    @NonNull Location location) {

                locationManager.removeUpdates(this);

                userLocation = location;

                tryDisplayNearbyEvents();
            }
        };

        try {

            boolean providerAvailable = false;

            if (hasLocationPermission()) {

                List<String> providers =
                        locationManager.getProviders(true);

                for (String provider : providers) {

                    locationManager.requestLocationUpdates(
                            provider,
                            0,
                            0,
                            locationListener
                    );

                    providerAvailable = true;
                }
            }

            if (!providerAvailable) {

                txtNearbyStatus.setText(
                        "Please enable location services on your device."
                );
            }

        } catch (SecurityException e) {

            permissionDenied = true;
            showPermissionDeniedMessage();
        }
    }

    // Load incidents and roadworks from both XML files.
    private void loadTrafficEvents() {

        executor.execute(() -> {

            try {

                List<TrafficEvent> combinedEvents =
                        new ArrayList<>();

                String incidentsXml =
                        readXmlFile(R.raw.unplanned_content);

                String roadworksXml =
                        readXmlFile(R.raw.roadworks_content);

                List<TrafficEvent> incidents =
                        TrafficParser.parse(incidentsXml);

                List<TrafficEvent> roadworks =
                        TrafficParser.parse(roadworksXml);

                if (incidents != null) {
                    combinedEvents.addAll(incidents);
                }

                if (roadworks != null) {
                    combinedEvents.addAll(roadworks);
                }

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    allEvents = combinedEvents;
                    trafficEventsLoaded = true;

                    tryDisplayNearbyEvents();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()
                            && !trafficEventsLoaded) {

                        txtNearbyStatus.setText(
                                "Unable to load traffic events."
                        );
                    }
                });
            }
        });
    }

    // Read an XML resource.
    private String readXmlFile(int resourceId) throws Exception {

        try (InputStream inputStream =
                     getResources().openRawResource(resourceId);

             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toString(
                    StandardCharsets.UTF_8.name()
            );
        }
    }

    // Calculate distances and display the closest events first.
    private void displayNearbyEvents(Location userLocation) {

        if (allEvents.isEmpty()) {

            txtNearbyStatus.setText(
                    "No traffic events are available."
            );
            return;
        }

        List<TrafficEvent> nearbyEvents =
                new ArrayList<>();

        for (TrafficEvent event : allEvents) {

            try {

                double latitude =
                        Double.parseDouble(event.getFromLatitude());

                double longitude =
                        Double.parseDouble(event.getFromLongitude());

                if (!Double.isFinite(latitude)
                        || !Double.isFinite(longitude)
                        || latitude < -90 || latitude > 90
                        || longitude < -180 || longitude > 180) {
                    continue;
                }

                Location eventLocation = new Location("event");

                eventLocation.setLatitude(latitude);
                eventLocation.setLongitude(longitude);

                event.setDistanceFromUser(
                        userLocation.distanceTo(eventLocation)
                );

                nearbyEvents.add(event);

            } catch (Exception ignored) {
                // Ignore events without valid coordinates.
            }
        }

        nearbyEvents.sort(
                Comparator.comparingDouble(
                        TrafficEvent::getDistanceFromUser
                )
        );

        if (nearbyEvents.isEmpty()) {

            txtNearbyStatus.setText(
                    "No traffic events have usable location coordinates."
            );

            listNearbyEvents.setAdapter(null);
            return;
        }

        if (adapter == null) {

            adapter = new TrafficAdapter(
                    this,
                    new ArrayList<>(nearbyEvents)
            );

            listNearbyEvents.setAdapter(adapter);

        } else {

            adapter.updateEvents(nearbyEvents);
        }

        listNearbyEvents.setVisibility(ListView.VISIBLE);

        txtNearbyStatus.setText(
                "Found " + nearbyEvents.size()
                        + " traffic events with coordinates. "
                        + "Showing closest events first."
        );
    }

    // Wait until traffic data and location are available.
    private void tryDisplayNearbyEvents() {

        // Never overwrite the permission-denied message.
        if (!hasLocationPermission() || permissionDenied) {
            return;
        }

        if (!trafficEventsLoaded) {
            txtNearbyStatus.setText("Loading traffic events...");
            return;
        }

        if (userLocation == null) {
            txtNearbyStatus.setText("Waiting for your location...");
            return;
        }

        displayNearbyEvents(userLocation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        refreshHandler.removeCallbacks(refreshRunnable);

        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException ignored) {
            }
        }

        executor.shutdownNow();
    }
}
