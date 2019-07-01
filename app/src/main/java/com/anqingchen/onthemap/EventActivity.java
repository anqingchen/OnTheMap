package com.anqingchen.onthemap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.IOException;
import java.util.List;


public class EventActivity extends AppCompatActivity {

    EditText name, desc, addr;
    Button doBtn;
    private DatabaseReference mDatabase;
    Spinner typeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        name = findViewById(R.id.editText);
        desc = findViewById(R.id.editText2);
        addr = findViewById(R.id.editText7);
        doBtn = findViewById(R.id.button);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        typeSpinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.type_options_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        final Location cLocation = getIntent().getParcelableExtra("EXTRA_LOC");

        // OnClickListener for button to submit event add request
        doBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check for form completion
                if(name.getText().toString().isEmpty() || desc.getText().toString().isEmpty() || addr.getText().toString().isEmpty()) {
                    Toast.makeText(EventActivity.this, "NOT ENOUGH INFO", Toast.LENGTH_SHORT).show();
                } else {
                    Event newEvent;
                    String address = addr.getText().toString();
                    String eventType = typeSpinner.getSelectedItem().toString();
                    if(!address.equals("Current Location")) {
                        LatLng tempLatLng = getLocationFromAddress(getApplicationContext(), address);
                        newEvent = new Event(tempLatLng, name.getText().toString(), desc.getText().toString(), eventType);
                    } else {
                        newEvent = new Event(cLocation.getLatitude(), cLocation.getLongitude(), name.getText().toString(), desc.getText().toString(), eventType);
                    }
                    writeNewEvent(newEvent);
                }
            }
        });
    }

    // Update Firebase Realtime with newEvent
    private void writeNewEvent(final Event newEvent) {
        mDatabase.child("events").child(newEvent.getUniqueID()).setValue(newEvent, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if(databaseError != null) {
                    Toast.makeText(EventActivity.this, "Your Event Failed To Be Added, Error Msg: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EventActivity.this, "Your Event " + newEvent.getEventName() + " Has Been Successfully Added", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    // Get LatLng objects from an address string
    public LatLng getLocationFromAddress(Context context, String strAddress) {
        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;
        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            p1 = new LatLng(location.getLatitude(), location.getLongitude() );
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return p1;
    }
}
