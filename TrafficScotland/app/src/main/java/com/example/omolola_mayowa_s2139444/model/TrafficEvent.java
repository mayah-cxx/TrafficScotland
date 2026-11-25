package com.example.omolola_mayowa_s2139444.model;

public class TrafficEvent {

    private String id;
    private String eventType;
    private String description;
    private String roadName;
    private String startTime;
    private String endTime;
    private String delayType;
    private String direction;

    // Location coordinates
    private String fromLatitude;
    private String fromLongitude;
    private String toLatitude;
    private String toLongitude;
    private float distanceFromUser;

    public TrafficEvent(String id, String eventType,
                        String description, String roadName,
                        String startTime, String endTime,
                        String delayType, String direction) {

        this(id, eventType, description, roadName,
                startTime, endTime, delayType, direction,
                null, null, null, null);
    }

    public TrafficEvent(String id, String eventType,
                        String description, String roadName,
                        String startTime, String endTime,
                        String delayType, String direction,
                        String fromLatitude, String fromLongitude,
                        String toLatitude, String toLongitude) {

        this.id = id;
        this.eventType = eventType;
        this.description = description;
        this.roadName = roadName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.delayType = delayType;
        this.direction = direction;

        this.fromLatitude = fromLatitude;
        this.fromLongitude = fromLongitude;
        this.toLatitude = toLatitude;
        this.toLongitude = toLongitude;
    }

    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public String getRoadName() {
        return roadName;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getDelayType() {
        return delayType;
    }

    public String getDirection() {
        return direction;
    }

    public String getFromLatitude() {
        return fromLatitude;
    }

    public String getFromLongitude() {
        return fromLongitude;
    }

    public String getToLatitude() {
        return toLatitude;
    }

    public String getToLongitude() {
        return toLongitude;
    }
    public void setDistanceFromUser(float distance) {
        this.distanceFromUser = distance;
    }

    public float getDistanceFromUser() {
        return distanceFromUser;
    }
}
