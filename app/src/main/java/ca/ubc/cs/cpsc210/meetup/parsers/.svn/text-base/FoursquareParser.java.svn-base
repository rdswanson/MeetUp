package ca.ubc.cs.cpsc210.meetup.parsers;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import ca.ubc.cs.cpsc210.meetup.model.EatingPlace;
import ca.ubc.cs.cpsc210.meetup.model.PlaceFactory;
import ca.ubc.cs.cpsc210.meetup.util.LatLon;

/**
 * Created by Ryan on 15-03-24.
 */
public class FoursquareParser {

    /**
     * Parse the given JSON response from FourSquare for the provided food venues.
     * @param rawJSON - JSON text to parse
     *
     */
    public int parseFoodVenues(String rawJSON) {

        PlaceFactory placeFactory = PlaceFactory.getInstance();

        JSONTokener jsonTokener = new JSONTokener(rawJSON);
        int numberOfPlacesFound = 0;

        try {
            JSONObject jObject = new JSONObject(jsonTokener);
            JSONObject response = jObject.getJSONObject("response");
            JSONArray groups = response.getJSONArray("groups");
            JSONObject group = groups.getJSONObject(0);
            JSONArray items = group.getJSONArray("items");

            for (int i = 0 ; i < items.length() ; i++) {

                JSONObject item = items.getJSONObject(i);
                JSONObject venue = item.getJSONObject("venue");
                String venueName = venue.getString("name");
                JSONObject location = venue.getJSONObject("location");
                double lat = location.getDouble("lat");
                double lon = location.getDouble("lng");

                JSONArray categories = venue.getJSONArray("categories");
                JSONObject category = categories.getJSONObject(0);
                String categoryShortName = category.getString("shortName");

                LatLon latLon = new LatLon(lat, lon);

                EatingPlace eatingPlace = new EatingPlace(venueName, latLon);
                eatingPlace.addTag(categoryShortName);

                placeFactory.add(eatingPlace);
                numberOfPlacesFound++;
            }

            return numberOfPlacesFound;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return numberOfPlacesFound;
    }
}
