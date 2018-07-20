package com.example.elrayes.wslt.Util;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * Created by LeGenÐ on 5/12/2018.
 */

public class SharedPreferencesHelper {

    private static String PREF_KEY = "Wslt";

    public static void storeDataToSharedPref(Context context, String data, String key) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedpreferences.edit();
        prefsEditor.putString(key, data);
        prefsEditor.apply();
    }

    public static String retrieveDataFromSharedPref(Context context, String key) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        String data = sharedpreferences.getString(key, null);
        return data;
    }

    public static void clearAllSavedSharedData(Context context) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedpreferences.edit();
        prefsEditor.clear();
        prefsEditor.apply();
    }
}
