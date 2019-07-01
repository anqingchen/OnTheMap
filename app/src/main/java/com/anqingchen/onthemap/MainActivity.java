package com.anqingchen.onthemap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Filter;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;

import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    FloatingActionButton addEventBtn, filterButton;

    MapboxMap mapboxMap;
    MapView mapView;
    ArrayList<Event> eventsList = new ArrayList<>();
    ArrayList<Event> filteredList = new ArrayList<>();
    List<Symbol> currentSymbols = new ArrayList<>();
    LocationManager mLocationManager;
    SymbolManager mSymbolManager;
    HashMap<String, Boolean> filterOptions = new HashMap<>();

    ChildEventListener eventListener;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load Mapbox Map with my API key
        Mapbox.getInstance(this, "pk.eyJ1Ijoic2FtYXJpdGFucyIsImEiOiJjanhjaHN6OXowM2twM3dvY3k1Z2k2bWQzIn0.qNMnSU_p4akStUv8Z8uQ6w");
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        filterOptions.put("FOOD", true);
        filterOptions.put("ENTERTAINMENT", true);

        // Initialize the map
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Initialize addEventBtn to load the add event page
        addEventBtn = findViewById(R.id.addEventBtn);
        addEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "CLICKED ME", Toast.LENGTH_SHORT).show();
                openEventActivity(getLastKnownLocation());
            }
        });

        filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilterActivity();
            }
        });

        // Initialize Firebase Database to update list of events
        mDatabase = FirebaseDatabase.getInstance().getReference().child("events");
        eventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Toast.makeText(MainActivity.this, "NEW CHILD", Toast.LENGTH_SHORT).show();
                Event event = dataSnapshot.getValue(Event.class);
                addEvent(event);
                filterSymbols();
                repopulateSymbols();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Event event = dataSnapshot.getValue(Event.class);
                removeEvent(event.getUniqueID());
                filterSymbols();
                repopulateSymbols();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mDatabase.addChildEventListener(eventListener);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/samaritans/cjxcp0y0s01lg1cnwh18eid9q"), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                // moves map center to show current phone location
                enableLocationComponent(style);
                FloatingActionButton locationButton = findViewById(R.id.locationButton);
                locationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getApplicationContext(),"Showing Current Location", Toast.LENGTH_SHORT).show();
                        if(getLastKnownLocation() != null) {
                            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(getLastKnownLocation().getLatitude(), getLastKnownLocation().getLongitude()), 14));
                        }
                    }
                });

                // Create SymbolManager layer to draw markers
                SymbolManager symbolManager = new SymbolManager(mapView, mapboxMap, style);

                // Set OnClickListener for individual markers
                symbolManager.addClickListener(new OnSymbolClickListener() {
                    @Override
                    public void onAnnotationClick(Symbol symbol) {
                        Toast.makeText(getApplicationContext(), "Loading" , Toast.LENGTH_SHORT).show();
                        for (int i = 0; i < eventsList.size(); i++) {
                            if (symbol.getTextJustify().equals(eventsList.get(i).getUniqueID())) {
                                Log.i(eventsList.get(i).getEventName(), "DEBUG");
                                openInfoActivity(eventsList.get(i));
                                break;
                            }
                        }
                    }
                });
                symbolManager.setIconAllowOverlap(true);
                symbolManager.setIconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT);
                mSymbolManager = symbolManager;

                // Load Resources
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icons8_italian_pizza_64);
                mapboxMap.getStyle().addImage("food-marker", bm);
                bm = BitmapFactory.decodeResource(getResources(), R.drawable.icons8_party_balloons_64);
                mapboxMap.getStyle().addImage("entertainment-marker", bm);
            }
        });
    }

    private void addEvent(Event event) {
        eventsList.add(event);
        Log.i("DEBUG ADD", event.getEventName());
    }


    private boolean removeEvent(String uniqueID) {
        for (int i = 0; i < eventsList.size(); i++) {
            if (uniqueID.equals(eventsList.get(i).getUniqueID())) {
                eventsList.remove(i);
                return true;
            }
        }
        return false;
    }

    // Refresh the symbol layer with eventsList
    private void repopulateSymbols() {
        if(mSymbolManager != null) {
            clearSymbols();
            ArrayList<SymbolOptions> symbolOptionsList = new ArrayList<>();
            Log.i("DEBUG S", String.valueOf(filteredList.size()));
            for (int i = 0; i < filteredList.size(); i++) {
                symbolOptionsList.add(filteredList.get(i).toSymbol());
            }
            currentSymbols = mSymbolManager.create(symbolOptionsList);
        }
    }

    private void filterSymbols () {
        filteredList.clear();
        ArrayList<String> filters = new ArrayList<>();
        for(Map.Entry<String, Boolean> entry : filterOptions.entrySet()) {
            if(entry.getValue()) {
                filters.add(entry.getKey());
            }
        }
        for(Event event : eventsList) {
            if(filters.contains(event.getEventType())) {
                filteredList.add(event);
            }
        }
    }

    // Remove all current symbols from map (preserves the list);
    private void clearSymbols() {
        mSymbolManager.delete(currentSymbols);
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        LocationComponent locationComponent;
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            LocationComponentOptions locationComponentOptions = LocationComponentOptions
                    .builder(this)
                    .build();
            LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions
                    .builder(this, loadedMapStyle)
                    .locationComponentOptions(locationComponentOptions)
                    .build();
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(activationOptions);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);

        } else {
            PermissionsManager permissionsManager = new PermissionsManager(new PermissionsListener() {
                @Override
                public void onExplanationNeeded(List<String> permissionsToExplain) {

                }

                @Override
                public void onPermissionResult(boolean granted) {

                }
            });
            permissionsManager.requestLocationPermissions(this);
            enableLocationComponent(loadedMapStyle);
        }
    }


    @SuppressWarnings({"MissingPermission"})
    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public void openInfoActivity(Event mEvent) {
        Intent intent = new Intent(this, InfoActivity.class);
        intent.putExtra("EXTRA_EVENT", mEvent);  // Pass event selected to info page
        startActivity(intent);
    }

    public void openEventActivity(Location location) {
        Intent intent = new Intent(this, EventActivity.class);
        intent.putExtra("EXTRA_LOC", location);
        startActivity(intent);
    }

    public void openFilterActivity() {
        Intent intent = new Intent(this, FilterActivity.class);
        intent.putExtra("EXTRA_OPTIONS", filterOptions);
        startActivityForResult(intent, 100, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                filterOptions = (HashMap<String, Boolean>) data.getSerializableExtra("sortBy");
                Log.i("DEBUG RESULT", filterOptions.toString());
                filterSymbols();
                repopulateSymbols();
            } else {
                Log.d("DEBUG", "onActivityResult: canceled");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_action, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        repopulateSymbols();
        return true;
    }
}