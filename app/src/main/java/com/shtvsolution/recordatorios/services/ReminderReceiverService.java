package com.shtvsolution.recordatorios.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.shtvsolution.recordatorios.utils.IntentManager;

//Service that receives and processes external reminders
//@author Eduardo on 28/06/2018.

public class ReminderReceiverService extends IntentService {

    public ReminderReceiverService() {
        super("UserDataReceiverService");
    }

    @Override
    public void onHandleIntent(Intent reminderIntent) {

        IntentManager.manageIntent(this, reminderIntent);
    }
}