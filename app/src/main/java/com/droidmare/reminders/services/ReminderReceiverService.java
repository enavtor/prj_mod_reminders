package com.droidmare.reminders.services;

import android.content.Intent;

import com.droidmare.reminders.utils.CalendarManager;
import com.droidmare.reminders.views.ReminderActivity;
import com.droidmare.common.models.ConstantValues;
import com.droidmare.common.models.EventJsonObject;
import com.droidmare.common.services.CommonIntentService;

import java.lang.ref.WeakReference;

//Service that receives and processes external reminders
//@author Eduardo on 28/06/2018.
public class ReminderReceiverService extends CommonIntentService {

    //A reference to the reminder activity so its context can be retrieved before starting a new reminder:
    private static WeakReference<ReminderActivity> reminderActivityReference;
    public static void setReminderActivityReference(ReminderActivity activity) {
        reminderActivityReference = new WeakReference<>(activity);
    }

    public ReminderReceiverService() {
        super("ReminderReceiverService");
    }

    @Override
    public void onHandleIntent(Intent reminderIntent) {

        COMMON_TAG = getClass().getCanonicalName();

        super.onHandleIntent(reminderIntent);

        boolean deleteAlarm = reminderIntent.getBooleanExtra(ConstantValues.DELETE_ALARM_OP, false);

        String[] eventJsonStrings = reminderIntent.getStringArrayExtra(ConstantValues.EVENT_JSON_FIELD);

        for (String eventJsonString : eventJsonStrings) {
            EventJsonObject eventJson = EventJsonObject.createEventJson(eventJsonString);
            if (deleteAlarm) CalendarManager.deleteAlarm(getApplicationContext(), eventJson);
            else CalendarManager.createAlarm(getApplicationContext(), eventJson, false);
        }
    }
}