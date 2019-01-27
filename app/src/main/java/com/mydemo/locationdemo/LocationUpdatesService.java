

package com.mydemo.locationdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mydemo.model.MyLocation;
import com.mydemo.utility.MFunction;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationUpdatesService extends Service {

    private static final String PACKAGE_NAME = "com.mydemo.locationdemo.service";
    private static final String TAG = LocationUpdatesService.class.getSimpleName();
    private static final String CHANNEL_ID = "channel_01";
    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification";
    private final IBinder mBinder = new LocalBinder();
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = java.util.concurrent.TimeUnit.SECONDS.toMillis(15);
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final long ACCEPTABLE_FUSED_LOCATION_ACCURACY = 30;
    private static final int NOTIFICATION_ID = 12345678;
    private boolean mChangingConfiguration = false;
    private NotificationManager mNotificationManager;
    private LocationRequest mLocationRequest;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private Intent mIntentService;
    private PendingIntent mPendingIntent;


    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Handler mServiceHandler;
    private Location mLocation;
    private String mActivity;
    private String mActivityTransitionType;
    private float mAccuracy;
    Boolean checkinClicked = false;
    private int stillDataCount = 0;

    public LocationUpdatesService() {
    }

    @Override
    public void onCreate() {

        //mActivityRecognitionClient = new ActivityRecognitionClient(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };
        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        checkinClicked = intent.getBooleanExtra("checkin_clicked", false);
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        removeActivityTransitionUpdate();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");

        // Called when the last client (MainActivity) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (MFunction.isCheckin(getApplicationContext())) {

            Log.i(TAG, "Starting foreground service");
            // targeting O, use the following code.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(getApplicationContext(),new Intent(this, LocationUpdatesService.class));
            } else {
                startForeground(NOTIFICATION_ID, getNotification());
            }
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
        removeActivityTransitionUpdate();
    }

    public void requestActivityTransitionUpdate(){

        ActivityTransitionRequest activityTransitionRequest = buildActivityTransitionRequest();
        Intent tempIntent = new Intent(this, ReceiverActivityTransition.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 19880415, tempIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Task task = ActivityRecognition.getClient(this).requestActivityTransitionUpdates(activityTransitionRequest, pendingIntent);

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i("Success","requestActivityTransitionUpdate");
                    }
                }
        );

        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        // Handle error
                        Log.i("Success","LocationUpdatesService");

                    }
                }
        );
    }


    public void removeActivityTransitionUpdate(){

        Intent tempIntent = new Intent(this, ReceiverActivityTransition.class);
        final PendingIntent myPendingIntent = PendingIntent.getBroadcast(this, 0, tempIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Task<Void> task = ActivityRecognition.getClient(this).removeActivityTransitionUpdates(myPendingIntent);

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        myPendingIntent.cancel();
                        Log.i("success","removeActivityTransitionUpdate");
                    }
                });

        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e("fail",e.toString());
                    }
                });


    }


    public void requestLocationUpdates() {

        // lets start activity transition request

        requestActivityTransitionUpdate();

        Log.i(TAG, "Requesting location updates");
        Utils.setRequestingLocationUpdates(this, true);
        Intent intent = new Intent(getApplicationContext(), LocationUpdatesService.class);
        boolean clicked = Utils.checkinClicked(getApplicationContext());
        intent.putExtra("checkin_clicked", clicked);
        startService(intent);

        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }



    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Utils.setRequestingLocationUpdates(this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, LocationUpdatesService.class);

        CharSequence text = Utils.getLocationText(mLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("Location Tracking")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("")
                .setWhen(System.currentTimeMillis());

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        Notification notification = builder.build();
        notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;

        return builder.build();
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        //Log.i(TAG, "New location: " + location);

        if (location != null) {
            mLocation = location;
            mAccuracy = location.getAccuracy();

            if(checkinClicked){

                saveToDatabase(location,false);
                checkinClicked = false;
                Utils.setCheckinClicked(getApplicationContext(),false);
            } else{
                //takeActionOnActivity();
                takeActionOnActivityTransitionType();
            }

            MFunction.putMyPrefVal("latitude",Double.toString(location.getLatitude()),getApplicationContext());
            MFunction.putMyPrefVal("longitude",Double.toString(location.getLongitude()),getApplicationContext());
        }
    }


    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    class LocalBinder extends Binder {
        public LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }

    private void takeActionOnActivityTransitionType(){

        mActivity = MFunction.getMyPrefVal("current_activity",getApplicationContext());
        if(mActivity == null) mActivity ="3";

        mActivityTransitionType = MFunction.getMyPrefVal("current_activity_transition_type",getApplicationContext());
        if(mActivityTransitionType == null) mActivityTransitionType ="0";

        mAccuracy = mLocation.getAccuracy();
        if(!TextUtils.isEmpty(mActivityTransitionType)) {
            switch (Integer.parseInt(mActivityTransitionType)) {

                case 0: {
                    if (mAccuracy < ACCEPTABLE_FUSED_LOCATION_ACCURACY) {
                        saveToDatabase(mLocation, true);
                        Log.i("STILL", "***************************STILL***********************************");
                    }

                    break;
                }
                case 1: {
                    if (mAccuracy < ACCEPTABLE_FUSED_LOCATION_ACCURACY) {
                        saveToDatabase(mLocation, false);
                        Log.i("MOVING", "***************************MOVING***********************************");
                    }
                    break;
                }

                default:{
                    break;
                }
            }
        }
    }



    private void saveToDatabase(android.location.Location location,boolean updateRecent){

        Context tempContext = getApplicationContext();

        String locationRef = "locations/"+MFunction.getAndroidSecureID(getApplicationContext())+"/"+MFunction.getCurrentDate();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(locationRef);
        String deviceInfo = MFunction.getDeviceInfo();
        int batLevel = MFunction.getBatteryLevel(getApplicationContext());
        String mActivity = MFunction.getMyPrefVal("current_activity",getApplicationContext());

        MyLocation mLocation= new MyLocation();
        mLocation.unique_id = MFunction.getUID();
        mLocation.activity = mActivity;
        mLocation.latitude = location.getLatitude();
        mLocation.longitude = location.getLongitude();
        mLocation.altitude = location.getAltitude();
        mLocation.battery_level = batLevel;
        mLocation.device_indo = deviceInfo;
        mLocation.created_at = MFunction.getCurrentDateTime();
        mLocation.unix_time = MFunction.getUnixTimestamp();
        mLocation.still = updateRecent;


        ref.push().setValue(mLocation);



    }


    private ActivityTransitionRequest buildActivityTransitionRequest() {
        List transitions = new ArrayList<>();
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        return new ActivityTransitionRequest(transitions);
    }



    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

}
