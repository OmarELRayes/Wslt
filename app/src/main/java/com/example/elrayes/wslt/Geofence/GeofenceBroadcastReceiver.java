package com.example.elrayes.wslt.Geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;

import com.example.elrayes.wslt.Util.SharedPreferencesHelper;

import java.util.Calendar;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Geofencing.getInstance(context, null).unRegisterAllGeofences();
        // TODO: 7/20/2018  Set Alarm
        Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        alarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        Calendar c = Calendar.getInstance();
        alarmIntent.putExtra(AlarmClock.EXTRA_VIBRATE, true);
        alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, c.get(Calendar.HOUR_OF_DAY));
        alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, c.get(Calendar.MINUTE) + 1);
        alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "You Arrived!");
        SharedPreferencesHelper.storeDataToSharedPref(context, "off", "geofence");
        context.startActivity(alarmIntent);
    }
}
