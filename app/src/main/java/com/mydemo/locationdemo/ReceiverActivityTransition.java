package com.mydemo.locationdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.mydemo.utility.MFunction;

public class ReceiverActivityTransition extends BroadcastReceiver {

    public ReceiverActivityTransition() {
        super();
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                // chronological sequence of events....
                int activityType = event.getActivityType();
                int transitionType = event.getTransitionType();
                Log.i("mahen","mahen");

                MFunction.putMyPrefVal("current_activity", String.valueOf(activityType), context);
                MFunction.putMyPrefVal("current_activity_transition_type", String.valueOf(transitionType), context);


                //String activityRef = "activity/"+ MFunction.getMyPrefVal("domain", context)+"/"+MFunction.getCurrentDate()+"/empid:"+MFunction.getMyPrefVal("employee_id",MyApp.getContext());
                //DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(activityRef);
                //mDatabase.push().setValue("Act("+String.valueOf(activityType)+","+transitionType+")"+MFunction.getCurrentTime());
                //broadcastActivity(context, activityType+""+transitionType);
            }
        }
    }

    private void broadcastActivity(Context mContext, String transitionInfo) {
        Intent intent = new Intent("activity_transition");
        intent.putExtra("transitionInfo", transitionInfo);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

}