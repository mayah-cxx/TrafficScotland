package com.example.omolola_mayowa_s2139444;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.omolola_mayowa_s2139444.adapter.TrafficAdapter;
import com.example.omolola_mayowa_s2139444.model.TrafficEvent;
import com.example.omolola_mayowa_s2139444.parser.TrafficParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchRoadActivity extends AppCompatActivity {

    private static final long REFRESH_INTERVAL = 60 * 1000;

    private EditText edtSearchRoad;
    private ListView listSearchResults;
    private TextView txtSearchEmpty;

    private List<TrafficEvent> allEvents = new ArrayList<>();
    private TrafficAdapter adapter;

    private boolean hasLoadedSuccessfully = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler refreshHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadAllEvents();

            // Schedule the next refresh after 60 seconds.
            refreshHandler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_road);

        edtSearchRoad = findViewById(R.id.edtSearchRoad);
        listSearchResults = findViewById(R.id.listSearchResults);
        txtSearchEmpty = findViewById(R.id.txtSearchEmpty);

        txtSearchEmpty.setText("Loading traffic information...");

        // Add the search listener once.
        edtSearchRoad.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

                if (hasLoadedSuccessfully) {
                    searchEvents(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Load incidents and roadworks initially.
        loadAllEvents();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start automatic refreshing while the screen is visible.
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(
                refreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop refreshing when the screen is not visible.
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // Load both XML sample files in the background.
    private void loadAllEvents() {

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

                    // Replace the stored event information.
                    allEvents = combinedEvents;
                    hasLoadedSuccessfully = true;

                    // Create the adapter only on the first load.
                    if (adapter == null) {

                        adapter = new TrafficAdapter(
                                this,
                                new ArrayList<>());

                        listSearchResults.setAdapter(adapter);
                    }

                    // Reapply the current search after refreshing.
                    searchEvents(
                            edtSearchRoad.getText().toString());

                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()
                            && !hasLoadedSuccessfully) {

                        txtSearchEmpty.setText(
                                "Unable to load traffic information.");
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
                    StandardCharsets.UTF_8.name());
        }
    }

    // Search incidents and roadworks by road name.
    private void searchEvents(String query) {

        if (adapter == null) {
            return;
        }

        List<TrafficEvent> filteredEvents =
                new ArrayList<>();

        String searchQuery =
                query.trim().toLowerCase(Locale.ROOT);

        if (searchQuery.isEmpty()) {

            adapter.updateEvents(filteredEvents);

            listSearchResults.setVisibility(View.GONE);
            txtSearchEmpty.setVisibility(View.VISIBLE);

            txtSearchEmpty.setText(
                    "Enter a road name to search incidents and roadworks.");

            return;
        }

        for (TrafficEvent event : allEvents) {

            String roadName = event.getRoadName();

            if (roadName != null &&
                    roadName.toLowerCase(Locale.ROOT)
                            .contains(searchQuery)) {

                filteredEvents.add(event);
            }
        }

        adapter.updateEvents(filteredEvents);

        if (filteredEvents.isEmpty()) {

            listSearchResults.setVisibility(View.GONE);
            txtSearchEmpty.setVisibility(View.VISIBLE);

            txtSearchEmpty.setText(
                    "No incidents or roadworks found for that road.");

        } else {

            listSearchResults.setVisibility(View.VISIBLE);
            txtSearchEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        refreshHandler.removeCallbacks(refreshRunnable);
        executor.shutdownNow();
    }
}
