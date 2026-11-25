package com.example.omolola_mayowa_s2139444;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.omolola_mayowa_s2139444.model.TrafficEvent;
import com.example.omolola_mayowa_s2139444.parser.TrafficParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficMapActivity extends AppCompatActivity {

    private WebView trafficMapWebView;
    private TextView txtMapStatus;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler refreshHandler =
            new Handler(Looper.getMainLooper());

    private static final long REFRESH_INTERVAL = 60 * 1000;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadTrafficEvents();
            refreshHandler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

    private List<TrafficEvent> allEvents = new ArrayList<>();

    private boolean mapPageLoaded = false;
    private boolean trafficDataLoaded = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_map);

        trafficMapWebView = findViewById(R.id.trafficMapWebView);
        txtMapStatus = findViewById(R.id.txtMapStatus);

        trafficMapWebView.getSettings().setJavaScriptEnabled(true);

        trafficMapWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                mapPageLoaded = true;
                displayTrafficEvents();
            }
        });

        trafficMapWebView.loadUrl(
                "file:///android_asset/traffic_map.html"
        );

        loadTrafficEvents();
    }

    // Start automatic refresh while the map screen is visible
    @Override
    protected void onStart() {
        super.onStart();

        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    // Stop automatic refresh when leaving the map screen
    @Override
    protected void onStop() {
        super.onStop();

        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // Load incidents and roadworks from the XML files
    private void loadTrafficEvents() {

        if (isFinishing() || isDestroyed()) {
            return;
        }

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
                    trafficDataLoaded = true;

                    displayTrafficEvents();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        txtMapStatus.setText(
                                "Unable to load traffic data."
                        );
                    }
                });
            }
        });
    }

    // Read an XML resource
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

    // Send the traffic events to the Leaflet map
    private void displayTrafficEvents() {

        if (!mapPageLoaded || !trafficDataLoaded) {
            return;
        }

        try {

            JSONArray eventsArray = new JSONArray();

            int eventsWithCoordinates = 0;

            for (TrafficEvent event : allEvents) {

                try {

                    String latitude = event.getFromLatitude();
                    String longitude = event.getFromLongitude();

                    if (latitude == null || longitude == null) {
                        continue;
                    }

                    double lat = Double.parseDouble(latitude);
                    double lon = Double.parseDouble(longitude);

                    if (!Double.isFinite(lat) ||
                            !Double.isFinite(lon) ||
                            lat < -90 || lat > 90 ||
                            lon < -180 || lon > 180) {
                        continue;
                    }

                    JSONObject eventObject = new JSONObject();

                    eventObject.put("latitude", lat);
                    eventObject.put("longitude", lon);

                    eventObject.put(
                            "roadName",
                            safeValue(event.getRoadName())
                    );

                    eventObject.put(
                            "eventType",
                            safeValue(event.getEventType())
                    );

                    eventObject.put(
                            "description",
                            safeValue(event.getDescription())
                    );

                    eventsArray.put(eventObject);

                    eventsWithCoordinates++;

                } catch (Exception ignored) {
                    // Skip events with invalid coordinates
                }
            }

            String javascript =
                    "clearTrafficEvents();" +
                            "var events = " + eventsArray.toString() + ";" +
                            "events.forEach(function(event) {" +
                            "addTrafficEvent(event);" +
                            "});" +
                            "finishLoadingEvents();";

            trafficMapWebView.evaluateJavascript(
                    javascript,
                    null
            );

            txtMapStatus.setText(
                    "Displaying " + eventsWithCoordinates +
                            " traffic events on the map."
            );

        } catch (Exception e) {

            txtMapStatus.setText(
                    "Unable to display traffic events."
            );
        }
    }

    // Handle missing text values
    private String safeValue(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Information unavailable";
        }

        return value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        refreshHandler.removeCallbacks(refreshRunnable);

        executor.shutdownNow();

        if (trafficMapWebView != null) {
            trafficMapWebView.stopLoading();
            trafficMapWebView.destroy();
        }
    }
}
