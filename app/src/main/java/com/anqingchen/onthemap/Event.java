package com.anqingchen.onthemap;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import java.util.UUID;

public class Event implements Parcelable {

    private LatLng eventLatLng;
    private String eventName;
    private String eventDesc;
    private String uniqueID;
    private String eventType;
    private long eventStartDate, eventEndDate;      // Event Start/End times are stored in UTC Epoch time

    // Constructors
    public Event(double lat, double lang, String eventName, String eventDesc, String eventType, long eventStartDate, long eventEndDate) {
        this.eventLatLng = new LatLng(lat, lang);
        this.eventName = eventName;
        this.eventDesc = eventDesc;
        this.uniqueID = UUID.randomUUID().toString();
        this.eventType = eventType.toUpperCase();
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
    }

    public Event(LatLng latLng, String eventName, String eventDesc, String eventType, long eventStartDate, long eventEndDate) {
        this.eventLatLng = latLng;
        this.eventName = eventName;
        this.eventDesc = eventDesc;
        this.uniqueID = UUID.randomUUID().toString();
        this.eventType = eventType.toUpperCase();
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
    }


    // Parcelable Config
    private Event(Parcel in) {
        this.eventLatLng = (LatLng) in.readValue(LatLng.class.getClassLoader());
        this.eventName = in.readString();
        this.eventDesc = in.readString();
        this.uniqueID = in.readString();
        this.eventType = in.readString().toUpperCase();
        this.eventStartDate = in.readLong();
        this.eventEndDate = in.readLong();
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
        this.eventType = eventType.toUpperCase();
    }

    public void setEventStartDate(long eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    public void setEventEndDate(long eventEndDate) {
        this.eventEndDate = eventEndDate;
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

    public long getEventStartDate() {
        return eventStartDate;
    }

    public long getEventEndDate() {
        return eventEndDate;
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
        parcel.writeLong(eventStartDate);
        parcel.writeLong(eventEndDate);
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
        Log.i("DEBUG SYMBOL", "IVE BEEN CALLED");
        String iconImage = null;
        switch(eventType) {
            case "FOOD":
                iconImage = "food-marker";
                break;
            case "ENTERTAINMENT":
                iconImage = "entertainment-marker";
                break;
        }
        return new SymbolOptions()
                .withLatLng(getEventLatLng())
                .withTextJustify(getUniqueID())
                .withIconImage(iconImage);
    }
}
