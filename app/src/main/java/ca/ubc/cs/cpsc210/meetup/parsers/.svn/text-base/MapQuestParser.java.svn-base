package ca.ubc.cs.cpsc210.meetup.parsers;

/**
 * Created by Ryan on 15-03-18.
 */

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class MapQuestParser {

    double lat;
    double lon;

    public List<GeoPoint> parseLatLon(JSONTokener input) {

        List<GeoPoint> geoPointPath = new ArrayList<GeoPoint>();

        try {
            JSONObject jInput = new JSONObject(input);
            JSONObject route = jInput.getJSONObject("route");
            JSONObject shape = route.getJSONObject("shape");
            JSONArray routePoints = shape.getJSONArray("shapePoints");

            for (int i = 0 ; (i < routePoints.length()) ; i+=2) {

                // i will get the index of the lat, j will access lon.

                int j = i + 1;

                double jLat = routePoints.getDouble(i);
                double jLon = routePoints.getDouble(j);

                GeoPoint newGeoPoint = new GeoPoint(jLat, jLon);

                geoPointPath.add(newGeoPoint);
            }
            return geoPointPath;
        }
        catch (JSONException e) {
            // Do
            Log.d("e", "JSONException");
        }

        return geoPointPath;
    }

}
