package ca.ubc.cs.cpsc210.meetup.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONTokener;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.PathOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ca.ubc.cs.cpsc210.meetup.R;
import ca.ubc.cs.cpsc210.meetup.model.BreakBlock;
import ca.ubc.cs.cpsc210.meetup.model.Building;
import ca.ubc.cs.cpsc210.meetup.model.Course;
import ca.ubc.cs.cpsc210.meetup.model.CourseFactory;
import ca.ubc.cs.cpsc210.meetup.model.Place;
import ca.ubc.cs.cpsc210.meetup.model.PlaceFactory;
import ca.ubc.cs.cpsc210.meetup.model.Schedule;
import ca.ubc.cs.cpsc210.meetup.model.Section;
import ca.ubc.cs.cpsc210.meetup.model.Student;
import ca.ubc.cs.cpsc210.meetup.model.StudentManager;
import ca.ubc.cs.cpsc210.meetup.parsers.FoursquareParser;
import ca.ubc.cs.cpsc210.meetup.parsers.MapQuestParser;
import ca.ubc.cs.cpsc210.meetup.parsers.MeetUpParser;
import ca.ubc.cs.cpsc210.meetup.util.LatLon;
import ca.ubc.cs.cpsc210.meetup.util.SchedulePlot;

/**
 * Fragment holding the map in the UI.
 */
public class MapDisplayFragment extends Fragment {

    /**
     * Log tag for LogCat messages
     */
    private final static String LOG_TAG = "MapDisplayFragment";

    /**
     * Preference manager to access user preferences
     */
    private SharedPreferences sharedPreferences;

    /**
     * String to know whether we are dealing with MWF or TR schedule.
     * You will need to update this string based on the settings dialog at appropriate
     * points in time. See the project page for details on how to access
     * the value of a setting.
     */
    private String activeDay = "MWF";

    /**
     * My path colour
     */
    private final static String MY_COLOUR = "#DC143C";


    /**
     * Random student path colour
     */
    private final static String STUDENT_COLOUR = "#6495ED";

    /**
     * A central location in campus that might be handy.
     */
    private final static GeoPoint UBC_MARTHA_PIPER_FOUNTAIN = new GeoPoint(49.264865,
            -123.252782);

    /**
     * Meetup Service URL
     * CPSC 210 Students: Complete the string.
     */
    private final String getStudentURL = "http://kramer.nss.cs.ubc.ca:8081/getStudent/";

    /**
     * FourSquare URLs. You must complete the client_id and client_secret with values
     * you sign up for.
     */
    private static String FOUR_SQUARE_URL = "https://api.foursquare.com/v2/venues/explore";
    private static String FOUR_SQUARE_CLIENT_ID = "RCIDQNOYJWWXQAY3IIDGM5SVSFJBGE45GEGU33ULRGP2ITFD";
    private static String FOUR_SQUARE_CLIENT_SECRET = "UMA4CAX0QWBABNIKWN1ZZCSUOLDRZH05T5K32HTS2G51ZNAW";

    /**
     * MapQuest Key
     */
    private static String MAP_QUEST_KEY = "Fmjtd%7Cluu82luzn0%2C7l%3Do5-948sha";


    /**
     * Overlays for displaying my schedules, buildings, etc.
     */
    private List<PathOverlay> scheduleOverlay;
    private ItemizedIconOverlay<OverlayItem> buildingOverlay;
    private OverlayItem selectedBuildingOnMap;

    /**
     * View that shows the map
     */
    private MapView mapView;

    /**
     * Access to domain model objects. Only store "me" in the studentManager for
     * the base project (i.e., unless you are doing bonus work).
     */
    private StudentManager studentManager;
    private Student randomStudent = null;
    private Schedule randomStudentSchedule = null;
    private Student me = null;
    private static int ME_ID = 999999;

    /**
     * Map controller for zooming in/out, centering
     */
    private IMapController mapController;

    // ******************** Android methods for starting, resuming, ...

    // You should not need to touch this method
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        scheduleOverlay = new ArrayList<PathOverlay>();

        // You need to setup the courses for the app to know about. Ideally
        // we would access a web service like the UBC student information system
        // but that is not currently possible
        initializeCourses();

        // Initialize the data for the "me" schedule. Note that this will be
        // hard-coded for now
        initializeMySchedule();

        // You are going to need an overlay to draw buildings and locations on the map
        buildingOverlay = createBuildingOverlay();
    }

    // You should not need to touch this method
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
    }

    // You should not need to touch this method
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (mapView == null) {
            mapView = new MapView(getActivity(), null);

            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setClickable(true);
            mapView.setBuiltInZoomControls(true);
            mapView.setMultiTouchControls(true);

            mapController = mapView.getController();
            mapController.setZoom(mapView.getMaxZoomLevel() - 2);
            mapController.setCenter(UBC_MARTHA_PIPER_FOUNTAIN);
        }

        return mapView;
    }

    // You should not need to touch this method
    @Override
    public void onDestroyView() {
        Log.d(LOG_TAG, "onDestroyView");
        ((ViewGroup) mapView.getParent()).removeView(mapView);
        super.onDestroyView();
    }

    // You should not need to touch this method
    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    // You should not need to touch this method
    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();
    }

    // You should not need to touch this method
    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause");
        super.onPause();
    }

    /**
     * Save map's zoom level and centre. You should not need to
     * touch this method
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(LOG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        if (mapView != null) {
            outState.putInt("zoomLevel", mapView.getZoomLevel());
            IGeoPoint cntr = mapView.getMapCenter();
            outState.putInt("latE6", cntr.getLatitudeE6());
            outState.putInt("lonE6", cntr.getLongitudeE6());
            Log.i("MapSave", "Zoom: " + mapView.getZoomLevel());
        }
    }

    // ****************** App Functionality

    /**
     * Show my schedule on the map. Every time "me"'s schedule shows, the map
     * should be cleared of all existing schedules, buildings, meetup locations, etc.
     */
    public void showMySchedule() {

        clearSchedules();
        activeDay = sharedPreferences.getString("dayOfWeek", "ERROR");
        initializeMySchedule();
        Schedule mySchedule =  studentManager.get(ME_ID).getSchedule();
        SortedSet<Section> sectionsToday = mySchedule.getSections(activeDay);

        SchedulePlot mySchedulePlot = new SchedulePlot(sectionsToday, "me", MY_COLOUR,
                android.R.drawable.ic_menu_myplaces);

        // UNCOMMENT NEXT LINE ONCE YOU HAVE INSTANTIATED mySchedulePlot
        new GetRoutingForSchedule().execute(mySchedulePlot);

        // CPSC 210 Students: You must complete the implementation of this method.
        // The very last part of the method should call the asynchronous
        // task (which you will also write the code for) to plot the route
        // for "me"'s schedule for the day of the week set in the Settings

        // Asynchronous tasks are a bit onerous to deal with. In order to provide
        // all information needed in one object to plot "me"'s route, we
        // create a SchedulePlot object and pass it to the asynchrous task.
        // See the project page for more details.


        // Get a routing between these points. This line of code creates and calls
        // an asynchronous task to do the calls to MapQuest to determine a route
        // and plots the route.
        // Assumes mySchedulePlot is a create and initialized SchedulePlot object

    }

    /**
     * Retrieve a random student's schedule from the Meetup web service and
     * plot a route for the schedule on the map. The plot should be for
     * the given day of the week as determined when "me"'s schedule
     * was plotted.
     */
    public void showRandomStudentsSchedule() {
        // To get a random student's schedule, we have to call the MeetUp web service.
        // Calling this web service requires a network access to we have to
        // do this in an asynchronous task. See below in this class for where
        // you need to implement methods for performing the network access
        // and plotting.

        // showMySchedule();
        new GetRandomSchedule().execute();
    }

    /**
     * Clear all schedules on the map
     */
    public void clearSchedules() {
        randomStudent = null;
        OverlayManager om = mapView.getOverlayManager();
        om.clear();
        scheduleOverlay.clear();
        buildingOverlay.removeAllItems();
        om.addAll(scheduleOverlay);
        om.add(buildingOverlay);
        mapView.invalidate();
    }

    /**
     * Find all possible locations at which "me" and random student could meet
     * up for the set day of the week and the set time to meet and the set
     * distance either "me" or random is willing to travel to meet.
     * A meetup is only possible if both "me" and random are free at the
     * time specified in the settings and each of us must have at least an hour
     * (>= 60 minutes) free. You should display dialog boxes if there are
     * conditions under which no meetup can happen (e.g., me or random is
     * in class at the specified time)
     */
    public void findMeetupPlace() {

        /**
         * Get all of the Schedules and key information to work with.
         */
        activeDay = sharedPreferences.getString("dayOfWeek", "Error");
        String timeOfDay = sharedPreferences.getString("timeOfDay", "Error");
        String searchRadius = sharedPreferences.getString("placeDistance", "Error");
        String foodOption = sharedPreferences.getString("foodOptions", "Error");

        Schedule mySchedule = me.getSchedule();
        Schedule randomSchedule = randomStudentSchedule;

        if (mySchedule == null || randomSchedule == null) {
            Toast setSchedulesNotice = Toast.makeText(getActivity(),
                    "Please ensure both schedules are set", Toast.LENGTH_LONG);
            setSchedulesNotice.show();
            return;
        }

        SortedSet<Section> mySectionsToday = mySchedule.getSections(activeDay);
        SortedSet<Section> randomSectionsToday = randomSchedule.getSections(activeDay);

        Set<BreakBlock> myOneHourOrLongerBreaks =
                mySchedule.getStartTimesOfOneHourBreaksRefactored(activeDay);
        Set<BreakBlock> randomOneHourOrLongerBreaks =
                randomSchedule.getStartTimesOfOneHourBreaksRefactored(activeDay);

        /**
         * Check if either student has available break times
         */

        if (myOneHourOrLongerBreaks.size() == 0) {
            AlertDialog alertDialog = createSimpleDialog("You have no breaks today");
            alertDialog.show();
            return;
        }

        if (randomOneHourOrLongerBreaks.size() == 0) {
            AlertDialog alertDialog = createSimpleDialog("The other student has no breaks today");
            alertDialog.show();
            return;
        }

        /**
         * Check if either student is in class at the specified time.
         */

        if (checkIfBusy(mySectionsToday, timeOfDay)) {
            AlertDialog alertDialog = createSimpleDialog("You are in class at this time");
            alertDialog.show();
            return;
        } else if (checkIfBusy(randomSectionsToday, timeOfDay)) {
            AlertDialog alertDialog = createSimpleDialog("The other student is in class at this time");
            alertDialog.show();
            return;
        }

        /**
         * If the time works, look for places within the search radius, otherwise notify the user
         * that there are no concurrent breaks.
         */

        if (checkIfTimeWorks(myOneHourOrLongerBreaks, timeOfDay) &&
                checkIfTimeWorks(randomOneHourOrLongerBreaks, timeOfDay)) {


            Building myNearestBuilding = mySchedule.whereAmIRefactored(activeDay, timeOfDay);
            Building randomStudentNearestBuilding = randomSchedule.whereAmIRefactored(activeDay, timeOfDay);

            if (myNearestBuilding == null) {
                AlertDialog alertDialog = createSimpleDialog("You have not started class for the day.");
                alertDialog.show();
                return;
            } else if (randomStudentNearestBuilding == null) {
                AlertDialog alertDialog = createSimpleDialog("The other student has not started class" +
                        "for the day.");
                alertDialog.show();
                return;
            }

            PlaceFactory placeFactory = PlaceFactory.getInstance();

            LatLon myLatLon = myNearestBuilding.getLatLon();
            LatLon randomLatLon = randomStudentNearestBuilding.getLatLon();

            int searchRadiusInt = Integer.parseInt(searchRadius);

            Set<Place> placesNearMe = placeFactory.findPlacesWithinDistance(myLatLon,
                    searchRadiusInt);
            Set<Place> placesNearRandom = placeFactory.findPlacesWithinDistance(randomLatLon,
                    searchRadiusInt);

            Set<Place> possibleMeetUpLocations = new HashSet<Place>();

            for (Place placeNearMe : placesNearMe) {
                if (placesNearRandom.contains(placeNearMe)) {
                    possibleMeetUpLocations.add(placeNearMe);
                }
            }

            if (!(possibleMeetUpLocations.size()==0)) {

                Set<Place> filteredMeetUpLocations =
                        filterPossibleMeetUpLocations(possibleMeetUpLocations, foodOption);

                for (Place place : filteredMeetUpLocations) {
                    plotAPlace(place, place.getName(), "Restaurant", R.drawable.ic_action_place);
                }

                Toast meetUpLocationsPosted = Toast.makeText(getActivity(),
                        "Possible meet up locations within " + searchRadius + " meters plotted",
                        Toast.LENGTH_LONG);
                meetUpLocationsPosted.show();

                mapView.invalidate();

            } else {

                AlertDialog alertDialog = createSimpleDialog("There are no locations within" +
                        " the given search radius");
                alertDialog.show();
                return;
            }


        } else {
            AlertDialog alertDialog = createSimpleDialog("No concurrent breaks at this time");
            alertDialog.show();
            return;
        }


    }

    /**
     * Filter possible meetup locaitons by food preferences.
     * @param possibleMeetUpLocations
     */
    private Set<Place> filterPossibleMeetUpLocations(Set<Place> possibleMeetUpLocations, String foodOption) {
        if (foodOption.equals("All")) {
            return possibleMeetUpLocations;
        } else {
            Set<Place> filteredLocations = new HashSet<Place>();
            for (Place possibleLocation : possibleMeetUpLocations) {
                if (possibleLocation.containsTag(foodOption)) {
                    filteredLocations.add(possibleLocation);
                }
            }
            return filteredLocations;
        }
    }

    /**
     * Helper to determine if a given meetup time works for the given student
     * @param myOneHourOrLongerBreaks
     * @param timeOfDay
     * @return
     */

    private static boolean checkIfTimeWorks(Set<BreakBlock> myOneHourOrLongerBreaks,
                                            String timeOfDay) {

        for (BreakBlock timeBlock : myOneHourOrLongerBreaks ) {
            String startTimeOfBreak = timeBlock.getStartTime();
            String endTimeOfBreak = timeBlock.getEndTime();
            int startTimeMinutes = convertStringTimeToMinutes(startTimeOfBreak);
            int endTimeMinutes = convertStringTimeToMinutes(endTimeOfBreak);
            int meetUpTime = (Integer.parseInt(timeOfDay)*60);

            if ((startTimeMinutes <= meetUpTime) && (meetUpTime < endTimeMinutes) &&
                    ((endTimeMinutes - meetUpTime) >= 60)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to convert HH:MM formatted string to minutes since midnight.
     * @param aTime - time to convert
     * @return - Minutes since midnight
     */
    private static int convertStringTimeToMinutes(String aTime) {
        int colonIndex = aTime.indexOf(":");
        int hours = Integer.parseInt(aTime.substring(0, colonIndex));
        int minutes = Integer.parseInt(aTime.substring(colonIndex + 1,
                aTime.length()));
        return (hours * 60) + minutes;
    }

    /**
     * Helper function to check if the student is in class at a given time of the day
     * @param sections
     * @param time
     * @return
     */
    private boolean checkIfBusy(SortedSet<Section> sections, String time) {
        for (Section section : sections) {
            if (section.checkIfBusy(time)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to plot a given place on the map, following the implementation of plotABuilding
     * @param place - Place
     * @param title - Title of place
     * @param msg - Msg to display when place is tapped
     * @param drawableToUse - Icon to display on map
     */
    private void plotAPlace(Place place, String title, String msg, int drawableToUse) {
        OverlayItem placeItem = new OverlayItem(title, msg,
                new GeoPoint(place.getLatLon().getLatitude(), place.getLatLon().getLongitude()));

        Drawable icon = this.getResources().getDrawable(drawableToUse);

        icon.setBounds(
                0 - icon.getIntrinsicWidth() / 2, 0 - icon.getIntrinsicHeight(),
                icon.getIntrinsicWidth() / 2, 0);

        placeItem.setMarker(icon);
        buildingOverlay.addItem(placeItem);
    }

    /**
     * Initialize the PlaceFactory with information from FourSquare
     */
    public void initializePlaces() {
        // CPSC 210 Students: You should not need to touch this method, but
        // you will have to implement GetPlaces below.
        new GetPlaces().execute();
    }


    /**
     * Plot all buildings referred to in the given information about plotting
     * a schedule.
     * @param schedulePlot All information about the schedule and route to plot.
     */
    private void plotBuildings(SchedulePlot schedulePlot) {

        // CPSC 210 Students: Complete this method by plotting each building in the
        // schedulePlot with an appropriate message displayed

        SortedSet<Section> sections = schedulePlot.getSections();

        // CPSC 210 Students: You will need to ensure the buildingOverlay is in
        // the overlayManager. The following code achieves this. You should not likely
        // need to touch it
        OverlayManager om = mapView.getOverlayManager();
        om.add(buildingOverlay);

        for (Section section : sections) {
            Building building = section.getBuilding();
            String buildingName = building.getName();
            String courseCode = section.getCourse().getCode();
            int courseNum = section.getCourse().getNumber();
            String courseNumber = Integer.toString(courseNum);
            String courseStart = section.getCourseTime().getStartTime();
            String courseEnd = section.getCourseTime().getEndTime();
            String studentName = schedulePlot.getName();
            String message = "Student: "+ studentName + " Course: " + courseCode + " " + courseNumber + " Time: " + courseStart +
                    " to " + courseEnd;
            plotABuilding(building, buildingName, message, schedulePlot.getIcon());
        }

    }

    /**
     * Plot a building onto the map
     * @param building The building to put on the map
     * @param title The title to put in the dialog box when the building is tapped on the map
     * @param msg The message to display when the building is tapped
     * @param drawableToUse The icon to use. Can be R.drawable.ic_action_place (or any icon in the res/drawable directory)
     */
    private void plotABuilding(Building building, String title, String msg, int drawableToUse) {
        // CPSC 210 Students: You should not need to touch this method
        OverlayItem buildingItem = new OverlayItem(title, msg,
                new GeoPoint(building.getLatLon().getLatitude(), building.getLatLon().getLongitude()));

        //Create new marker
        Drawable icon = this.getResources().getDrawable(drawableToUse);

        //Set the bounding for the drawable
        icon.setBounds(
                0 - icon.getIntrinsicWidth() / 2, 0 - icon.getIntrinsicHeight(),
                icon.getIntrinsicWidth() / 2, 0);

        //Set the new marker to the overlay
        buildingItem.setMarker(icon);
        buildingOverlay.addItem(buildingItem);
    }



    /**
     * Initialize your schedule by coding it directly in. This is the schedule
     * that will appear on the map when you select "Show My Schedule".
     */
    private void  initializeMySchedule() {

        Student me = new Student("Swanson", "Ryan", ME_ID);
        studentManager = new StudentManager();

        // Add me (and only me) to the student manager and construct my schedule.
        studentManager.addStudent("Swanson", "Ryan", ME_ID);

        // All four classes have been constructed below.
        studentManager.addSectionToSchedule(ME_ID, "TEST", 201, "201");
        studentManager.addSectionToSchedule(ME_ID, "TEST", 202, "202");
        //studentManager.addSectionToSchedule(ME_ID, "MATH", 221, "202");
        studentManager.addSectionToSchedule(ME_ID, "TEST", 203, "203");
        studentManager.addSectionToSchedule(ME_ID, "TEST", 204, "204");
        studentManager.addSectionToSchedule(ME_ID, "TEST", 205, "205");
        this.me = studentManager.get(ME_ID);
    }

    /**
     * Helper to create simple alert dialog to display message
     *
     * @param msg message to display in alert dialog
     * @return the alert dialog
     */
    private AlertDialog createSimpleDialog(String msg) {
        // CPSC 210 Students; You should not need to modify this method
        AlertDialog.Builder dialogBldr = new AlertDialog.Builder(getActivity());
        dialogBldr.setMessage(msg);
        dialogBldr.setNeutralButton(R.string.ok, null);

        return dialogBldr.create();
    }

    /**
     * Create the overlay used for buildings. CPSC 210 students, you should not need to
     * touch this method.
     * @return An overlay
     */
    private ItemizedIconOverlay<OverlayItem> createBuildingOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

        ItemizedIconOverlay.OnItemGestureListener<OverlayItem> gestureListener =
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

            /**
             * Display building description in dialog box when user taps stop.
             *
             * @param index
             *            index of item tapped
             * @param oi
             *            the OverlayItem that was tapped
             * @return true to indicate that tap event has been handled
             */
            @Override
            public boolean onItemSingleTapUp(int index, OverlayItem oi) {

                new AlertDialog.Builder(getActivity())
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                if (selectedBuildingOnMap != null) {
                                    mapView.invalidate();
                                }
                            }
                        }).setTitle(oi.getTitle()).setMessage(oi.getSnippet())
                        .show();

                selectedBuildingOnMap = oi;
                mapView.invalidate();
                return true;
            }

            @Override
            public boolean onItemLongPress(int index, OverlayItem oi) {
                // do nothing
                return false;
            }
        };

        return new ItemizedIconOverlay<OverlayItem>(
                new ArrayList<OverlayItem>(), getResources().getDrawable(
                R.drawable.ic_action_place), gestureListener, rp);
    }


    /**
     * Create overlay with a specific color
     * @param colour A string with a hex colour value
     */
    private PathOverlay createPathOverlay(String colour) {
        // CPSC 210 Students, you should not need to touch this method
        PathOverlay po = new PathOverlay(Color.parseColor(colour),
                getActivity());
        Paint pathPaint = new Paint();
        pathPaint.setColor(Color.parseColor(colour));
        pathPaint.setStrokeWidth(4.0f);
        pathPaint.setStyle(Paint.Style.STROKE);
        po.setPaint(pathPaint);
        return po;
    }

   // *********************** Asynchronous tasks

    /**
     * This asynchronous task is responsible for contacting the Meetup web service
     * for the schedule of a random student. The task must plot the retrieved
     * student's route for the schedule on the map in a different colour than the "me" schedule
     * or must display a dialog box that a schedule was not retrieved.
     */
    private class GetRandomSchedule extends AsyncTask<Void, Void, SchedulePlot> {

        // Some overview explanation of asynchronous tasks is on the project web page.

        @Override
        protected void onPreExecute() {
            clearSchedules();
            showMySchedule();
        }

        @Override
        protected SchedulePlot doInBackground(Void... params) {

            // CPSC 210 Students: You must complete this method. It needs to
            // contact the Meetup web service to get a random student's schedule.
            // If it is successful in retrieving a student and their schedule,
            // it needs to remember the student in the randomStudent field
            // and it needs to create and return a schedulePlot object with
            // all relevant information for being ready to retrieve the route
            // and plot the route for the schedule. If no random student is
            // retrieved, return null.
            //
            // Note, leave all determination of routing and plotting until
            // the onPostExecute method below.

            // Generate a placeholder student schedule to be overwritten below.

            SchedulePlot scheduleToPlot = new SchedulePlot(new TreeSet<Section>(),
                    "loading", STUDENT_COLOUR,android.R.drawable.ic_menu_myplaces);

            Student student;
            SortedSet<Section> sections;
            SortedSet<Section> filteredSections = new TreeSet<Section>();

            try {

                MeetUpParser parser = new MeetUpParser();
                String randomStudentRawJSON = getStudent();
                student = parser.parseStudent(randomStudentRawJSON);
                sections = parser.parseSections(randomStudentRawJSON);

                Schedule randomSchedule = new Schedule();

                for (Section section : sections) {
                    randomSchedule.add(section);
                }

                randomStudent = student;
                randomStudentSchedule = randomSchedule;


                String fName = student.getFirstName();

                for (Section section : sections) {
                    if (section.getDayOfWeek().equals(activeDay)) {
                        filteredSections.add(section);
                    }
                }

                scheduleToPlot = new SchedulePlot(filteredSections, fName, STUDENT_COLOUR,
                        android.R.drawable.ic_menu_myplaces);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        		return scheduleToPlot;
        }

        @Override
        protected void onPostExecute(SchedulePlot schedulePlot) {
            // CPSC 210 students: When this method is called, it will be passed
            // whatever schedulePlot object you created (if any) in doBackground
            // above. Use it to plot the route.

            //showMySchedule();
            if (schedulePlot.getSections().size() == 0) {
                Toast noAvailableSections = Toast.makeText(getActivity(),
                        "The selected random student has no sections today", Toast.LENGTH_LONG);
                noAvailableSections.show();
            }

            new GetRoutingForSchedule().execute(schedulePlot);

        }


        /**
         * Contacts the MeetUp webservice and obtains a JSON formatted string
         * @return Returns the raw JSON text from MeetUp webservice as a String.
         */
        private String getStudent() throws IOException {

            String httpRequest = "http://kramer.nss.cs.ubc.ca:8081/getStudent";

            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();

            return returnString;

        }
    }

    /**
     * This asynchronous task is responsible for contacting the MapQuest web service
     * to retrieve a route between the buildings on the schedule and for plotting any
     * determined route on the map.
     */
    private class GetRoutingForSchedule extends AsyncTask<SchedulePlot, Void, SchedulePlot> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected SchedulePlot doInBackground(SchedulePlot... params) {

            // The params[0] element contains the schedulePlot object
            SchedulePlot scheduleToPlot = params[0];
            SortedSet<Section> sections = scheduleToPlot.getSections();

            // Compile a list of all buildings LatLon's to visit (in order).
            ArrayList<LatLon> buildingLatLons = new ArrayList<LatLon>();

            for (Section section : sections) {
                buildingLatLons.add(section.getBuilding().getLatLon());
            }

            // Initialize a list of GeoPoints for plotting my schedule
            List<GeoPoint> myGeoPoints = new ArrayList<GeoPoint>();

            // Generate MapQuest http requests based off the given LatLon objects.

            for (int i = 0 ; i < (buildingLatLons.size() - 1) ; i++) {

                int j = i + 1;

                String newMqURL = generateMapQuestRequest(buildingLatLons.get(i),
                        buildingLatLons.get(j));

                MapQuestParser parser = new MapQuestParser();

                try {
                    String mqRawOutput = makeRoutingCall(newMqURL);
                    JSONTokener mqRawJson = new JSONTokener(mqRawOutput);
                    List<GeoPoint> geoPointDirections = parser.parseLatLon(mqRawJson);
                    myGeoPoints.addAll(geoPointDirections);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            scheduleToPlot.setRoute(myGeoPoints);

            return scheduleToPlot;

            // CPSC 210 Students: Complete this method. This method should
            // call the MapQuest webservice to retrieve a List<GeoPoint>
            // that forms the routing between the buildings on the
            // schedule. The List<GeoPoint> should be put into
            // scheduleToPlot object.
        }

  
        /**
         * An example helper method to call a web service
         */
        private String makeRoutingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();

            String formattedReturnString = returnString.substring(24, returnString.length() - 2);

            return formattedReturnString;
        }

        /**
         * Generates the relevant http request for mapquest from the given LatLons of the origin
         * and destination buildings on UBC's campus.
         * @param start - the origin LatLon point
         * @param end - the target destination LatLon
         * @return - a concatenated http string to use with the mapquest webservice.
         */
        private String generateMapQuestRequest(LatLon start, LatLon end) {

            String startLat = Double.toString(start.getLatitude());
            String startLon = Double.toString(start.getLongitude());
            String endLat = Double.toString(end.getLatitude());
            String endLon = Double.toString(end.getLongitude());

            String URL = "http://open.mapquestapi.com/directions/v2/route?key=" +
                    MAP_QUEST_KEY +
                    "&callback=renderAdvancedNarrative&outFormat=json&routeType=pedestrian" +
                    "&timeType=1&enhancedNarrative=false&shapeFormat=raw&generalize=0&locale=en_GB&unit=k&" +
                    "from=" +
                    startLat + "," + startLon +
                    "&" +
                    "to=" +
                    endLat + "," + endLon +
                    "&drivingStyle=2" +
                    "&highwayEfficiency=21.0";

            return URL;
        }

        @Override
        protected void onPostExecute(SchedulePlot schedulePlot) {

            // CPSC 210 Students: This method should plot the route onto the map
            // with the given line colour specified in schedulePlot. If there is
            // no route to plot, a dialog box should be displayed.

            List<GeoPoint> routeToPlot = schedulePlot.getRoute();

            PathOverlay pathOverlay = createPathOverlay(schedulePlot.getColourOfLine());

            for (int i = 0 ; i < schedulePlot.getRoute().size() ; i += 2) {

                pathOverlay.addPoint(routeToPlot.get(i));
            }

            scheduleOverlay.add(pathOverlay);
            plotBuildings(schedulePlot);
            OverlayManager overlayManager = mapView.getOverlayManager();
            overlayManager.addAll(scheduleOverlay);
            mapView.invalidate();


            // To actually make something show on the map, you can use overlays.
            // For instance, the following code should show a line on a map
            // PathOverlay po = createPathOverlay("#FFFFFF");
            // po.addPoint(point1); // one end of line
            // po.addPoint(point2); // second end of line
            // scheduleOverlay.add(po);
            // OverlayManager om = mapView.getOverlayManager();
            // om.addAll(scheduleOverlay);
            // mapView.invalidate(); // cause map to redraw

    
        }

    }

    /**
     * This asynchronous task is responsible for contacting the FourSquare web service
     * to retrieve all places around UBC that have to do with food. It should load
     * any determined places into PlaceFactory and then display a dialog box of how it did
     */
    private class GetPlaces extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... params) {

            // CPSC 210 Students: Complete this method to retrieve a string
            // of JSON from FourSquare. Return the string from this method

            String fourSquareHttpsRequest = generateFourSquareRequest();

            try {
                String fourSquareRawJSON = makeRoutingCall(fourSquareHttpsRequest);
                return fourSquareRawJSON;

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String jSONOfPlaces) {
            // CPSC 210 Students: Given JSON from FourQuest, parse it and load
            // PlaceFactory
            FoursquareParser parser = new FoursquareParser();
            int placesFound = parser.parseFoodVenues(jSONOfPlaces);
            Toast placesAddedToast = Toast.makeText(getActivity(), placesFound + " New Places Added"
                    , Toast.LENGTH_LONG );
            placesAddedToast.show();
        }

        /**
         * Method generates https request to Foursquare using the coordinates of the Fountain for now.
         * @return
         */
        private String generateFourSquareRequest() {

            double centralLat = UBC_MARTHA_PIPER_FOUNTAIN.getLatitude();
            double centralLon = UBC_MARTHA_PIPER_FOUNTAIN.getLongitude();

            return "https://api.foursquare.com/v2/venues/explore?" +
                        "ll="+ centralLat + "," + centralLon +
                        "&section=food" +
                        "&radius=3000" +
                        "&client_id=" + FOUR_SQUARE_CLIENT_ID +
                        "&client_secret=" + FOUR_SQUARE_CLIENT_SECRET +
                        "&v=20150329";
        }

        private void generateVersionForFourSquare() {
            int year = Calendar.getInstance().get(Calendar.YEAR);
            String yearString = Integer.toString(year);
            int month = Calendar.getInstance().get(Calendar.MONTH);
            month++;
            String monthString = Integer.toString(month);
            int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        }

        /**
         * Helper method to call FourSquare request
         */
        private String makeRoutingCall(String httpsRequest) throws IOException {
            URL url = new URL(httpsRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();

            return returnString;
        }
    }



    /**
     * Initialize the CourseFactory with some courses.
     */
    private void initializeCourses() {
        // CPSC 210 Students: You can change this data if you desire.
        CourseFactory courseFactory = CourseFactory.getInstance();

        Building dmpBuilding = new Building("DMP", new LatLon(49.261474, -123.248060));

        Course cpsc210 = courseFactory.getCourse("CPSC", 210);
        Section aSection = new Section("202", "MWF", "12:00", "12:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);
        aSection = new Section("201", "MWF", "16:00", "16:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);
        aSection = new Section("BCS", "MWF", "12:00", "12:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);


        Course engl222 = courseFactory.getCourse("ENGL", 222);
        aSection = new Section("007", "MWF", "14:00", "14:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        engl222.addSection(aSection);
        aSection.setCourse(engl222);

        Course scie220 = courseFactory.getCourse("SCIE", 220);
        aSection = new Section("200", "MWF", "15:00", "15:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        scie220.addSection(aSection);
        aSection.setCourse(scie220);

        Course math200 = courseFactory.getCourse("MATH", 200);
        aSection = new Section("201", "MWF", "09:00", "09:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        math200.addSection(aSection);
        aSection.setCourse(math200);

        Course fren102 = courseFactory.getCourse("FREN", 102);
        aSection = new Section("202", "MWF", "11:00", "11:50", new Building("Barber", new LatLon(49.267442,-123.252471)));
        fren102.addSection(aSection);
        aSection.setCourse(fren102);

        Course japn103 = courseFactory.getCourse("JAPN", 103);
        aSection = new Section("002", "MWF", "10:00", "11:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        japn103.addSection(aSection);
        aSection.setCourse(japn103);

        Course scie113 = courseFactory.getCourse("SCIE", 113);
        aSection = new Section("213", "MWF", "13:00", "13:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        scie113.addSection(aSection);
        aSection.setCourse(scie113);

        Course micb308 = courseFactory.getCourse("MICB", 308);
        aSection = new Section("201", "MWF", "12:00", "12:50", new Building("Woodward", new LatLon(49.264704,-123.247536)));
        micb308.addSection(aSection);
        aSection.setCourse(micb308);

        Course math221 = courseFactory.getCourse("MATH", 221);
        aSection = new Section("202", "TR", "11:00", "12:20", new Building("Klinck", new LatLon(49.266112, -123.254776)));
        math221.addSection(aSection);
        aSection.setCourse(math221);

        Course phys203 = courseFactory.getCourse("PHYS", 203);
        aSection = new Section("201", "TR", "09:30", "10:50", new Building("Hennings", new LatLon(49.266400,-123.252047)));
        phys203.addSection(aSection);
        aSection.setCourse(phys203);

        Course crwr209 = courseFactory.getCourse("CRWR", 209);
        aSection = new Section("002", "TR", "12:30", "13:50", new Building("Geography", new LatLon(49.266039,-123.256129)));
        crwr209.addSection(aSection);
        aSection.setCourse(crwr209);

        Course fnh330 = courseFactory.getCourse("FNH", 330);
        aSection = new Section("002", "TR", "15:00", "16:20", new Building("MacMillian", new LatLon(49.261167,-123.251157)));
        fnh330.addSection(aSection);
        aSection.setCourse(fnh330);

        Course cpsc499 = courseFactory.getCourse("CPSC", 430);
        aSection = new Section("201", "TR", "16:20", "17:50", new Building("Liu", new LatLon(49.267632,-123.259334)));
        cpsc499.addSection(aSection);
        aSection.setCourse(cpsc499);

        Course chem250 = courseFactory.getCourse("CHEM", 250);
        aSection = new Section("203", "TR", "10:00", "11:20", new Building("Klinck", new LatLon(49.266112, -123.254776)));
        chem250.addSection(aSection);
        aSection.setCourse(chem250);

        Course eosc222 = courseFactory.getCourse("EOSC", 222);
        aSection = new Section("200", "TR", "11:00", "12:20", new Building("ESB", new LatLon(49.262866, -123.25323)));
        eosc222.addSection(aSection);
        aSection.setCourse(eosc222);

        Course biol201 = courseFactory.getCourse("BIOL", 201);
        aSection = new Section("201", "TR", "14:00", "15:20", new Building("BioSci", new LatLon(49.263920, -123.251552)));
        biol201.addSection(aSection);
        aSection.setCourse(biol201);

        Course test101 = courseFactory.getCourse("TEST", 201);
        aSection = new Section("201", "TR", "14:00", "14:50", new Building("BioSci", new LatLon(49.263920, -123.251552)));
        test101.addSection(aSection);
        aSection.setCourse(test101);

        Course test102 = courseFactory.getCourse("TEST", 202);
        aSection = new Section("202", "TR", "08:30", "09:20", new Building("Geography", new LatLon(49.266039,-123.256129)));
        test102.addSection(aSection);
        aSection.setCourse(test102);

        Course test103 = courseFactory.getCourse("TEST", 203);
        aSection = new Section("203", "MWF", "08:30", "09:20", new Building("Geography", new LatLon(49.266039,-123.256129)));
        test103.addSection(aSection);
        aSection.setCourse(test103);

        Course test104 = courseFactory.getCourse("TEST", 204);
        aSection = new Section("204", "MWF", "14:00", "14:50", new Building("ESB", new LatLon(49.262866, -123.25323)));
        test104.addSection(aSection);
        aSection.setCourse(test104);

        Course test105 = courseFactory.getCourse("TEST", 205);
        aSection = new Section("205", "MWF", "17:00", "17:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        test105.addSection(aSection);
        aSection.setCourse(test105);

        Course stat200 = courseFactory.getCourse("STAT", 200);
        aSection = new Section("201", "MWF", "14:00", "14:50", new Building("ESB", new LatLon(49.262866, -123.25323)));
        stat200.addSection(aSection);
        aSection.setCourse(stat200);

    }
}