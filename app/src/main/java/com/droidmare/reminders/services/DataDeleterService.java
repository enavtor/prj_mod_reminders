package com.droidmare.reminders.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Intent;

import com.droidmare.common.services.CommonIntentService;

//App's data deleter service declaration
//@author Eduardo on 24/05/2018.

public class DataDeleterService extends CommonIntentService {

    public DataDeleterService() {
        super("DataDeleterService");
    }

    @Override
    public void onHandleIntent(Intent eventIntent) {

        COMMON_TAG = getClass().getCanonicalName();

        ActivityManager activityManager = (ActivityManager)getApplicationContext().getSystemService(ACTIVITY_SERVICE);

        if (activityManager != null) activityManager.clearApplicationUserData();
    }
}