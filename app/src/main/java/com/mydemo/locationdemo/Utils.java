

package com.mydemo.locationdemo;


import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;


import java.text.DateFormat;
import java.util.Date;

public class Utils {

    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates";

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    public static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    public static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }


    /**
     * Stores the checkin button clicked state in SharedPreferences.
     * @param checkinClicked clicked.
     */
    public static void setCheckinClicked(Context context, boolean checkinClicked) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("checkin_clicked", checkinClicked)
                .apply();
    }


    /**
     * Returns true if checkin clicked, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    public static boolean checkinClicked(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("checkin_clicked", false);
    }

    /**
     * Returns the {@code location} object as a human readable string.
     * @param location  The {@link Location}.
     */
    public static String getLocationText(Location location) {
        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    static String getLocationTitle(Context context) {
        return "Updated";
    }
}
