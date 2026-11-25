
package com.example.omolola_mayowa_s2139444;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TrafficDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_ROAD_NAME = "road_name";
    public static final String EXTRA_EVENT_TYPE = "event_type";
    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_START_TIME = "start_time";
    public static final String EXTRA_END_TIME = "end_time";
    public static final String EXTRA_DIRECTION = "direction";
    public static final String EXTRA_DELAY = "delay";
    public static final String EXTRA_COORDINATES = "coordinates";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        TextView txtDetailsTitle =
                findViewById(R.id.txtDetailsTitle);

        TextView txtRoadName =
                findViewById(R.id.txtDetailRoadName);

        TextView txtEventType =
                findViewById(R.id.txtDetailEventType);

        TextView txtDescription =
                findViewById(R.id.txtDetailDescription);

        TextView txtStartTime =
                findViewById(R.id.txtDetailStartTime);

        TextView txtEndTime =
                findViewById(R.id.txtDetailEndTime);

        TextView txtDirection =
                findViewById(R.id.txtDetailDirection);

        TextView txtDelay =
                findViewById(R.id.txtDetailDelay);

        TextView txtCoordinates =
                findViewById(R.id.txtDetailCoordinates);

        txtDetailsTitle.setText("Traffic Event Details");

        txtRoadName.setText(getEventValue(EXTRA_ROAD_NAME));
        txtEventType.setText(getEventValue(EXTRA_EVENT_TYPE));
        txtDescription.setText(getEventValue(EXTRA_DESCRIPTION));
        txtStartTime.setText(getEventValue(EXTRA_START_TIME));
        txtEndTime.setText(getEventValue(EXTRA_END_TIME));
        txtDirection.setText(getEventValue(EXTRA_DIRECTION));
        txtDelay.setText(getEventValue(EXTRA_DELAY));
        txtCoordinates.setText(getEventValue(EXTRA_COORDINATES));
    }

    private String getEventValue(String key) {

        String value = getIntent().getStringExtra(key);

        if (value == null || value.trim().isEmpty()
                || value.equals("null")) {
            return "Not available";
        }

        return value;
    }
}
