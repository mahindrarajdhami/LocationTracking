package com.mydemo.locationdemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.mydemo.utility.MFunction;

public class MainActivity extends AppCompatActivity {

    private Button btnCheckinCheckout;
    private Context mContext;
    private boolean isCheckin;

    private LocationUpdatesService mService = null;
    private boolean mBound = false;

    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 34;




    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if(MFunction.isCheckin(getApplicationContext())){
                startTrackerService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = this;
        isCheckin = MFunction.isCheckin(mContext);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        btnCheckinCheckout = findViewById(R.id.btnCheckinCheckout);
        toggleButton(isCheckin);
        btnCheckinCheckout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAttendance(v);

            }
        });



        //region Permissions
        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion >= Build.VERSION_CODES.M)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CALL_PHONE,Manifest.permission.READ_CALENDAR,Manifest.permission.READ_CALENDAR}, 101);
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        if(MFunction.isCheckin(mContext)){
            Intent tempIntent = new Intent(this, LocationUpdatesService.class);
            tempIntent.putExtra("checkin_clicked",false);
            boolean binded = bindService(tempIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void toggleAttendance(View view) {
        if(!isCheckin){
            //do checkin and start tracking
            MFunction.putMyPrefVal("is_checkin","true",mContext);
            isCheckin = true;
            toggleButton(true);
            Utils.setCheckinClicked(mContext, true);
            if(mBound){
                startTrackerService();
            } else {
                boolean binded = bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
            }

        } else {
            //do checkout and stop tracking
            MFunction.putMyPrefVal("is_checkin","false",mContext);
            isCheckin = false;
            stopTrackerService();
            toggleButton(false);

        }
    }


    private void toggleButton(boolean isCheckin){
        if(isCheckin){
            btnCheckinCheckout.setText("Checkout");

        } else {
            btnCheckinCheckout.setText("Checkin");

        }
    }

    private void startTrackerService() {

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            if(mService !=null){
                mService.requestActivityTransitionUpdate();
                mService.requestLocationUpdates();
            }
        }

    }

    private void startTrackerService(Boolean checkinClicked) {

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            mService.requestLocationUpdates();
        }

    }

    private void stopTrackerService() {
        if(mService != null) mService.removeActivityTransitionUpdate();
        if(mService != null) mService.removeLocationUpdates();
    }



    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        return  PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            //show info
        } else {
            Log.i("Requesting Permission", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i("onReqPermissionResult", "onRequestPermissionResult");
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("User", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
            } else {
                // Permission denied.
                Log.i("mahen","nopermission");

            }
        }
    }
}
