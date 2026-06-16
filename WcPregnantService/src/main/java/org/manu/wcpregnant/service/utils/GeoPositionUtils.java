package org.manu.wcpregnant.service.utils;


import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;

public class GeoPositionUtils {

    // corrected parameter order: lat1, lon1, lat2, lon2, el1, el2
    public static double distance(double lat1, double lon1, double lat2, double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public static double distance(GeoPosition p1, GeoPosition p2) {
        if (p1 == null || p2 == null || p1.getLatitude() == null || p1.getLongitude() == null
                || p2.getLatitude() == null || p2.getLongitude() == null) {
            // treat missing data as very far
            return Double.MAX_VALUE;
        }
        return distance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), 0, 0);
    }

    public static void setDistance(Toilet currentToilet, GeoPosition currentPosition) {
        if (currentToilet == null) {
            return;
        }
        double d = distance(currentToilet.getPosition(), currentPosition);
        // wrap primitive in Double for DTO
        currentToilet.setDistance(d == Double.MAX_VALUE ? null : d);
    }

    public static int compareWithDistance(Toilet t1, Toilet t2) {
        double d1 = t1 == null || t1.getDistance() == null ? Double.MAX_VALUE : t1.getDistance();
        double d2 = t2 == null || t2.getDistance() == null ? Double.MAX_VALUE : t2.getDistance();
        return Double.compare(d1, d2);

    }
}
