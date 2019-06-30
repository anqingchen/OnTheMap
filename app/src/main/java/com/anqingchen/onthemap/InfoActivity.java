package com.anqingchen.onthemap;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

public class InfoActivity extends AppCompatActivity {

    TextView infoText, titleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        infoText = findViewById(R.id.infoTextView);
        titleText = findViewById(R.id.titleTextView);
        Event tempEvent = getIntent().getParcelableExtra("EXTRA_EVENT");
        infoText.setText(tempEvent.getEventLatLng().toString());
        titleText.setText(tempEvent.getEventName());
    }

}
