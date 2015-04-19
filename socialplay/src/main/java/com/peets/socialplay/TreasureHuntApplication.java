package com.peets.socialplay;

import android.app.Application;

import com.facebook.FacebookSdk;

import org.json.JSONObject;

import java.util.List;

/**
 * Use a custom Application class to pass state data between Activities.
 */
public class TreasureHuntApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
    }

    private List<JSONObject> selectedUsers;
    private JSONObject selectedPlace;

    public List<JSONObject> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<JSONObject> users) {
        selectedUsers = users;
    }

    public JSONObject getSelectedPlace() {
        return selectedPlace;
    }

    public void setSelectedPlace(JSONObject place) {
        this.selectedPlace = place;
    }
}

