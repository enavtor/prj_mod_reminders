package com.droidmare.reminders.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.droidmare.reminders.utils.IntentManager;

//Service that receives and processes external reminders
//@author Eduardo on 28/06/2018.

public class ReminderReceiverService extends IntentService {

    public ReminderReceiverService() {
        super("ReminderReceiverService");
    }

    @Override
    public void onHandleIntent(Intent reminderIntent) {

        IntentManager.manageIntent(this, reminderIntent);
    }
}