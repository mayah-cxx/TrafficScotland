
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

public class TrafficListActivity extends AppCompatActivity {

    public static final String EXTRA_RESOURCE_ID = "resource_id";
    public static final String EXTRA_TITLE = "screen_title";

    private static final long REFRESH_INTERVAL = 60 * 1000;

    private ListView listTrafficEvents;
    private TextView txtListTitle;
    private TextView txtEmptyMessage;
    private EditText edtSearchRoad;

    private List<TrafficEvent> allEvents = new ArrayList<>();
    private TrafficAdapter adapter;

    private int resourceId;
    private boolean hasLoadedSuccessfully = false;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler refreshHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadEvents(resourceId);

            // Schedule the next refresh after 60 seconds.
            refreshHandler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        listTrafficEvents = findViewById(R.id.listTrafficEvents);
        txtListTitle = findViewById(R.id.txtListTitle);
        txtEmptyMessage = findViewById(R.id.txtEmptyMessage);
        edtSearchRoad = findViewById(R.id.edtSearchRoad);

        resourceId = getIntent().getIntExtra(
                EXTRA_RESOURCE_ID, 0);

        String title = getIntent().getStringExtra(EXTRA_TITLE);

        if (title == null) {
            title = "Traffic Events";
        }

        txtListTitle.setText(title);

        if (resourceId == 0) {
            showError();
            return;
        }

        // Add the search listener only once.
        edtSearchRoad.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Load the data when the screen first opens.
        loadEvents(resourceId);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (resourceId != 0) {
            // Begin automatic refreshing while the screen is visible.
            refreshHandler.removeCallbacks(refreshRunnable);
            refreshHandler.postDelayed(
                    refreshRunnable, REFRESH_INTERVAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop scheduled refreshing when the screen is not visible.
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void loadEvents(int resourceId) {

        executor.execute(() -> {

            try {
                String xml = readXmlFile(resourceId);

                List<TrafficEvent> events =
                        TrafficParser.parse(xml);

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        displayEvents(events);
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()
                            && !hasLoadedSuccessfully) {
                        showError();
                    }
                });
            }
        });
    }

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

    private void displayEvents(List<TrafficEvent> events) {

        if (events == null) {
            if (!hasLoadedSuccessfully) {
                showError();
            }
            return;
        }

        // Replace the existing event data.
        allEvents = new ArrayList<>(events);
        hasLoadedSuccessfully = true;

        // Create the adapter only on the first successful load.
        if (adapter == null) {

            adapter = new TrafficAdapter(
                    this,
                    new ArrayList<>(allEvents));

            listTrafficEvents.setAdapter(adapter);

        } else {

            // Update the existing adapter during refreshes.
            adapter.updateEvents(new ArrayList<>(allEvents));
        }

        // Reapply the current search after every refresh.
        filterEvents(edtSearchRoad.getText().toString());
    }

    private void filterEvents(String query) {

        if (adapter == null) {
            return;
        }

        List<TrafficEvent> filteredEvents = new ArrayList<>();

        String searchQuery =
                query.trim().toLowerCase(Locale.ROOT);

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

            listTrafficEvents.setVisibility(View.GONE);
            txtEmptyMessage.setVisibility(View.VISIBLE);

            if (searchQuery.isEmpty()) {
                txtEmptyMessage.setText(
                        "No traffic events are currently available.");
            } else {
                txtEmptyMessage.setText(
                        "No events found for that road.");
            }

        } else {

            listTrafficEvents.setVisibility(View.VISIBLE);
            txtEmptyMessage.setVisibility(View.GONE);
        }
    }

    private void showError() {

        listTrafficEvents.setVisibility(View.GONE);
        txtEmptyMessage.setVisibility(View.VISIBLE);

        txtEmptyMessage.setText(
                "No traffic events could be loaded.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        refreshHandler.removeCallbacks(refreshRunnable);
        executor.shutdownNow();
    }
}
