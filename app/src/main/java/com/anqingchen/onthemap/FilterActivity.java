package com.anqingchen.onthemap;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import java.util.HashMap;

public class FilterActivity extends AppCompatActivity {

    Switch foodSwitch, entertainmentSwitch;
    HashMap<String, Boolean> filterOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        foodSwitch = findViewById(R.id.switch1);
        entertainmentSwitch = findViewById(R.id.switch2);

        filterOptions = (HashMap<String, Boolean>) getIntent().getSerializableExtra("EXTRA_OPTIONS");

        foodSwitch.setChecked(filterOptions.get("FOOD"));
        entertainmentSwitch.setChecked(filterOptions.get("ENTERTAINMENT"));

//        for(Map.Entry<String, Boolean> entry: filterOptions.entrySet()) {
//            switch(entry.getKey()) {
//                case "FOOD":
//                    foodSwitch.setChecked(entry.getValue());
//                    break;
//                case "ENTERTAINMENT":
//                    entertainmentSwitch.setChecked(entry.getValue());
//                    break;
//            }
//        }

        Button button = findViewById(R.id.button3);
        button.setOnClickListener(view -> {
            filterOptions.put("FOOD", foodSwitch.isChecked());
            filterOptions.put("ENTERTAINMENT", entertainmentSwitch.isChecked());
            Intent intent = new Intent();
            intent.putExtra("sortBy", filterOptions);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }

    public void goBack(View view) {
        setResult(RESULT_CANCELED);
        onBackPressed();
    }
}
