package com.shtvsolution.recordatorios.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.shtvsolution.recordatorios.R;
import com.shtvsolution.recordatorios.model.Reminder;
import com.shtvsolution.recordatorios.utils.CalendarManager;
import com.shtvsolution.recordatorios.utils.ParcelableUtils;
import com.shtvsolution.recordatorios.view.ReminderActivity;

import java.lang.ref.WeakReference;
import java.util.Calendar;

/**
 * Receives alarm broadcast
 * @author Carolina on 05/09/2017
 */
public class ReminderReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = ReminderReceiver.class.getCanonicalName();

    //A reference to the reminder activity so its context can be retrieved before starting a new reminder:
    private static WeakReference<ReminderActivity> reminderActivityReference;
    public static void setReminderActivityReference(ReminderActivity activity) {
        reminderActivityReference = new WeakReference<>(activity);
    }

    /** Broadcast param: reminder object */
    public static final String REMINDER = "reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        byte[] array = intent.getByteArrayExtra(REMINDER);
        Log.d(TAG,"onReceive");

        Reminder reminder = ParcelableUtils.unmarshall(array, Reminder.CREATOR);

        if (checkTimeParameters(context, reminder)) launchReminder(context,reminder);
    }

    /**
     * Starts ReminderActivity
     * @param context App context
     * @param reminder Info of reminder
     */
    public static void launchReminder(Context context, Reminder reminder) {

        //When a reminder is already being displayed the incoming reminder will be postpone for 11 seconds (since the auto-hide time for most reminders is set to 10 seconds),
        //unless the incoming reminder corresponds to an incoming call, case in which the instantlyShown field will be true and the reminder activity will be finished:
        if (reminder.getAdditionalOptions().getBoolean("instantlyShown") && ReminderActivity.isCreated()) {
            if (reminderActivityReference != null && reminderActivityReference.get() != null) {
                reminderActivityReference.get().noneButtonPressed();
                //The thread is stopped for a second so the previous reminder can be hidden before showing the incoming call reminder:
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Log.e(TAG, "launchReminder. InterruptedException: " + ie.getMessage());
                }
            }
            else ReminderActivity.notCreated();
        }

        if(ReminderActivity.isCreated()) CalendarManager.postponeAlarmClock(context, reminder);

        else{
            //Although the activity has not yet been created, the attribute isCreated is modified here since it serves as control so that the other reminders that are received
            //are postponed until the current reminder has been shown and its alarm reset if needed. If this operation is not performed here, some reminders could be lost:
            ReminderActivity.created();

            //When the reminder can be displayed, the aux parameters used to postpone it are reset:
            reminder.resetAuxParameters();

            //Now the reminder with the reset aux params is stored in the byte array that will be sent to the reminder activity:
            byte[] array = ParcelableUtils.marshall(reminder);

            Intent intent = new Intent();
            intent.setAction(context.getResources().getString(R.string.launch_reminder));
            intent.putExtra(ReminderReceiver.REMINDER, array);
            intent.setClassName(context.getPackageName(), ReminderActivity.class.getCanonicalName());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
        }
    }

    /**
     * Method that checks the time parameters of the alarm and acts in consequence:
     */
    private static boolean checkTimeParameters(Context context, Reminder reminder) {
        Boolean showReminder = true;

        //If the alarm's start date is previous to the current date, the reminder won't be shown until the next repetition (in case there is an interval time for the event):
        long currentAlarmTime = reminder.getReminderMillis(false);

        //If the received reminder has a repetition interval, the alarm must be reset according to that interval.
        //This is done that way because since API 19 the repeating alarms are always inexact, so no repeating alarms are set:
        Bundle additionalOptions = reminder.getAdditionalOptions();

        //If a reminder with repetition must be instantly shown, the repetition is not set until the alarm for that reminder is received,
        //moment in which the additional option instantlyShown will have been deleted from the Bundle within the reminder:
        boolean instantlyShown = additionalOptions.getBoolean("instantlyShown", false);

        //If the received alarm corresponds to an early alarm of this reminder, the operations will be performed based on that early alarm's date:
        long prevAlarmMillis = additionalOptions.getLong("prevAlarmMillis", -1);

        //If the alarm's start date and the alarm's aux date are previous to the current date, and it is the first time that the reminder
        //is received, the reminder won't be shown until the next repetition (in case there is an interval time for the event):
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);

        //When a reminder must be instantly shown, the first time that the reminder is received will be when the set alarm is received (instantlyShown == false):
        if (reminder.isFirstReceived() && !instantlyShown) {

            //If the received alarm corresponds to a early alarm for the reminder, the firstReceived attribute is
            //not updated and the currentAlarmTime is set to the one that corresponds to the early alarm time:
            if (prevAlarmMillis == -1) reminder.isNotFirstReceived();

            else currentAlarmTime = prevAlarmMillis;

            //If the current alarm time is smaller than the actual current time, te reminder is not shown:
            if (currentAlarmTime < calendar.getTimeInMillis()) showReminder = false;
        }

        int intervalTime = additionalOptions.getInt("intervalTime", 0);

        //If the reminder has a repetition interval and it is not instantly shown, the reminder parameters will be reset accordingly, and so will be the alarm.
        //Regardless of the interval time and the instantlyShown parameter, if the received alarm is an early alarm, at least one more alarm must be set:
        if ((intervalTime != 0 && !instantlyShown) || prevAlarmMillis != -1)
            CalendarManager.createAlarm(context, reminder, showReminder);

        return showReminder;
    }
}
