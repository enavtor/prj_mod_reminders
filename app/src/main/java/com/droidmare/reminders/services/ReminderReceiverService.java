package com.droidmare.reminders.services;

import android.content.Intent;
import android.util.Log;

import com.droidmare.reminders.utils.CalendarManager;
import com.droidmare.common.models.ConstantValues;
import com.droidmare.common.models.EventJsonObject;
import com.droidmare.common.services.CommonIntentService;
import com.droidmare.reminders.views.ReminderActivity;

//Service that receives and processes external reminders
//@author Eduardo on 28/06/2018.

public class ReminderReceiverService extends CommonIntentService {

    public static boolean PERMISSION_GRANTED = false;

    public ReminderReceiverService() {
        super("ReminderReceiverService");
    }

    @Override
    public void onHandleIntent(Intent reminderIntent) {

        COMMON_TAG = getClass().getCanonicalName();

        super.onHandleIntent(reminderIntent);

        boolean deleteAlarm = reminderIntent.getBooleanExtra(ConstantValues.DELETE_ALARM_OP, false);

        String[] eventJsonStrings = reminderIntent.getStringArrayExtra(ConstantValues.EVENT_JSON_FIELD);

        //Before proceeding, it is necessary to check the permissions:
        startActivity(new Intent(getApplicationContext(), ReminderActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("requestingPermission", true));

        //Until the permission is granted, this service will be paused:
        while (!PERMISSION_GRANTED) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Log.e(COMMON_TAG, "onHandelIntent. InterruptedException: " + ie.getMessage());
            }
        }

        for (String eventJsonString : eventJsonStrings) {
            EventJsonObject eventJson = EventJsonObject.createEventJson(eventJsonString);
            if (deleteAlarm) CalendarManager.deleteAlarm(getApplicationContext(), eventJson);
            else CalendarManager.createAlarm(getApplicationContext(), eventJson, false);
        }
    }
}