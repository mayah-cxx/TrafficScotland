
package com.example.omolola_mayowa_s2139444;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnIncidents;
    private Button btnRoadworks;
    private Button btnSearch;
    private Button btnNearby;
    private Button btnMap;

    private TextView txtLastUpdated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.enableEdgeToEdge(getWindow());

        setContentView(R.layout.activity_main);

        // Connect homepage components
        btnIncidents = findViewById(R.id.btnIncidents);
        btnRoadworks = findViewById(R.id.btnRoadworks);
        btnSearch = findViewById(R.id.btnSearch);
        btnNearby = findViewById(R.id.btnNearby);
        btnMap = findViewById(R.id.btnMap);

        txtLastUpdated = findViewById(R.id.txtLastUpdated);

        // Open Traffic Incidents list
        btnIncidents.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    TrafficListActivity.class
            );

            intent.putExtra(
                    TrafficListActivity.EXTRA_RESOURCE_ID,
                    R.raw.unplanned_content
            );

            intent.putExtra(
                    TrafficListActivity.EXTRA_TITLE,
                    "Traffic Incidents"
            );

            startActivity(intent);
        });

        // Open Current Roadworks list
        btnRoadworks.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    TrafficListActivity.class
            );

            intent.putExtra(
                    TrafficListActivity.EXTRA_RESOURCE_ID,
                    R.raw.roadworks_content
            );

            intent.putExtra(
                    TrafficListActivity.EXTRA_TITLE,
                    "Current Roadworks"
            );

            startActivity(intent);
        });

        // Open the dedicated road search screen
        btnSearch.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    SearchRoadActivity.class
            );

            startActivity(intent);
        });

        // Nearby Events
        btnNearby.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NearbyEventsActivity.class);
            startActivity(intent);


        });

        // Traffic Map
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TrafficMapActivity.class);
            startActivity(intent);
        });

        // Display sample data update information
        txtLastUpdated.setText(
                "Traffic information loaded from sample data"
        );
    }
}
