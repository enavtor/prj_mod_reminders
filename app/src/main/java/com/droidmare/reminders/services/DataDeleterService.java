package com.droidmare.reminders.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Intent;

//App's data deleter service declaration
//@author Eduardo on 24/05/2018.

public class DataDeleterService extends IntentService {

    public DataDeleterService() {
        super("DataDeleterService");
    }

    @Override
    public void onHandleIntent(Intent eventIntent) {

        UserDataReceiverService.resetUser();

        ((ActivityManager)getApplicationContext().getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
    }
}