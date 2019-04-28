package com.shtvsolution.recordatorios.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.shtvsolution.recordatorios.model.Reminder;
import com.shtvsolution.recordatorios.receiver.ReminderReceiver;
import com.shtvsolution.recordatorios.view.ReminderActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Calendar;

/**
 * Manages external intents for creating and deleting alarms
 *
 * @author Eduardo on 20/03/2018
 */

public class IntentManager {

    private static final String TAG = IntentManager.class.getCanonicalName();

    //A reference to the reminder activity so its context can be retrieved before starting a new reminder:
    private static WeakReference<ReminderActivity> reminderActivityReference;
    public static void setReminderActivityReference(ReminderActivity activity) {
        reminderActivityReference = new WeakReference<>(activity);
    }

    private static Intent reminderIntent;

    private static Context context;

    /**
     * Retrieves the information from the intent and performs the corresponding operations
     */
    public static void manageIntent(Context context, Intent intent) {

        IntentManager.reminderIntent = intent;
        IntentManager.context = context;

        int numberOfEvents = reminderIntent.getIntExtra("numberOfEvents", -1);

        byte[] reminderBytes = reminderIntent.getByteArrayExtra("reminderBytes");
        boolean deleteAlarm = reminderIntent.getBooleanExtra("deleteAlarm", false);

        boolean callEnded = reminderIntent.getBooleanExtra("callEnded", false);

        if (callEnded && ReminderActivity.isCreated()) {
            Intent missedCallIntent = new Intent();
            missedCallIntent.putExtra("reminderIntent", true);
            missedCallIntent.putExtra("callMissed", true);
            missedCallIntent.setComponent(new ComponentName("com.shtvsolution.videoconferencia", "com.shtvsolution.videoconferencia.services.MissedCallsService"));

            reminderActivityReference.get().startService(missedCallIntent);

            reminderActivityReference.get().noneButtonPressed();
        }

        //If the intent came from another app, the alarm must be set or deleted using the reminder stored within that intent:
        else if (reminderBytes != null) {

            Reminder newReminder = ParcelableUtils.unmarshall(reminderBytes, Reminder.CREATOR);

            if (deleteAlarm) CalendarManager.deleteAlarm(context, newReminder);
            else createAlarm(newReminder);
        }

        //If the extra numberOfEvents is different from -1, then the intent has more than one reminder (multiple alarms will be created or deleted):
        else if (numberOfEvents != -1) {

            Reminder newReminder;

            if (deleteAlarm) for (int i = 0; i < numberOfEvents; i++) {
                reminderBytes = reminderIntent.getByteArrayExtra("reminderBytes" + i);
                newReminder = ParcelableUtils.unmarshall(reminderBytes, Reminder.CREATOR);
                CalendarManager.deleteAlarm(context, newReminder);
            }

            else for (int i = 0; i < numberOfEvents; i++) {
                reminderBytes = reminderIntent.getByteArrayExtra("reminderBytes" + i);
                newReminder = ParcelableUtils.unmarshall(reminderBytes, Reminder.CREATOR);
                createAlarm(newReminder);
            }
        }
    }

    /**
     * Creates an alarm for a single reminder
     *
     * @param newReminder the reminder that will generate the alarm
     */
    private static void createAlarm(Reminder newReminder) {

        Bundle additionalOptions = newReminder.getAdditionalOptions();

        boolean instantlyShown = additionalOptions.getBoolean("instantlyShown");

        //If the reminder must be instantly shown, no alarms are created after showing it:
        if (instantlyShown) {
            ReminderReceiver.launchReminder(context, newReminder);
            //Once the reminder have been instantly shown, the additional option is deleted before setting the alarm:
            newReminder.deleteInstantlyShown();
        }

        else CalendarManager.createAlarm(context, newReminder, false);
    }
}
