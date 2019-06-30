package com.anqingchen.onthemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    com.github.clans.fab.FloatingActionButton addEventBtn;

    MapboxMap mapboxMap;
    MapView mapView;
    ArrayList<Event> eventsList = new ArrayList<>();
    List<Symbol> currentSymbols = new ArrayList<>();
    LocationManager mLocationManager;
    SymbolManager mSymbolManager;

    ValueEventListener postListener;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load Mapbox Map with my API key
        Mapbox.getInstance(this, "pk.eyJ1Ijoic2FtYXJpdGFucyIsImEiOiJjanhjaHN6OXowM2twM3dvY3k1Z2k2bWQzIn0.qNMnSU_p4akStUv8Z8uQ6w");
        setContentView(R.layout.activity_main);

        // Initialize the map
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Initialize addEventBtn to load the add event page
        addEventBtn = findViewById(R.id.menu_item);
        addEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "CLICKED ME", Toast.LENGTH_SHORT).show();
                openEventActivity(getLastKnownLocation());
            }
        });

        // Initialize Firebase Database to update list of events
        mDatabase = FirebaseDatabase.getInstance().getReference().child("events");
        postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.i("DATA CHANGE TRIGGERED", "DEBUG");
                for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                    Event event = childSnapshot.getValue(Event.class);
                    newEvent(event);
                }
                repopulateSymbols();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mDatabase.addValueEventListener(postListener);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.DARK, new Style.OnStyleLoaded() {
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
                        Toast.makeText(getApplicationContext(), "clicked  " + symbol.getTextField().toLowerCase().toString(), Toast.LENGTH_SHORT).show();
                        Log.i("DING", "DEBUG");
                        for (int i = 0; i < eventsList.size(); i++) {
                            if (symbol.getTextField() == eventsList.get(i).getEventName()) {
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
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icons8_beef_burger_64);
                mapboxMap.getStyle().addImage("my-marker", bm);
            }
        });

    }

    private SymbolOptions toSymbol(Event event) {
        return new SymbolOptions()
                .withLatLng(event.getEventLatLng())
                .withTextField(event.getEventName())
                .withIconImage("my-marker");
    }

    private void newEvent(Event event) {
        eventsList.add(event);
    }


    private boolean removeEvent(String eventName) {
        for (int i = 0; i < eventsList.size(); i++) {
            if (eventName.equals(eventsList.get(i).getEventName())) {
                eventsList.remove(i);
                return true;
            }
        }
        return false;
    }

    // Refresh the symbol layer with eventsList
    private void repopulateSymbols() {
        if(mSymbolManager != null) {
            clearSymbols(mSymbolManager);
            ArrayList<SymbolOptions> symbolOptionsList = new ArrayList<>();
            for (int i = 0; i < eventsList.size(); i++) {
                symbolOptionsList.add(toSymbol(eventsList.get(i)));
            }
            currentSymbols = mSymbolManager.create(symbolOptionsList);
        }
    }

    // Remove all current symbols from map (preserves the list);
    private void clearSymbols(SymbolManager symbolManager) {
        symbolManager.delete(currentSymbols);
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

}
