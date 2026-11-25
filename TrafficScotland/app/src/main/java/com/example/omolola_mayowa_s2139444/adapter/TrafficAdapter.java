package com.example.omolola_mayowa_s2139444.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.omolola_mayowa_s2139444.R;
import com.example.omolola_mayowa_s2139444.TrafficDetailsActivity;
import com.example.omolola_mayowa_s2139444.model.TrafficEvent;

import java.util.List;

public class TrafficAdapter extends BaseAdapter {

    private final Context context;
    private final List<TrafficEvent> events;

    public TrafficAdapter(Context context, List<TrafficEvent> events) {
        this.context = context;
        this.events = events;
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public Object getItem(int position) {
        return events.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Update the displayed events when the search results change
    public void updateEvents(List<TrafficEvent> updatedEvents) {
        events.clear();
        events.addAll(updatedEvents);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_traffic_event, parent, false);
        }

        TrafficEvent event = events.get(position);

        // Display the appropriate event icon
        TextView txtEventIcon =
                convertView.findViewById(R.id.txtEventIcon);

        String eventType = event.getEventType();

        if (eventType != null &&
                (eventType.toLowerCase().contains("roadwork")
                        || eventType.toLowerCase().contains("maintenance"))) {

            txtEventIcon.setText("🚧");

        } else {
            txtEventIcon.setText("⚠️");
        }

        // Display event information
        TextView txtRoadName =
                convertView.findViewById(R.id.txtRoadName);

        TextView txtEventType =
                convertView.findViewById(R.id.txtEventType);

        TextView txtDescription =
                convertView.findViewById(R.id.txtDescription);

        txtRoadName.setText(getValue(event.getRoadName()));
        txtEventType.setText(getValue(event.getEventType()));
        txtDescription.setText(getValue(event.getDescription()));

        // Display color-coded delay information
        TextView txtDelayBadge =
                convertView.findViewById(R.id.txtDelayBadge);

        String delay = event.getDelayType();

        if (delay != null && delay.equalsIgnoreCase("shortDelays")) {

            txtDelayBadge.setText("SHORT DELAYS");
            txtDelayBadge.setBackgroundColor(
                    android.graphics.Color.rgb(46, 125, 50));

        } else if (delay != null && delay.equalsIgnoreCase("longDelays")) {

            txtDelayBadge.setText("LONG DELAYS");
            txtDelayBadge.setBackgroundColor(
                    android.graphics.Color.rgb(198, 40, 40));

        } else {

            txtDelayBadge.setText("DELAY INFORMATION UNAVAILABLE");
            txtDelayBadge.setBackgroundColor(
                    android.graphics.Color.GRAY);
        }

        txtDelayBadge.setTextColor(android.graphics.Color.WHITE);

        // Open the full event details when a row is tapped
        convertView.setOnClickListener(v -> {

            Intent intent = new Intent(
                    context,
                    TrafficDetailsActivity.class
            );

            intent.putExtra(
                    TrafficDetailsActivity.EXTRA_ROAD_NAME,
                    event.getRoadName()
            );

            intent.putExtra(
                    TrafficDetailsActivity.EXTRA_EVENT_TYPE,
                    event.getEventType()
            );

            intent.putExtra(
                    TrafficDetailsActivity.EXTRA_DESCRIPTION,
                    event.getDescription()
            );

            intent.putExtra(
                    TrafficDetailsActivity.EXTRA_START_TIME,
                    event.getStartTime()
            );

            intent.putExtra(
                    TrafficDetailsActivity.EXTRA_END_TIME,
                    event.getEndTime()
            );

            intent.putExtra(
                    TrafficDetailsActivity.EXTRA_DIRECTION,
                    event.getDirection()
            );

            intent.putExtra(
                    TrafficDetailsActivity.EXTRA_DELAY,
                    event.getDelayType()
            );

            intent.putExtra(
                    TrafficDetailsActivity.EXTRA_COORDINATES,
                    getCoordinates(event)
            );

            context.startActivity(intent);
        });

        return convertView;
    }

    private String getValue(String value) {

        if (value == null || value.trim().isEmpty()
                || value.equals("null")) {
            return "Not available";
        }

        return value;
    }

    private String getCoordinates(TrafficEvent event) {

        String fromLatitude = event.getFromLatitude();
        String fromLongitude = event.getFromLongitude();
        String toLatitude = event.getToLatitude();
        String toLongitude = event.getToLongitude();

        if (isMissing(fromLatitude) || isMissing(fromLongitude)) {
            return "Not available";
        }

        String coordinates = "Start: "
                + fromLatitude + ", " + fromLongitude;

        if (!isMissing(toLatitude) && !isMissing(toLongitude)) {
            coordinates += "\nEnd: "
                    + toLatitude + ", " + toLongitude;
        }

        return coordinates;
    }

    private boolean isMissing(String value) {
        return value == null || value.trim().isEmpty()
                || value.equals("null");
    }
}
