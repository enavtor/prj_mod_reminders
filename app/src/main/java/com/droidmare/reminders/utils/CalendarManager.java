package com.droidmare.reminders.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.Calendar;

import com.droidmare.common.models.ConstantValues;
import com.droidmare.reminders.R;
import com.droidmare.reminders.receiver.ReminderReceiver;
import com.droidmare.common.models.EventJsonObject;
import com.droidmare.common.utils.DateUtils;

import org.json.JSONArray;
import org.json.JSONException;

//Manages events on calendar for showing overlay reminders
//@author enavas on 05/09/2017
public class CalendarManager {

    private static final String TAG = CalendarManager.class.getCanonicalName();

    //Creates an entry on calendar from Reminder object without repeating:
    public static void createAlarm(Context context, EventJsonObject eventJson, boolean reminderWillBeDisplayed){
        //A repeating and a non repeating alarm are established in the same way, the only thing that changes are the log messages displayed in each case.
        //This is due to the fact that a new alarm will be set, according to the specified interval, when the reminder is shown in ReminderActivity and no
        //repeating alarms will be created (owing to the fact repeating alarms are always inexact since API 19 (Android Kitkat 4.4.4)):
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);

        String prevAlarm = null;

        String alarmDate = null;

        int intervalTime = 0;

        long nextRepetition = -1;

        long repetitionStop = eventJson.getLong(ConstantValues.EVENT_REPETITION_STOP_FIELD, -1);

        JSONArray prevAlarmsArray = eventJson.getPreviousAlarmsArray();

        if (prevAlarmsArray.length() > 0) {

            try {
                prevAlarm = prevAlarmsArray.getJSONObject(0).getString("Alarm");

                alarmDate = prevAlarm;

                prevAlarmsArray.remove(0);

                if (prevAlarmsArray.length() > 0) eventJson.put(ConstantValues.EVENT_PREV_ALARMS_FIELD, prevAlarmsArray.toString());
                else eventJson.put(ConstantValues.EVENT_PREV_ALARMS_FIELD, null);

            } catch (JSONException jse) {
                Log.e(TAG, "createAlarm. JSONException: " + jse.getMessage());
            }
        }

        else {
            alarmDate = DateUtils.getFormattedDate(DateUtils.transformFromMillis(eventJson.getLong(ConstantValues.EVENT_START_DATE_FIELD, -1)));
            intervalTime = eventJson.getInt(ConstantValues.EVENT_REP_INTERVAL_FIELD, 0);

            if (intervalTime != 0) {
                nextRepetition = DateUtils.calculateNextRepetition(eventJson, reminderWillBeDisplayed, repetitionStop, intervalTime);
                alarmDate = DateUtils.getFormattedDate(DateUtils.transformFromMillis(nextRepetition));
            }
        }

        String eventId = eventJson.getString(ConstantValues.EVENT_ID_FIELD, "");
        String eventType = eventJson.getString(ConstantValues.EVENT_TYPE_FIELD, "");
        String eventDescription = eventJson.getString(ConstantValues.EVENT_DESCRIPTION_FIELD, "");

        //Depending on the existence of an interval, the log displayed message will be different:
        if (intervalTime == 0) {
            Log.d(TAG, "createAlarm: " + eventId + " ,, " + eventType + " ,, " + alarmDate + " ,, " + eventDescription);
            addAlarmClock(context, eventJson, prevAlarm);
        }
        else if (nextRepetition >= calendar.getTimeInMillis() && (nextRepetition < repetitionStop || repetitionStop == -1)) {
            Log.d(TAG, "createAlarmRepeating: " + eventId + " ,, " + eventType + " ,, " + alarmDate + " ,, " + eventDescription);
            Log.d(TAG, "createAlarmRepeating. interval=" + intervalTime + " hours, " + intervalTime * 60 + " minutes");
            addAlarmClock(context, eventJson, null);
        }
    }


    //Deletes an entry on calendar from Reminder object without repeating:
    public static void deleteAlarm(Context context, EventJsonObject eventJson) {
        String eventId = eventJson.getString(ConstantValues.EVENT_ID_FIELD, "");
        String eventType = eventJson.getString(ConstantValues.EVENT_TYPE_FIELD, "");
        String eventDescription = eventJson.getString(ConstantValues.EVENT_DESCRIPTION_FIELD, "");
        String eventFormattedDate = DateUtils.getFormattedDate(DateUtils.transformFromMillis(eventJson.getLong(ConstantValues.EVENT_START_DATE_FIELD, -1)));

        Log.d(TAG,"deleteAlarm: " + eventId + " ,, " + eventType + " ,, " + eventFormattedDate + " ,, " + eventDescription);
        deleteAlarmClock(context, eventJson);
    }

    //Gets current time:
    public static String getCurrentTime(){
        return DateUtils.getTodayFormattedDate().split(" ")[1];
    }

    //Adds a new alarm clock:
    private static void addAlarmClock(Context context, EventJsonObject eventJson, String prevAlarm){
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        if (prevAlarm != null) {

            int[] prevAlarmValues = DateUtils.getDateValues(prevAlarm);

            calendar.set(Calendar.MILLISECOND, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MINUTE, prevAlarmValues[DateUtils.MINUTE]);
            calendar.set(Calendar.HOUR_OF_DAY, prevAlarmValues[DateUtils.HOUR]);
            calendar.set(Calendar.DAY_OF_MONTH, prevAlarmValues[DateUtils.DAY]);
            calendar.set(Calendar.MONTH, prevAlarmValues[DateUtils.MONTH]);
            calendar.set(Calendar.YEAR, prevAlarmValues[DateUtils.YEAR]);
        }

        else calendar.setTimeInMillis(eventJson.getReminderMillis(true));

        long alarmMillis = calendar.getTimeInMillis();

        if (prevAlarm != null) eventJson.put(ConstantValues.PREV_ALARM_MILLIS, alarmMillis);
        else eventJson.put(ConstantValues.PREV_ALARM_MILLIS, -1);

        setAlarmIntent(context, eventJson, alarmMillis, false);
    }

    //Postpones an alarm for 11 seconds if there is already a reminder being displayed:
    public static void postponeAlarmClock(Context context, EventJsonObject eventJson){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + 11 * 1000);

        setAlarmIntent(context, eventJson, calendar.getTimeInMillis(), false);
    }

    //Deletes an existing alarm clock:
    private static void deleteAlarmClock(Context context, EventJsonObject eventJson){
        setAlarmIntent(context, eventJson, -1, true);
    }

    //Method that wraps the functionality to create and set a new alarm pending intent:
    private static void setAlarmIntent(Context context, EventJsonObject eventJson, long alarmMillis, boolean cancelingAlarm) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.setAction(context.getResources().getString(R.string.launch_reminder));
            intent.putExtra(ConstantValues.EVENT_JSON_FIELD, eventJson.toString());

            int eventId = Integer.valueOf(eventJson.getString(ConstantValues.EVENT_ID_FIELD, "").replace("localId:", ""));

            if (cancelingAlarm) {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                alarmManager.cancel(pendingIntent);
            }

            else {
                //The flag UPDATE_CURRENT is added so if a pending intent with the same request code exists, the reminder information is updated:
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AlarmManager.AlarmClockInfo clock = new AlarmManager.AlarmClockInfo(alarmMillis, pendingIntent);
                    alarmManager.setAlarmClock(clock, pendingIntent);
                }

                else alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
            }
        }
    }
}
