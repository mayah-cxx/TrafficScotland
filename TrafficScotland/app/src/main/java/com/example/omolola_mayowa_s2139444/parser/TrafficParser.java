package com.example.omolola_mayowa_s2139444.parser;

import com.example.omolola_mayowa_s2139444.model.TrafficEvent;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

public class TrafficParser {

    private static final String XSI_NAMESPACE =
            "http://www.w3.org/2001/XMLSchema-instance";

    public static List<TrafficEvent> parse(String xml)
            throws XmlPullParserException, IOException {

        List<TrafficEvent> events = new ArrayList<>();

        if (xml == null || xml.trim().isEmpty()) {
            return events;
        }

        XmlPullParserFactory factory =
                XmlPullParserFactory.newInstance();

        factory.setNamespaceAware(true);

        XmlPullParser parser = factory.newPullParser();

        parser.setInput(new StringReader(xml));

        ArrayList<String> path = new ArrayList<>();

        EventBuilder currentEvent = null;

        int event = parser.getEventType();

        while (event != XmlPullParser.END_DOCUMENT) {

            if (event == XmlPullParser.START_TAG) {

                String tag = parser.getName();

                path.add(tag);

                // Start reading a new traffic event
                if ("situationRecord".equals(tag)) {

                    currentEvent = new EventBuilder();

                    currentEvent.id =
                            parser.getAttributeValue(null, "id");

                    currentEvent.eventType =
                            parser.getAttributeValue(
                                    XSI_NAMESPACE, "type");
                }

            } else if (event == XmlPullParser.TEXT) {

                if (currentEvent != null) {

                    String value = parser.getText().trim();

                    if (!value.isEmpty()) {

                        String currentPath =
                                String.join("/", path);

                        // Event description
                        if (currentPath.endsWith(
                                "generalPublicComment/comment/values/value")) {

                            currentEvent.description = value;
                        }

                        // Road name from the DATEX II ILC field
                        else if (currentPath.endsWith(
                                "ilc/descriptor/values/value")) {

                            if (currentEvent.roadName == null) {
                                currentEvent.roadName = value;
                            }
                        }

                        // Fallback road name from a link name
                        else if (currentPath.endsWith(
                                "name/descriptor/values/value")) {

                            currentEvent.pendingName = value;
                        }

                        else if (currentPath.endsWith(
                                "tpegOtherPointDescriptorType")) {

                            if ("linkName".equals(value)
                                    && currentEvent.roadName == null) {

                                currentEvent.roadName =
                                        currentEvent.pendingName;
                            }
                        }

                        // Direction
                        else if (currentPath.endsWith(
                                "tpegDirection")) {

                            currentEvent.direction = value;
                        }

                        // Validity dates
                        else if (currentPath.endsWith(
                                "overallStartTime")) {

                            currentEvent.startTime = value;
                        }

                        else if (currentPath.endsWith(
                                "overallEndTime")) {

                            currentEvent.endTime = value;
                        }

                        // Roadwork delay information
                        else if (currentPath.endsWith(
                                "delaysType")) {

                            currentEvent.delayType = value;
                        }

                        // Specific event category
                        else if (currentPath.endsWith(
                                "roadOrCarriagewayOrLaneManagementType")
                                || currentPath.endsWith(
                                "abnormalTrafficType")
                                || currentPath.endsWith(
                                "roadMaintenanceType")) {

                            currentEvent.eventType = value;
                        }

                        // Starting coordinates
                        else if (currentPath.endsWith(
                                "from/pointCoordinates/latitude")) {

                            currentEvent.fromLatitude = value;
                        }

                        else if (currentPath.endsWith(
                                "from/pointCoordinates/longitude")) {

                            currentEvent.fromLongitude = value;
                        }

                        // Ending coordinates
                        else if (currentPath.endsWith(
                                "to/pointCoordinates/latitude")) {

                            currentEvent.toLatitude = value;
                        }

                        else if (currentPath.endsWith(
                                "to/pointCoordinates/longitude")) {

                            currentEvent.toLongitude = value;
                        }
                    }
                }

            } else if (event == XmlPullParser.END_TAG) {

                String tag = parser.getName();

                // Finish the current traffic event
                if ("situationRecord".equals(tag)
                        && currentEvent != null) {

                    events.add(currentEvent.build());

                    currentEvent = null;
                }

                if (!path.isEmpty()) {
                    path.remove(path.size() - 1);
                }
            }

            event = parser.next();
        }

        return events;
    }

    // Temporary holder for information being read from one XML record
    private static class EventBuilder {

        String id;
        String eventType;
        String description;
        String roadName;
        String pendingName;
        String startTime;
        String endTime;
        String delayType;
        String direction;

        String fromLatitude;
        String fromLongitude;
        String toLatitude;
        String toLongitude;

        TrafficEvent build() {

            return new TrafficEvent(
                    id,
                    eventType,
                    description,
                    roadName,
                    startTime,
                    endTime,
                    delayType,
                    direction,
                    fromLatitude,
                    fromLongitude,
                    toLatitude,
                    toLongitude
            );
        }
    }
}
