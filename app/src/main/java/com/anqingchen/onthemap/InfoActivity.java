package com.anqingchen.onthemap;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.geometry.LatLng;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;

import android.os.Vibrator;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class InfoActivity extends AppCompatActivity {

    Event event;
    TextView month, date, name, address, startTime, endTime, desc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.getNavigationIcon().setColorFilter(getColor(android.R.color.black), PorterDuff.Mode.SRC_ATOP);

        month = findViewById(R.id.monthText);
        date = findViewById(R.id.dateText);
        name = findViewById(R.id.nameText);
        address = findViewById(R.id.locationText);
        startTime = findViewById(R.id.startTimeText);
        endTime = findViewById(R.id.endTimeText);
        desc = findViewById(R.id.descText);

        final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        event = getIntent().getParcelableExtra("EXTRA_EVENT");

        month.setText(getUTCtoMonth(event.getEventStartDate()));
        date.setText(String.format("%d", getUTCtoDate(event.getEventStartDate())));
        name.setText(event.getEventName().toUpperCase());
        desc.setText(event.getEventDesc());
        address.setText(getAddressFromLocation(InfoActivity.this, event.getEventLatLng()));

        address.setOnLongClickListener(
                view -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + event.getEventLatLng().getLatitude() + "," +
                                    event.getEventLatLng().getLongitude()));
                    vibrator.vibrate(100);
                    startActivity(intent);
                    return false;
                }
        );

        // Configure Date/Time displays
        String datePattern = "E,  MMM dd";
        String timePattern = "HH:mm  z";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(timePattern);
        Date tempDate = new Date(event.getEventStartDate());
        String tempText = simpleDateFormat.format(tempDate) + " at  " + simpleTimeFormat.format(tempDate);
        startTime.setText(tempText);
        tempDate = new Date(event.getEventEndDate());
        tempText = simpleDateFormat.format(tempDate) + " at " + simpleTimeFormat.format(tempDate);
        endTime.setText(tempText);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = getSharableString(event);
                String shareSub = event.getEventName();
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub);
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share using"));
        });
    }

    public String getAddressFromLocation(Context context, LatLng latLng) {
        Geocoder coder = new Geocoder(context);
        List<Address> address = null;
        try {
            address = coder.getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if(address == null) {
            return null;
        } else {
            return address.get(0).getAddressLine(0);
        }
    }

    public String getUTCtoMonth(long time) {
        Date date = new Date(time);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int monthNum = calendar.get(Calendar.MONTH) + 1;
        String month = "ERR";
        switch(monthNum) {
            case 1:
                month = "JAN";
                break;
            case 2:
                month = "FEB";
                break;
            case 3:
                month = "MAR";
                break;
            case 4:
                month = "APR";
                break;
            case 5:
                month = "MAY";
                break;
            case 6:
                month = "JUN";
                break;
            case 7:
                month = "JUL";
                break;
            case 8:
                month = "AUG";
                break;
            case 9:
                month = "SEP";
                break;
            case 10:
                month = "OCT";
                break;
            case 11:
                month = "NOV";
                break;
            case 12:
                month = "DEC";
                break;
        }
        return month;
    }

    public int getUTCtoDate(long time) {
        Date date = new Date(time);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public String getSharableString(Event event) {
        String mString = "Let's go to " + event.getEventName() + ". It's between " + startTime.getText().toString() + " and " +
                endTime.getText().toString() + " at " + address.getText().toString() + ".";
        return mString;
    }
}