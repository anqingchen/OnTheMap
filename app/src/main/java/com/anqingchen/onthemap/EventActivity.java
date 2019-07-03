package com.anqingchen.onthemap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class EventActivity extends AppCompatActivity {

    EditText name, desc, addr;
    Button doBtn;
    private DatabaseReference mDatabase;
    Spinner typeSpinner;
    DatePickerDialog startDpd, endDpd;
    TimePickerDialog startTpd, endTpd;
    Calendar startDate, endDate;
    TextView startDateText, startTimeText, endDateText, endTimeText;
    ImageButton newImage;
    Bitmap bitmap;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    Uri filePath;

    public static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        name = findViewById(R.id.eventNameText);
        desc = findViewById(R.id.editText2);
        addr = findViewById(R.id.editText7);
        doBtn = findViewById(R.id.button);
        startDateText = findViewById(R.id.textView7);
        startTimeText = findViewById(R.id.textView8);
        endDateText = findViewById(R.id.textView9);
        endTimeText = findViewById(R.id.textView10);
        newImage = findViewById(R.id.newImage);

        // Initialize top tool bar
        Toolbar toolbar = findViewById(R.id.eventToolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.getNavigationIcon().setColorFilter(getColor(android.R.color.black), PorterDuff.Mode.SRC_ATOP);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize Firebase Storage
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        newImage.setOnClickListener(view -> {
            Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getIntent.setType("image/*");
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/*");
            Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});
            startActivityForResult(chooserIntent, PICK_IMAGE);
        });

        // Configure Date/Time displays
        String datePattern = "MMMM dd,yyyy";
        String timePattern = "KK:mm a";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(timePattern);

        // Initialize Spinner for picking activity types
        typeSpinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.type_options_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        // Get user's current location from intent
        final Location cLocation = getIntent().getParcelableExtra("EXTRA_LOC");

        // Timestamp for reference
        Calendar now = Calendar.getInstance();
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();

        // Configure Date/Time Picker Dialogs
        startDpd = DatePickerDialog.newInstance(
                (view, year, monthOfYear, dayOfMonth) -> {
                    startDate.set(year, monthOfYear, dayOfMonth);
                    startDateText.setText(simpleDateFormat.format(new Date(startDate.getTimeInMillis())));
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        endDpd = DatePickerDialog.newInstance(
                (view, year, monthOfYear, dayOfMonth) -> {
                    endDate.set(year, monthOfYear, dayOfMonth);
                    endDateText.setText(simpleDateFormat.format(new Date(endDate.getTimeInMillis())));
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        startTpd = TimePickerDialog.newInstance(
                (view, hourOfDay, minute, second) -> {
                    startDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    startDate.set(Calendar.MINUTE, minute);
                    startDate.set(Calendar.SECOND, second);
                    startTimeText.setText(simpleTimeFormat.format(new Date(startDate.getTimeInMillis())));
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );

        endTpd = TimePickerDialog.newInstance(
                (view, hourOfDay, minute, second) -> {
                    endDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    endDate.set(Calendar.MINUTE, minute);
                    endDate.set(Calendar.SECOND, second);
                    endTimeText.setText(simpleTimeFormat.format(new Date(endDate.getTimeInMillis())));
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );



        // OnClickListener for button to submit event add request
        doBtn.setOnClickListener(view -> {
            // Check for form completion
            if(name.getText().toString().isEmpty() || desc.getText().toString().isEmpty() || addr.getText().toString().isEmpty() || startTimeText.getText().toString().equals("Start Time")
            || startDateText.getText().toString().equals("Start Date") || endTimeText.getText().toString().equals("End Time") || endDateText.getText().toString().equals("End Date")) {
                Toast.makeText(EventActivity.this, "NOT ENOUGH INFO", Toast.LENGTH_SHORT).show();
            } else if(startDate.after(endDate)) {   // Check if start time is configured after end time!
               Toast.makeText(EventActivity.this, "Start Date/Time is After the End Time, Please Check Your Inputs", Toast.LENGTH_LONG).show();
            } else {    // store event onto Firebase
                Event newEvent;
                LatLng tempLatLng;
                String address = addr.getText().toString();
                String eventType = typeSpinner.getSelectedItem().toString();
                if(address.equals("Current Location")) {
                    tempLatLng = new LatLng(cLocation.getLatitude(), cLocation.getLongitude());
                } else {
                    tempLatLng = getLocationFromAddress(getApplicationContext(), address);
                }
                newEvent = new Event(tempLatLng, name.getText().toString(), desc.getText().toString(),
                        eventType, startDate.getTimeInMillis(), endDate.getTimeInMillis());
                if(filePath != null) {
                    // add bitmap to firebase here!
                    uploadImage(newEvent);
                }
                writeNewEvent(newEvent);
            }
        });
    }

    // Update Firebase Realtime with newEvent
    private void writeNewEvent(final Event newEvent) {
        mDatabase.child("events").child(newEvent.getUniqueID()).setValue(newEvent, (databaseError, databaseReference) -> {
            if(databaseError != null) {
                Toast.makeText(EventActivity.this, "Your Event Failed To Be Added, Error Msg: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EventActivity.this, "Your Event " + newEvent.getEventName() + " Has Been Successfully Added", Toast.LENGTH_SHORT).show();
                finish();
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

    // OnClick functions for Date/Time Picker TextViews
    public void startDatePicker(View v) {
        startDpd.show(getSupportFragmentManager(), "Start Date");
    }

    public void startTimePicker(View v) {
        startTpd.show(getSupportFragmentManager(), "Start Time");
    }

    public void endDatePicker(View v) {
        endDpd.show(getSupportFragmentManager(), "End Date");
    }

    public void endTimePicker(View v) {
        endTpd.show(getSupportFragmentManager(), "End Time");
    }

    // Get image back from user selection interface
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Get result filterOptions HashMap back
        if (requestCode == PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                try{
                    filePath = data.getData();
                    InputStream inputStream = EventActivity.this.getContentResolver().openInputStream(data.getData());
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    newImage.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    Toast.makeText(EventActivity.this, "Image Does Not Exist", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(EventActivity.this, "Image Selection Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImage(Event event) {
        if(bitmap != null) {
            StorageReference ref = storageReference.child("images/" + event.getUniqueID());
            ref.putFile(filePath).addOnSuccessListener(taskSnapshot -> {
                Toast.makeText(EventActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Log.i("DEBUG UPLOAD", e.getMessage());
            });
        }
    }
}
