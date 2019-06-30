package com.anqingchen.onthemap;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import java.util.UUID;

public class Event implements Parcelable {

    private LatLng eventLatLng;
    private String eventName;
    private String eventDesc;
    private String uniqueID;
    private String eventType;

    public Event(double lat, double lang, String eventName, String eventDesc, String eventType) {
        this.eventLatLng = new LatLng(lat, lang);
        this.eventName = eventName;
        this.eventDesc = eventDesc;
        this.uniqueID = UUID.randomUUID().toString();
        this.eventType = eventType;
    }

    public Event(LatLng latLng, String eventName, String eventDesc, String eventType) {
        this.eventLatLng = latLng;
        this.eventName = eventName;
        this.eventDesc = eventDesc;
        this.uniqueID = UUID.randomUUID().toString();
        this.eventType = eventType;
    }

    private Event(Parcel in) {
        this.eventLatLng = (LatLng) in.readValue(LatLng.class.getClassLoader());
        this.eventName = in.readString();
        this.eventDesc = in.readString();
        this.uniqueID = in.readString();
        this.eventType = in.readString();
    }

    // Default constructor required for calls to DataSnapshot.getValue(Event.class)
    public Event() { }

    // Setters
    public void setEventLatLng(LatLng eventLatLng) {
        this.eventLatLng = eventLatLng;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setEventDesc(String eventDesc) {
        this.eventDesc = eventDesc;
    }

    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    // Getters
    public LatLng getEventLatLng() {
        return eventLatLng;
    }

    public String getEventName() {
        return eventName;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public String getEventDesc() {
        return eventDesc;
    }

    public String getEventType() {
        return eventType;
    }

    // Parcelable Implementation
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeValue(eventLatLng);
        parcel.writeString(eventName);
        parcel.writeString(eventDesc);
        parcel.writeString(uniqueID);
        parcel.writeString(eventType);
    }

    // Parcelables CREATOR that implements these two methods
    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    public SymbolOptions toSymbol() {
        String iconImage = null;
        switch(eventType) {
            case "Food":
                iconImage = "food-marker";
                break;
            case "Entertainment":
                iconImage = "entertainment-marker";
                break;
        }
        return new SymbolOptions()
                .withLatLng(getEventLatLng())
                .withTextJustify(getUniqueID())
                .withIconImage(iconImage);
    }
}
