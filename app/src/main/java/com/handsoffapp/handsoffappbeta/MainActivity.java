package com.handsoffapp.handsoffappbeta;

        import android.app.Activity;
        import android.content.Context;
        import android.location.Address;
        import android.os.Bundle;
        import android.os.Vibrator;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.here.android.mpa.cluster.ClusterLayer;
        import com.here.android.mpa.common.GeoCoordinate;
        import com.here.android.mpa.common.GeoPosition;
        import com.here.android.mpa.common.OnEngineInitListener;
        import com.here.android.mpa.common.PositioningManager;
        import com.here.android.mpa.common.RoadElement;
        import com.here.android.mpa.customlocation.ProximityRequest;
        import com.here.android.mpa.customlocation.Request;
        import com.here.android.mpa.customlocation.Result;
        import com.here.android.mpa.guidance.LaneInfo;
        import com.here.android.mpa.guidance.NavigationManager;
        import com.here.android.mpa.mapping.Map;
        import com.here.android.mpa.mapping.MapFragment;
        import com.here.android.mpa.mapping.MapMarker;
        import com.here.android.mpa.mapping.MapRoute;
        import com.here.android.mpa.routing.Maneuver;
        import com.here.android.mpa.routing.Route;
        import com.here.android.mpa.routing.RouteManager;
        import com.here.android.mpa.routing.RouteOptions;
        import com.here.android.mpa.routing.RoutePlan;
        import com.here.android.mpa.routing.RouteResult;
        import com.here.android.mpa.search.DiscoveryLink;
        import com.here.android.mpa.search.DiscoveryRequest;
        import com.here.android.mpa.search.DiscoveryResult;
        import com.here.android.mpa.search.DiscoveryResultPage;
        import com.here.android.mpa.search.ErrorCode;
        import com.here.android.mpa.search.GeocodeRequest;
        import com.here.android.mpa.search.Location;
        import com.here.android.mpa.search.PlaceLink;
        import com.here.android.mpa.search.ResultListener;
        import com.here.android.mpa.search.SearchRequest;
        import com.here.android.mpa.search.TextSuggestionRequest;

        import org.w3c.dom.Text;

        import java.lang.ref.WeakReference;
        import java.util.Enumeration;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Timer;
        import java.util.TimerTask;

public class MainActivity extends Activity {

    // map embedded in the map fragment
    private Map map = null;

    public ClusterLayer cl;
    public RouteManager rm;
    public MapRoute mapRoute;

    public TextView textViewTime;

    public GeoPosition publicGeoPos;

    public PositioningManager ps;
    public PositioningManager positioningManager;
    public GeoCoordinate gc;

    public DiscoveryResultPage mResultPage = null;

    public TextView navTextView;
    public TextView textViewTurn;

    public NavigationManager navigationManager;

    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.mapfragment);

        // Init map fragment
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();

                    ps = PositioningManager.getInstance();
                    ps.start(PositioningManager.LocationMethod.GPS_NETWORK);


                    Log.d("#", "Working");


                    while (!ps.hasValidPosition()) {

                    }

                    gc = new GeoCoordinate(ps.getPosition().getCoordinate().getLatitude(), ps.getPosition().getCoordinate().getLongitude(), 0.0);

                    Log.d("€", gc.toString());


                    map.setCenter(new GeoCoordinate(ps.getPosition().getCoordinate().getLatitude(),
                            ps.getPosition().getCoordinate().getLongitude()), Map.Animation.NONE);

                    // Get the maximum,minimum zoom level.
                    double maxZoom = map.getMaxZoomLevel();
                    map.setZoomLevel(maxZoom);

                    map.getPositionIndicator().setVisible(true);

                    createRoute();
                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment" + error.toString());
                }
            }
        });



    }



    public void createRoute()
    {


        Log.d("BJLIET", "Works");





        Button btnClick = (Button) findViewById(R.id.searchButton) ;

        cl = new ClusterLayer();

        btnClick.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                EditText edittextitem = (EditText)findViewById(R.id.editTextTitle);
                String text = edittextitem.getText().toString();
                search(text);
            }
        });





    }


    private class RouteListener implements RouteManager.Listener {

        // Method defined in Listener
        public void onProgress(int percentage) {
            // Display a message indicating calculation progress
        }

        // Method defined in Listener
        public void onCalculateRouteFinished(RouteManager.Error error, List<RouteResult> routeResult) {
            // If the route was calculated successfully
            if (error == RouteManager.Error.NONE) {
                // Render the route on the map
                if(mapRoute != null) {
                    map.removeMapObject(mapRoute);
                }


                mapRoute = new MapRoute(routeResult.get(0).getRoute());
                map.addMapObject(mapRoute);



                navigationManager = NavigationManager.getInstance();

                //set the map where the navigation will be performed
                navigationManager.setMap(map);

                map.setCenter(new GeoCoordinate(ps.getPosition().getCoordinate().getLatitude(),
                        ps.getPosition().getCoordinate().getLongitude()), Map.Animation.NONE);

                // if user wants to start real navigation, submit calculated route
                // for more information on calculating a route, see the "Directions" section

                Log.d("#", "Navigation about to start");

                NavigationManager.Error derpError = navigationManager.startNavigation(mapRoute.getRoute());



                // Init listners

                navigationManager.addRerouteListener(new WeakReference<NavigationManager.RerouteListener>(rerouteListner));

                //// start listening to navigation events
                navigationManager.addNewInstructionEventListener(
                        new WeakReference<NavigationManager.NewInstructionEventListener>(instructListener));

                // start listening to position events
                navigationManager.addPositionListener(
                        new WeakReference<NavigationManager.PositionListener>(positionListener));
//
                navigationManager.addAudioFeedbackListener(new WeakReference<NavigationManager.AudioFeedbackListener>(audioListner));

            }
            else {
                // Display a message indicating route calculation failure
            }

        }

        private NavigationManager.AudioFeedbackListener audioListner = new NavigationManager.AudioFeedbackListener() {
            @Override
            public void onAudioStart() {
                super.onAudioStart();
                Context context = getApplicationContext();
                CharSequence text = "Audio activated";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

                Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                v.vibrate(500);
            }
        };

        private NavigationManager.RerouteListener rerouteListner
                = new NavigationManager.RerouteListener() {

            @Override
            public void onRerouteBegin() {
                // Interpret and present the Maneuver object as it contains
                // turn by turn navigation instructions for the user.


                Context context = getApplicationContext();
                CharSequence text = "Reroute activated";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();


               // navTextView = (TextView) findViewById(R.id.textViewNav);
               // navTextView.setText("Next maneover:" + navigationManager.getNextManeuver().getTurn().toString());


            }
        };

        private NavigationManager.NewInstructionEventListener instructListener
                = new NavigationManager.NewInstructionEventListener() {

            @Override
            public void onNewInstructionEvent() {
                // Interpret and present the Maneuver object as it contains
                // turn by turn navigation instructions for the user.


                navTextView = (TextView) findViewById(R.id.textViewNav);

                // Junction
                if (navigationManager.getNextManeuver().getAction().value() == 4) {
                    if ((navigationManager.getNextManeuver().getTurn() == Maneuver.Turn.QUITE_LEFT) || (navigationManager.getNextManeuver().getTurn() == Maneuver.Turn.HEAVY_LEFT)) {
                        navTextView.setText("Turn left");
                    } else if ((navigationManager.getNextManeuver().getTurn() == Maneuver.Turn.QUITE_RIGHT) || (navigationManager.getNextManeuver().getTurn() == Maneuver.Turn.HEAVY_RIGHT)) {
                        navTextView.setText("Turn right");
                    }

                    else
                    {
                        navTextView.setText("Turn " + navigationManager.getNextManeuver().getTurn());
                    }
                }

                else
                {
                    navTextView.setText(navigationManager.getNextManeuver().getInstruction() + " " + navigationManager.getNextManeuver().getTurn().toString());
                }
            }
        };

        private NavigationManager.PositionListener positionListener
                = new NavigationManager.PositionListener() {

            @Override
            public void onPositionUpdated(GeoPosition loc) {

                navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.POSITION_ANIMATION);

                publicGeoPos = loc;

                Log.d("Route", "Position update");
                // the position we get in this callback can be used
                // to reposition the map and change orientation.
                loc.getCoordinate();
                loc.getHeading();
                loc.getSpeed();

                textViewTime = (TextView) findViewById(R.id.textViewTime);

                textViewTurn = (TextView) findViewById(R.id.textViewCurrent);

                // also remaining time and distance can be
                // fetched from navigation manager
                navigationManager.getEta(true,
                        Route.TrafficPenaltyMode.DISABLED);
                navigationManager.getDestinationDistance();




                // Inner class that runs the distance update every 5 secs
                class SayHello extends TimerTask {
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                navTextView = (TextView) findViewById(R.id.textViewNav);

                                // Junction
                                if (navigationManager.getNextManeuver().getAction().value() == 4) {
                                    if ((navigationManager.getNextManeuver().getTurn() == Maneuver.Turn.QUITE_LEFT) || (navigationManager.getNextManeuver().getTurn() == Maneuver.Turn.HEAVY_LEFT)) {
                                        navTextView.setText("Turn left");
                                    } else if ((navigationManager.getNextManeuver().getTurn() == Maneuver.Turn.QUITE_RIGHT) || (navigationManager.getNextManeuver().getTurn() == Maneuver.Turn.HEAVY_RIGHT)) {
                                        navTextView.setText("Turn right");
                                    }

                                    else
                                    {
                                        navTextView.setText("Turn " + navigationManager.getNextManeuver().getTurn());
                                    }
                                }

                                else
                                {
                                    navTextView.setText(navigationManager.getNextManeuver().getInstruction() + " " + navigationManager.getNextManeuver().getTurn().toString());
                                }

                            Log.d("Position", "Updated UI with real info");
                            textViewTime.setText(navigationManager.getNextManeuverDistance() + " meters");
                                textViewTurn.setText("Moving speed: " + publicGeoPos.getSpeed() + ", time to finish: " + navigationManager.getEta(true,
                                        Route.TrafficPenaltyMode.DISABLED).toString());

                            }
                        });



                    }
                }

                // And From your main() method or any other method // Runs the pos updater
                Timer timer = new Timer();
                timer.schedule(new SayHello(), 0, 2000);

                textViewTime.setText(navigationManager.getNextManeuverDistance() + " meters");

                textViewTurn.setText("Moving speed: " + loc.getSpeed() + ", time to finish: " + navigationManager.getEta(true,
                        Route.TrafficPenaltyMode.DISABLED).toString());


            }
        };




    }


    public void search(String searchquery)
    {
       Log.d("€", "Searches");


        // Example Search request listener
        class SearchRequestListener implements ResultListener<DiscoveryResultPage> {

            @Override
            public void onCompleted(DiscoveryResultPage data, ErrorCode error) {
                if (error != ErrorCode.NONE) {
                    // Handle error

                } else {
                    List<DiscoveryResult> items = data.getItems();

                    // Iterate through the found place items.
                    for (DiscoveryResult item : items) {
                        // A Item can either be a PlaceLink (meta information
                        // about a Place) or a DiscoveryLink (which is a reference
                        // to another refined search that is related to the
                        // original search; for example, a search for
                        // "Leisure & Outdoor").

                        if (item.getResultType() == DiscoveryResult.ResultType.PLACE) {
                            PlaceLink placeLink = (PlaceLink) item;

                            GeoCoordinate newgc = placeLink.getPosition();
                            map.setCenter(new GeoCoordinate(newgc.getLatitude(),
                                    newgc.getLongitude()), Map.Animation.NONE);

                            MapMarker mm = new MapMarker();
                            mm.setCoordinate(new GeoCoordinate(newgc.getLatitude(), newgc.getLongitude()));


                            cl.removeMarkers(cl.getMarkers());
                            cl.addMarker(mm);

                            map.addClusterLayer(cl);

                            // Declare the rm variable (the RouteManager)
                            rm = new RouteManager();
                            rm.cancel();


                            // Create the RoutePlan and add two waypoints
                            RoutePlan routePlan = new RoutePlan();

                            routePlan.addWaypoint(gc);
                            routePlan.addWaypoint(new GeoCoordinate(newgc.getLatitude(), newgc.getLongitude()));

                            // Create the RouteOptions and set its transport mode & routing type
                            RouteOptions routeOptions = new RouteOptions();
                            routeOptions.setTransportMode(RouteOptions.TransportMode.PEDESTRIAN);
                            routeOptions.setRouteType(RouteOptions.Type.SHORTEST);

                            routePlan.setRouteOptions(routeOptions);

                            Log.d("BJLIET", "Works");



                            // Calculate the route
                            rm.calculateRoute(routePlan, new RouteListener());

                            Log.d("€", "Searching");

                            Log.d("place", placeLink.toString());

                            break;

                            // PlaceLink should be presented to the user, so the link can be
                            // selected in order to retrieve additional details about a place
                            // of interest.


                        } else if (item.getResultType() == DiscoveryResult.ResultType.DISCOVERY) {
                            DiscoveryLink discoveryLink = (DiscoveryLink) item;

                            Log.d("place", discoveryLink.toString());

                            // DiscoveryLink can also be presented to the user.
                            // When a DiscoveryLink is selected, another search request should be
                            // performed to retrieve results for a specific category.

                        }
                    }

                }
            }
        }

        // Create a request to search for restaurants in Seattle
        try {

            DiscoveryRequest request =
                    new SearchRequest(searchquery).setSearchCenter(ps.getPosition().getCoordinate());



            // limit number of items in each result page to 10
            request.setCollectionSize(10);

            ErrorCode error = request.execute(new SearchRequestListener());
            if( error != ErrorCode.NONE ) {
                // Handle request error

            }
        } catch (IllegalArgumentException ex) {
            // Handle invalid create search request parameters

        }

    }


}
