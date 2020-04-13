package com.droidmare.reminders.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.droidmare.common.models.ConstantValues;
import com.droidmare.common.models.EventJsonObject;
import com.droidmare.reminders.R;
import com.droidmare.reminders.utils.CalendarManager;
import com.droidmare.reminders.views.ReminderActivity;

import java.util.Calendar;

//Receives alarm broadcast
//@author enavas on 05/09/2017

public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = ReminderReceiver.class.getCanonicalName();

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG,"onReceive");

        EventJsonObject eventJson = EventJsonObject.createEventJson(intent.getStringExtra(ConstantValues.EVENT_JSON_FIELD));

        //In order to show the correct time on the Reminder, the event to show must be a copy if the eventJson object,
        //since that object's next repetition is going to be modified during the execution of checkTimeParameters:
        EventJsonObject eventToShow = EventJsonObject.createEventJson(eventJson.toString());

        if (checkTimeParameters(context, eventJson)) launchReminder(context, eventToShow);
    }

    ///Method that postpones the reminder or starts the ReminderActivity:
    public static void launchReminder(Context context, EventJsonObject eventJson) {

        if(ReminderActivity.isCreated()) CalendarManager.postponeAlarmClock(context, eventJson);

        else{
            //Although the activity has not yet been created, the attribute isCreated is modified here since it serves as control so that the other reminders that are received
            //are postponed until the current reminder has been shown and its alarm reset if needed. If this operation is not performed here, some reminders could be lost:
            ReminderActivity.created();

            Intent intent = new Intent(context, ReminderActivity.class);
            intent.setAction(context.getResources().getString(R.string.launch_reminder));
            intent.putExtra(ConstantValues.EVENT_JSON_FIELD, eventJson.toString());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
        }
    }

    //Method that checks the time parameters of the alarm and acts in consequence:
    private static boolean checkTimeParameters(Context context, EventJsonObject eventJson) {
        boolean showReminder = true;

        //If the alarm's start date is previous to the current date, the reminder won't be shown until the next repetition (in case there is an interval time for the event):
        long currentAlarmTime = eventJson.getLong(ConstantValues.EVENT_START_DATE_FIELD, -1);

        //If the received alarm corresponds to an early alarm of this reminder, the operations will be performed based on that early alarm's date:
        long prevAlarmMillis = eventJson.getLong(ConstantValues.PREV_ALARM_MILLIS, -1);

        //If the alarm's start date and the alarm's aux date are previous to the current date, and it is the first time that the reminder
        //is received, the reminder won't be shown until the next repetition (in case there is an interval time for the event):
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);

        //When a reminder must be instantly shown, the first time that the reminder is received will be when the set alarm is received (instantlyShown == false):
        if (eventJson.getBoolean(ConstantValues.IS_FIRST_RECEIVED, true)) {

            //If the received alarm corresponds to a early alarm for the reminder, the firstReceived attribute is
            //not updated and the currentAlarmTime is set to the one that corresponds to the early alarm time:
            if (prevAlarmMillis == -1) eventJson.put(ConstantValues.IS_FIRST_RECEIVED, false);

            else currentAlarmTime = prevAlarmMillis;

            //If the current alarm time is smaller than the actual current time, te reminder is not shown:
            if (currentAlarmTime < calendar.getTimeInMillis()) showReminder = false;
        }

        int intervalTime = eventJson.getInt(ConstantValues.EVENT_REP_INTERVAL_FIELD, 0);

        //If the reminder has a repetition interval and it is not instantly shown, the reminder parameters will be reset accordingly, and so will be the alarm.
        //Regardless of the interval time and the instantlyShown parameter, if the received alarm is an early alarm, at least one more alarm must be set:
        if (intervalTime != 0 || prevAlarmMillis != -1)
            CalendarManager.createAlarm(context, eventJson, showReminder);

        return showReminder;
    }
}
