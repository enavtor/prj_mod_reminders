package com.droidmare.reminders.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import com.droidmare.reminders.R;
import com.droidmare.reminders.receiver.ReminderReceiver;
import com.droidmare.reminders.model.Reminder;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Manages events on calendar for showing overlay reminders
 * @author Carolina on 05/09/2017
 */

public class CalendarManager {

    private static final String TAG=CalendarManager.class.getCanonicalName();

    public static final int MINUTE = 0;
    public static final int HOUR = 1;
    public static final int DAY = 2;
    public static final int MONTH = 3;
    public static final int YEAR = 4;

    private static final int MIN_YEAR = 2000, MAX_YEAR = 2050;

    /**
     * Creates an entry on calendar from Reminder object without repeating
     * @param context App context
     * @param reminder Reminder object
     */
    public static void createAlarm(Context context,Reminder reminder,boolean reminderWillBeDisplayed){
        //A repeating and a non repeating alarm are established in the same way, the only thing that changes are the log messages displayed in each case.
        //This is due to the fact that a new alarm will be set, according to the specified interval, when the reminder is shown in ReminderActivity and no
        //repeating alarms will be created (owing to the fact repeating alarms are always inexact since API 19 (Android Kitkat 4.4.4)):
        Bundle additionalOptions = reminder.getAdditionalOptions();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);

        String prevAlarm = null;

        String alarmDate = null;

        if (additionalOptions != null) {

            int intervalTime = 0;

            long nextRepetition = -1;
            long repetitionStop = additionalOptions.getLong("repetitionStop");

            String previousAlarms = additionalOptions.getString("previousAlarms");

            JSONArray prevAlarmsArray;

            boolean thereIsPrevAlarm = previousAlarms != null && !previousAlarms.equals("");

            if (thereIsPrevAlarm) {

                try {
                    prevAlarmsArray = new JSONArray(previousAlarms);

                    prevAlarm = prevAlarmsArray.getJSONObject(0).getString("Alarm");

                    alarmDate = prevAlarm;

                    prevAlarmsArray.remove(0);

                    if (prevAlarmsArray.length() > 0) reminder.updatePrevAlarms(prevAlarmsArray.toString());
                    else reminder.updatePrevAlarms(null);

                } catch (JSONException jse) {
                    Log.e(TAG, "createAlarm. JSONException: " + jse.getMessage());
                }
            }

            else {
                alarmDate = reminder.getDate();
                intervalTime = additionalOptions.getInt("intervalTime", 0);

                if (intervalTime != 0) {
                    reminder.calculateNextRepetition(reminderWillBeDisplayed);
                    nextRepetition = additionalOptions.getLong("nextRepetition");
                    alarmDate = CalendarManager.getDateFromMillis(nextRepetition);
                }
            }

            //Depending on the existence of an interval, the log displayed message will be different:
            if (intervalTime == 0) {
                Log.d(TAG, "createAlarm: " + reminder.getNumber() + " ,, " + reminder.getType() + " ,, " + alarmDate + " ,, " + reminder.getAdditionalInfo());
                addAlarmClock(context, reminder, prevAlarm);
            }
            else if (nextRepetition >= calendar.getTimeInMillis() && (nextRepetition < repetitionStop || repetitionStop == -1)) {
                Log.d(TAG, "createAlarmRepeating: " + reminder.getNumber() + " ,, " + reminder.getType() + " ,, " + alarmDate + " ,, " + reminder.getAdditionalInfo());
                Log.d(TAG, "createAlarmRepeating. interval=" + intervalTime + " hours, " + intervalTime * 60 + " minutes");
                addAlarmClock(context, reminder, null);
            }
        }
     }

    /**
     * Delets an entry on calendar from Reminder object without repeating
     * @param context App context
     * @param reminder Reminder object
     */
    public static void deleteAlarm(Context context,Reminder reminder){
        Log.d(TAG,"deleteAlarm: "+reminder.getNumber()+" ,, "+reminder.getType()+" ,, "+reminder.getDate()+" ,, "+reminder.getAdditionalInfo());
        deleteAlarmClock(context,reminder);
    }

    /**
     * Gets current time
     * @return Current time
     */
    public static String getCurrentTime(){
        return getTimeFromMillis(Calendar.getInstance().getTimeInMillis());
    }

    /**
     * Adds a new alarm clock
     * @param context App context
     * @param reminder Reminder object
     */
    private static void addAlarmClock(Context context, Reminder reminder, String prevAlarm){
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        if (prevAlarm != null) {

            int[] prevAlarmValues = getDateValues(prevAlarm);

            calendar.set(Calendar.MILLISECOND, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MINUTE, prevAlarmValues[MINUTE]);
            calendar.set(Calendar.HOUR_OF_DAY, prevAlarmValues[HOUR]);
            calendar.set(Calendar.DAY_OF_MONTH, prevAlarmValues[DAY]);
            calendar.set(Calendar.MONTH, prevAlarmValues[MONTH]);
            calendar.set(Calendar.YEAR, prevAlarmValues[YEAR]);
        }

        else calendar.setTimeInMillis(reminder.getReminderMillis(true));

        long alarmMillis = calendar.getTimeInMillis();

        if (prevAlarm != null) reminder.addPrevAlarmMillis(alarmMillis);
        else reminder.addPrevAlarmMillis(-1);

        AlarmManager alarmManager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent=new Intent(context,ReminderReceiver.class);
        intent.setAction(context.getResources().getString(R.string.launch_reminder));
        byte[] array=ParcelableUtils.marshall(reminder);
        intent.putExtra(ReminderReceiver.REMINDER,array);

        //The flag UPDATE_CURRENT is added so if a pending intent with the same request code exists, the reminder information is updated:
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,reminder.getNumber(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            AlarmManager.AlarmClockInfo clock=new AlarmManager.AlarmClockInfo(alarmMillis, pendingIntent);
            alarmManager.setAlarmClock(clock,pendingIntent);
        }
        else
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmMillis, pendingIntent);
    }

    /**
     * Postpones an alarm for 11 seconds if there is already a reminder being displayed:
     * @param context App context
     * @param reminder Reminder object
     */
    public static void postponeAlarmClock(Context context,Reminder reminder){
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        //Now the alarm is postponed:
        reminder.postponeReminder();

        calendar.set(Calendar.SECOND,reminder.auxSecond);
        calendar.set(Calendar.MINUTE,reminder.auxMinute);
        calendar.set(Calendar.HOUR_OF_DAY,reminder.auxHour);
        calendar.set(Calendar.DAY_OF_MONTH,reminder.auxDay);
        calendar.set(Calendar.MONTH,reminder.auxMonth);
        calendar.set(Calendar.YEAR,reminder.auxYear);

        Log.d("Reminder " + reminder.getNumber() + " postponed", "Next attempt: " + reminder.auxDay + "/" + (reminder.auxMonth + 1) + "/" + reminder.auxYear +
                                                                            " - " + reminder.auxHour + ":" + reminder.auxMinute + ":" + calendar.get(Calendar.SECOND));

        AlarmManager alarmManager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent=new Intent(context,ReminderReceiver.class);
        intent.setAction(context.getResources().getString(R.string.launch_reminder));
        byte[] array=ParcelableUtils.marshall(reminder);
        intent.putExtra(ReminderReceiver.REMINDER,array);

        //In each pending intent the flag UPDATE_CURRENT is added so if a pending intent with the same request code exists, the reminder information is updated:
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,reminder.getNumber(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            AlarmManager.AlarmClockInfo clock=new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent);
            alarmManager.setAlarmClock(clock,pendingIntent);
        }
        else
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    /**
     * Deletes an existing alarm clock
     * @param context App context
     * @param reminder Reminder object
     */
    private static void deleteAlarmClock(Context context,Reminder reminder){

        AlarmManager alarmManager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent=new Intent(context,ReminderReceiver.class);
        intent.setAction(context.getResources().getString(R.string.launch_reminder));
        byte[] array=ParcelableUtils.marshall(reminder);
        intent.putExtra(ReminderReceiver.REMINDER,array);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,reminder.getNumber(),intent,0);
        alarmManager.cancel(pendingIntent);
    }

    /**
     * Gets hour and minutes from milliseconds
     * @param millis Milliseconds
     * @return Hours and minutes in format <code>HH:mm</code> format
     */
    public static String getTimeFromMillis(long millis){
        return new SimpleDateFormat("HH:mm", new Locale("es_ES")).format(millis);
    }

    /**
     * Gets date from milliseconds
     * @param millis Milliseconds
     * @return Date in format <code>dd/MM/yyyy HH:mm</code> format
     */
    public static String getDateFromMillis(long millis){
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("es_ES")).format(millis);
    }

    private static int[] getDateValues(String fullDate) {

        int HOUR, MINUTE, DAY, MONTH, YEAR;
        HOUR = DAY = 0;
        MINUTE = MONTH = 1;
        YEAR = 2;

        //The date and time are delimited by a space, so the regex \\s is used to split them:
        String[] dateAndTime = fullDate.split("\\s");

        //Now each component of the date and the time strings are split as well:
        String[] date = dateAndTime[0].split("/");
        String[] time = dateAndTime[1].split(":");

        return new int[] {
            Integer.valueOf(time[MINUTE]),
            Integer.valueOf(time[HOUR]),
            Integer.valueOf(date[DAY]),
            Integer.valueOf(date[MONTH]) - 1,
            Integer.valueOf(date[YEAR])
        };
    }

    public static long getNextDailyRepetition(long currentDate, long eventStartDate, long eventStopDate, int intervalTime) {

        long repetition;

        int[] eventDateArray = transformFromMillis(eventStartDate);
        int[] currentDateArray = transformFromMillis(currentDate);

        //When the event start date is posterior to the current date, the next event's repetition will be its start date:
        if (isPosterior(eventStartDate, currentDate)) {
            repetition = eventStartDate;
        }

        //When the event start date is previous to the actual current one:
        else if (isPrevious(eventStartDate, currentDate))
            repetition = getNextRepetitionByInterval(eventDateArray, currentDateArray, intervalTime);

        //When the current day is equal to the actual current one:
        else {
            repetition = eventStartDate;

            if (currentDateArray[HOUR] > eventDateArray[HOUR] || currentDateArray[HOUR] == eventDateArray[HOUR] && currentDateArray[MINUTE] > eventDateArray[MINUTE]) {
                repetition = getMillisFromArray(eventDateArray);

                int[] repetitionArray;

                //After that, the interval is going to be added to the repetition date until the repetition is bigger than the current day or the event stop date:
                while (repetition < currentDate && (repetition < eventStopDate || eventStopDate == -1)) {
                    repetitionArray = getIncrementedDate(repetition, intervalTime);
                    repetition = getMillisFromArray(repetitionArray);
                }
            }
        }

        return repetition;
    }

    private static long getNextRepetitionByInterval(int[] eventStartDate, int[] currentDate, int intervalTime){

        Calendar calendar = Calendar.getInstance();

        int monthNumberOfDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int currentMinute = currentDate[MINUTE];
        int currentHour = currentDate[HOUR];
        int currentDay = currentDate[DAY];
        int currentMonth = currentDate[MONTH];
        int currentYear = currentDate[YEAR];

        int eventMinute = eventStartDate[MINUTE];
        int eventHour = eventStartDate[HOUR];
        int eventDay = eventStartDate[DAY];
        int eventMonth = eventStartDate[MONTH];
        int eventYear = eventStartDate[YEAR];

        //The first thing that must be checked is the number of hours between the original reminder time and the current time so that the reminder hour can be
        //appropriately reset by working out the number of hours left until the next repetition if the alarm had been repeated since its original time and date:
        long reminderTime = getMillisFromArray(eventStartDate);
        long currentTime = calendar.getTimeInMillis();

        //the method getNumberOfHours() returns the number of hours between the start date and the 00:00 of the current date, so in
        //order to obtain the actual number ofHours the current hour must be added to the result returned by the aforementioned method:
        long numberOfHours = getNumberOfHours(reminderTime, currentTime) + currentHour;
        long numberOfSkippedRep = numberOfHours / intervalTime;

        long hoursUntilNextRep;

        if (numberOfSkippedRep == 0) hoursUntilNextRep = intervalTime - numberOfHours;
        else hoursUntilNextRep = (intervalTime - (numberOfHours - numberOfSkippedRep * intervalTime)) % intervalTime;

        if (hoursUntilNextRep == 0 && currentMinute > eventMinute) hoursUntilNextRep = intervalTime;

        Log.d("Difference", numberOfHours + " hours");
        Log.d("Skipped reps", numberOfSkippedRep + " skips");
        Log.d("Next repetition in", hoursUntilNextRep + " hours");

        //Now it is necessary to modify the reminder attributes' values with the current time and date ones (if they are different from each other).
        //Given that the reminder could have been received out of its time and date (if the device was off for a long time, for instance):

        if (eventHour != currentHour) eventHour = currentHour;
        if (eventDay != currentDay) eventDay = currentDay;
        if (eventMonth != currentMonth) eventMonth = currentMonth;
        if (eventYear != currentYear) eventYear = currentYear;

        //Now the time and date can be set according to the interval:
        intervalTime = (int)hoursUntilNextRep;

        if (eventHour + intervalTime > 23) {

            eventHour = (eventHour + intervalTime) % 24;

            if (eventDay == monthNumberOfDays) {

                eventDay = 1;

                if (eventMonth == 11) {

                    eventMonth = 0;
                    eventYear++;
                }
                else eventMonth++;
            }
            else eventDay++;
        }
        else eventHour += intervalTime;

        return transformToMillis(eventMinute, eventHour, eventDay, eventMonth, eventYear);
    }

    public static long getNextAlternateRepetition(long currentDate, long eventStartDate, long eventStopDate, ArrayList<Integer> repetitionConfig, int intervalTime) {

        long repetition;

        int[] eventDateArray = transformFromMillis(eventStartDate);
        int[] currentDateArray = transformFromMillis(currentDate);

        int alternateInterval = repetitionConfig.get(0);

        //When the event start date is posterior to the current date, the next event's repetition will be its start date:
        if (isPosterior(eventStartDate, currentDate)) {
            repetition = eventStartDate;
        }

        //When the event start date is previous to the actual current one:
        else if (isPrevious(eventStartDate, currentDate)) {
            int numberOfDays = getNumberOfDays(eventStartDate, currentDate);

            int daysTillNextRep = (alternateInterval - (numberOfDays % alternateInterval)) % alternateInterval;

            if (daysTillNextRep == 0) {
                repetition = getNextDailyRepetition(currentDate, getIncreasedByDayDate(eventStartDate, numberOfDays), eventStopDate, intervalTime);
                if (isPosterior(repetition, currentDate)) repetition = getIncreasedByDayDate(eventStartDate, numberOfDays + alternateInterval);
            }
            else repetition = getIncreasedByDayDate(eventStartDate, numberOfDays + daysTillNextRep);
        }

        //When the current day is equal to the actual current one:
        else {
            repetition = eventStartDate;

            if (currentDateArray[HOUR] > eventDateArray[HOUR] || currentDateArray[HOUR] == eventDateArray[HOUR] && currentDateArray[MINUTE] > eventDateArray[MINUTE]) {

                int[] repetitionArray;

                //After that, the interval is going to be added to the repetition date until the repetition is bigger than the current day or the event stop date:
                while (repetition < currentDate && (repetition < eventStopDate || eventStopDate == -1)) {
                    repetitionArray = getIncrementedDate(repetition, intervalTime);
                    repetition = getMillisFromArray(repetitionArray);
                    if (isPosterior(repetition, currentDate)) repetition = getIncreasedByDayDate(eventStartDate, alternateInterval);
                }
            }
        }

        return repetition;
    }

    public static long getNextWeeklyRepetition(long currentDate, long eventStartDate, long eventStopDate, ArrayList<Integer> repetitionConfig, int intervalTime) {

        long repetition;

        int[] eventDateArray = transformFromMillis(eventStartDate);
        int[] currentDateArray = transformFromMillis(currentDate);

        Integer currentDayOfWeek = getDayOfWeek(currentDateArray[DAY], currentDateArray[MONTH], currentDateArray[YEAR]);

        //When the event start date is posterior to the current date, the next event's repetition will be its start date:
        if (isPosterior(eventStartDate, currentDate)) {
            repetition = eventStartDate;
        }

        //When the event start date is previous to the actual current one:
        else if (isPrevious(eventStartDate, currentDate)) {

            repetition = currentDate;

            //First of all, the next day of week that is contained in the config array must be found, as well as the date that corresponds to that day:
            while (!repetitionConfig.contains(currentDayOfWeek)) {
                currentDayOfWeek = (currentDayOfWeek + 1) % 7;
                if (currentDayOfWeek == 0) currentDayOfWeek = 7;
                repetition = getIncreasedByDayDate(repetition, 1);
            }

            long nextFirstRepetition = getIncreasedByDayDate(eventStartDate, getNumberOfDays(eventStartDate, repetition));

            if (repetition == currentDate)
                repetition = getNextWeeklyRepetition(currentDate, nextFirstRepetition, eventStopDate, repetitionConfig, intervalTime);

            else repetition = nextFirstRepetition;
        }

        //When the current day is equal to the actual current one:
        else {
            repetition = eventStartDate;

            if (currentDateArray[HOUR] > eventDateArray[HOUR] || currentDateArray[HOUR] == eventDateArray[HOUR] && currentDateArray[MINUTE] > eventDateArray[MINUTE]) {

                int[] repetitionArray;

                //After that, the interval is going to be added to the repetition date until the repetition is bigger than the current day or the event stop date:
                while (repetition < currentDate && (repetition < eventStopDate || eventStopDate == -1)) {
                    repetitionArray = getIncrementedDate(repetition, intervalTime);
                    repetition = getMillisFromArray(repetitionArray);
                }

                if (isPosterior(repetition, currentDate)) {

                    repetition = eventStartDate;

                    do {
                        currentDayOfWeek = (currentDayOfWeek + 1) % 7;
                        if (currentDayOfWeek == 0) currentDayOfWeek = 7;
                        repetition = getIncreasedByDayDate(repetition, 1);
                    } while (!repetitionConfig.contains(currentDayOfWeek));
                }
            }
        }

        return repetition;
    }

    public static long getNextMonthlyRepetition(long currentDate, long eventStartDate, long eventStopDate, ArrayList<Integer> repetitionConfig, int intervalTime) {

        long repetition;

        int[] eventDateArray = transformFromMillis(eventStartDate);
        int[] currentDateArray = transformFromMillis(currentDate);

        int currentYear = currentDateArray[YEAR];
        int currentMonth = currentDateArray[MONTH];
        Integer currentDayOfMonth = currentDateArray[DAY];

        //When the event start date is posterior to the current date, the next event's repetition will be its start date:
        if (isPosterior(eventStartDate, currentDate)) {
            repetition = eventStartDate;
        }

        //When the event start date is previous to the actual current one:
        else if (isPrevious(eventStartDate, currentDate)) {

            repetition = currentDate;

            //First of all, the next day of week that is contained in the config array must be found, as well as the date that corresponds to that day:
            while (!repetitionConfig.contains(currentDayOfMonth)) {

                currentDayOfMonth = currentDayOfMonth + 1;
                if (currentDayOfMonth > numberOfDays(currentMonth, currentYear)) {
                    currentDayOfMonth = 1;
                    int [] nextMonthAndYear = moveToNextMonth(currentMonth, currentYear);
                    currentMonth = nextMonthAndYear[0];
                    currentYear = nextMonthAndYear[1];
                }
                repetition = getIncreasedByDayDate(repetition, 1);

                Log.d("Repetition", currentDayOfMonth + "/" + currentMonth + "/" + currentYear);

                int[] auxDateArray = transformFromMillis(repetition);
                int minute = auxDateArray[CalendarManager.MINUTE];
                int hour = auxDateArray[CalendarManager.HOUR];
                int day = auxDateArray[CalendarManager.DAY];
                int month = auxDateArray[CalendarManager.MONTH];
                int year = auxDateArray[CalendarManager.YEAR];

                String message = day + "/" + (month + 1) + "/" + year + " - " + hour + ":" + minute;

                Log.d("Repetition", message);
            }

            Log.d("Repetition", "numberOfDays: " + getNumberOfDays(eventStartDate, repetition));

            long nextFirstRepetition = getIncreasedByDayDate(eventStartDate, getNumberOfDays(eventStartDate, repetition));

            if (repetition == currentDate)
                repetition = getNextMonthlyRepetition(currentDate, nextFirstRepetition, eventStopDate, repetitionConfig, intervalTime);

            else repetition = nextFirstRepetition;
        }

        //When the current day is equal to the actual current one:
        else {
            repetition = eventStartDate;

            if (currentDateArray[HOUR] > eventDateArray[HOUR] || currentDateArray[HOUR] == eventDateArray[HOUR] && currentDateArray[MINUTE] > eventDateArray[MINUTE]) {

                int[] repetitionArray;

                //After that, the interval is going to be added to the repetition date until the repetition is bigger than the current day or the event stop date:
                while (repetition < currentDate && (repetition < eventStopDate || eventStopDate == -1)) {
                    repetitionArray = getIncrementedDate(repetition, intervalTime);
                    repetition = getMillisFromArray(repetitionArray);
                }

                if (isPosterior(repetition, currentDate)) {

                    repetition = eventStartDate;

                    do {
                        currentDayOfMonth = currentDayOfMonth + 1;
                        if (currentDayOfMonth > numberOfDays(currentMonth, currentYear)) {
                            currentDayOfMonth = 1;
                            int [] nextMonthAndYear = moveToNextMonth(currentMonth, currentYear);
                            currentMonth = nextMonthAndYear[0];
                            currentYear = nextMonthAndYear[1];
                        }
                        repetition = getIncreasedByDayDate(repetition, 1);
                    } while (!repetitionConfig.contains(currentDayOfMonth));
                }
            }
        }

        return repetition;
    }

    public static long getNextAnnualRepetition(long currentDate, long eventStartDate, long eventStopDate, int intervalTime) {

        long repetition;

        int[] eventDateArray = transformFromMillis(eventStartDate);
        int[] currentDateArray = transformFromMillis(currentDate);

        //It is convenient to store ins simple variables the following values since they will be used more than once:
        int eventStartDay = eventDateArray[DAY];
        int eventStartMonth = eventDateArray[MONTH];
        int eventStartYear = eventDateArray[YEAR];

        int currentDay = currentDateArray[DAY];
        int currentMonth = currentDateArray[MONTH];
        int currentYear = currentDateArray[YEAR];

        //It is also important to know if the start day is the 29th and the start month is February:
        boolean startOnFeb29 = eventStartDay == 29 && eventStartMonth == 1;

        //When the event start date is posterior to the current date, the next event's repetition will be its start date:
        if (isPosterior(eventStartDate, currentDate)) {
            repetition = eventStartDate;
        }

        //When the event start date is previous or equal to the actual current one:
        else {

            //If the current month is bigger than the event's one, the next repetition will take place a year after the current one:
            if (currentMonth > eventStartMonth) {

                int nextYear = currentYear + 1;

                eventDateArray[YEAR] = nextYear;

                //When an event starts on the 29th of February, the start day must be checked for non leap years:
                if (startOnFeb29 && !leapYear(nextYear)) eventDateArray[DAY] = 28;

                repetition = getMillisFromArray(eventDateArray);
            }

            //If the current month is smaller than the event's one, the next repetition's year will be the current one:
            else if (currentMonth < eventStartMonth) {

                eventDateArray[YEAR] = currentYear;

                //When an event starts on the 29th of February, the start day must be checked for non leap years:
                if (startOnFeb29 && !leapYear(currentYear)) eventDateArray[DAY] = 28;

                repetition = getMillisFromArray(eventDateArray);
            }

            //If the current month is equals to te event start one, the next repetition must be calculated based on the year and the day:
            else {

                //If the current year and the event start year are different, it must be checked if the current year is leap or not and act consequently:
                if (eventStartYear < currentYear && startOnFeb29 && !leapYear(currentYear)) eventStartDay = eventDateArray[DAY] = 28;

                //If the event start day is bigger than the current day, the next repetition will be the start date in the current year:
                if (eventStartDay > currentDay) {
                    eventDateArray[YEAR] = currentYear;
                    repetition = getMillisFromArray(eventDateArray);
                }

                //If the event start day is smaller than the current one, the next repetition will take place in the next year:
                else if (eventStartDay < currentDay) {
                    int nextYear = currentYear + 1;

                    eventDateArray[YEAR] = nextYear;

                    //When an event starts on the 29th of February, the start day must be checked for non leap years:
                    if (startOnFeb29 && !leapYear(nextYear)) eventDateArray[DAY] = 28;

                    repetition = getMillisFromArray(eventDateArray);
                }

                //If the event start day is equals to the current one, the repetition could take place in different dates and times depending on the minutes and hours:
                else {

                    //First of all the repetition is set to the event start date but in the current year:
                    eventDateArray[YEAR] = currentYear;

                    repetition = getMillisFromArray(eventDateArray);

                    //Now the start time and the current time must be compared in order to determine if the repetition will take place in the current date or in the ext year:
                    if (currentDateArray[HOUR] > eventDateArray[HOUR] || currentDateArray[HOUR] == eventDateArray[HOUR] && currentDateArray[MINUTE] > eventDateArray[MINUTE]) {

                        int[] repetitionArray;

                        //After that, the interval is going to be added to the repetition date until the repetition is bigger than the current day or the event stop date:
                        while (repetition < currentDate && (repetition < eventStopDate || eventStopDate == -1)) {
                            repetitionArray = getIncrementedDate(repetition, intervalTime);
                            repetition = getMillisFromArray(repetitionArray);
                        }

                        //if repetition is posterior to the current date a recursive call to this method is make because that situation has been already contemplated:
                        if (isPosterior(repetition, currentDate))
                            repetition = getNextAnnualRepetition(repetition, eventStartDate, eventStopDate, intervalTime);
                    }
                }
            }
        }

        return repetition;
    }

    //Method that returns the date that corresponds to incrementing the parameter date by numberOfHours:
    public static int[] getIncrementedDate (long date, int numberOfHours) {
        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTimeInMillis(date);

        int dateMinute = dateCalendar.get(Calendar.MINUTE);
        int dateHour = dateCalendar.get(Calendar.HOUR_OF_DAY);
        int dateDay = dateCalendar.get(Calendar.DAY_OF_MONTH);
        int dateMonth = dateCalendar.get(Calendar.MONTH);
        int dateYear = dateCalendar.get(Calendar.YEAR);

        if (numberOfHours >= 0) {
            if (dateHour + numberOfHours > 23) {

                dateHour = (dateHour + numberOfHours) % 24;

                if (dateDay == numberOfDays(dateMonth, dateYear)) {

                    dateDay = 1;

                    if (dateMonth == 11) {

                        dateMonth = 0;
                        dateYear++;
                    }
                    else dateMonth++;
                }
                else dateDay++;
            }
            else dateHour += numberOfHours;
        }

        return new int[] {dateMinute, dateHour, dateDay, dateMonth, dateYear};
    }

    //Method that decreases a date based on the specified number of days:
    private static long getIncreasedByDayDate(long dateInMillis, int numberOfDays) {

        int[] dateArray = transformFromMillis(dateInMillis);

        int dateMinute = dateArray[MINUTE];
        int dateHour = dateArray[HOUR];
        int dateDay = dateArray[DAY];
        int dateMonth = dateArray[MONTH];
        int dateYear = dateArray[YEAR];

        int monthNumberOfDays = numberOfDays(dateMonth, dateYear);

        for (; numberOfDays > 0; numberOfDays--) {

            if (dateDay < monthNumberOfDays) dateDay++;
            else {
                if (dateMonth < 11) dateMonth++;
                else {
                    dateMonth = 0;
                    dateYear++;
                }
                dateDay = 1;
                monthNumberOfDays = numberOfDays(dateMonth, dateYear);
            }
        }

        return transformToMillis(dateMinute, dateHour, dateDay, dateMonth, dateYear);
    }

    //Method that returns whether or not the parameter dates are the same day:
    public static boolean isSameDay(long currentDate, long startDate) {
        return !isPrevious(currentDate, startDate) && !isPosterior(currentDate, startDate);
    }

    //Method that returns whether or not the currentDate parameter is previous to the startDate parameter:
    private static boolean isPrevious(long currentDate, long startDate) {
        int[] currentDateArray = transformFromMillis(currentDate);
        int[] startDateArray = transformFromMillis(startDate);

        return (currentDateArray[YEAR] < startDateArray[YEAR]
                || currentDateArray[YEAR] == startDateArray[YEAR] && currentDateArray[ MONTH] < startDateArray[MONTH]
                || currentDateArray[YEAR] == startDateArray[YEAR] && currentDateArray[MONTH] == startDateArray[MONTH]
                && currentDateArray[DAY] < startDateArray[DAY]);
    }

    //Method that returns whether or not the currentDate parameter is posterior to the startDate parameter:
    private static boolean isPosterior(long currentDate, long startDate) {
        int[] currentDateArray = transformFromMillis(currentDate);
        int[] startDateArray = transformFromMillis(startDate);

        return (currentDateArray[YEAR] > startDateArray[YEAR]
                || currentDateArray[YEAR] == startDateArray[YEAR] && currentDateArray[MONTH] > startDateArray[MONTH]
                || currentDateArray[YEAR] == startDateArray[YEAR] && currentDateArray[MONTH] ==  startDateArray[MONTH]
                && currentDateArray[DAY] > startDateArray[DAY]);
    }

    //This method transforms a date array into millis:
    public static long getMillisFromArray(int[] dateArray) {
        return transformToMillis(dateArray[MINUTE], dateArray[HOUR], dateArray[DAY], dateArray[MONTH], dateArray[YEAR]);
    }

    //This method transforms the given parameters (that corresponds to a date) into milliseconds:
    private static long transformToMillis(int minute, int hour, int day, int month, int year) {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);

        return calendar.getTimeInMillis();
    }

    //This method transforms the given parameter (that corresponds to a date in millis) into minute, hour, day, month and year:
    public static int[] transformFromMillis(long timeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);

        return new int[] {
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.YEAR)
        };
    }

    //This method returns the number of days of the given month:
    private static int numberOfDays (int month, int year) {

        if (month == 1 && leapYear(year)) return 29;

        if (month == 1 && !leapYear(year)) return 28;

        if (month <= 6 && month % 2 == 0 || month > 6 && month % 2 != 0 )
            return 31;

        return 30;
    }

    //This method returns true if the given year is Leap Year:
    private static boolean leapYear(int year) {

        if (year % 4 == 0 && year % 100 != 0)
            return true;

        if (year % 400 == 0)
            return true;

        return false;
    }

    //Method that returns the number of hours between the parameters startDate and currentDate (without DST differences):
    private static int getNumberOfHours (long startDate, long currentDate) {

        int numberOfHours = 0;

        //An array that contains the number of days for each month corresponding to the array's index (0 = january, 1 = february, etc):
        int[] monthsDays = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        Calendar startDateCalendar = Calendar.getInstance();
        startDateCalendar.setTimeInMillis(startDate);

        int startHour = startDateCalendar.get(Calendar.HOUR_OF_DAY);
        int startDay = startDateCalendar.get(Calendar.DAY_OF_MONTH);
        int startMonth = startDateCalendar.get(Calendar.MONTH);
        int startYear = startDateCalendar.get(Calendar.YEAR);

        Calendar currentDateCalendar = Calendar.getInstance();
        currentDateCalendar.setTimeInMillis(currentDate);

        int currentDay = currentDateCalendar.get(Calendar.DAY_OF_MONTH);
        int currentMonth = currentDateCalendar.get(Calendar.MONTH);
        int currentYear = currentDateCalendar.get(Calendar.YEAR);

        for (int year = startYear; year <= currentYear; year++) {

            //February's number of days must be calculated based on the current year:
            monthsDays[1] = numberOfDays(1, year);

            int month = 0;

            if (year == startYear) month = startMonth;

            for (; month < 12; month++) {

                if (startMonth == currentMonth && startYear == currentYear) {
                    if (startDay == currentDay) {
                        return numberOfHours;
                    }
                    else {
                        numberOfHours += 24 - startHour;
                        numberOfHours += 24 * (currentDay - startDay - 1);
                        return numberOfHours;
                    }
                }

                else if (month == startMonth && year == startYear) {
                    numberOfHours += 24 - startHour;
                    numberOfHours += 24 * (monthsDays[month] - startDay);
                }

                else if (month == currentMonth && year == currentYear) {
                    numberOfHours += 24 * (currentDay - 1);
                    return numberOfHours;
                }

                else numberOfHours += 24 * monthsDays[month];
            }
        }

        return (numberOfHours);
    }

    //Method that returns the number of days between the parameters startDate and currentDate (without DST differences):
    private static int getNumberOfDays (long startDate, long currentDate) {
        int numberOfHours = getNumberOfHours(startDate, currentDate);
        int numberOfDays = numberOfHours / 24;
        if (numberOfHours % 24 != 0) numberOfDays++;

        return numberOfDays;
    }

    //This method returns the day of week that corresponds to a specific day inside a month:
    private static int getDayOfWeek(int day, int month, int year) {
        int startingDayOfWeek = findDayOfWeek(month, year);

        //An array containing the first seven week days for the month is constructed (for example, if a month starts in friday, the array contents will be: {5,6,7,1,2,3,4}):
        int[] firstSevenDaysOfWeek = new int[7];

        for (int i = 0; i < 7; i++) {
            firstSevenDaysOfWeek[i] = (startingDayOfWeek + i) % 7;
            if (firstSevenDaysOfWeek[i] == 0) firstSevenDaysOfWeek[i] = 7;
        }

        //Now the calculation to get the week day for the current month day can be performed:
        int correspondingFirstSevenDays = day % 7;
        if (correspondingFirstSevenDays == 0) correspondingFirstSevenDays = 7;

        return firstSevenDaysOfWeek[correspondingFirstSevenDays - 1];
    }

    //This method returns the starting week day for the given month inside the given year:
    private static int findDayOfWeek(int month, int year) {
        double a = Math.floor((14 - (month + 1)) / 12);
        double y = year - a;
        double m = (month + 1) + 12 * a - 2;
        double d = (1 + y + Math.floor(y / 4) - Math.floor(y / 100) + Math.floor(y / 400) + Math.floor((31 * m) / 12)) % 7;

        if (d == 0) return 7;
        else return (int) d;
    }

    //This method will move on the given parameters to the next month:
    private static int[] moveToNextMonth (Integer month, Integer year) {
        if (month != 11) month++;
        else {
            month = 0;

            year = moveToNextYear(year);
        }

        return new int[] {month, year};
    }

    //This method will move on the given parameter to the next year:
    private static int moveToNextYear (Integer year) {
        if (year != MAX_YEAR) year++;

        else year = MIN_YEAR;

        return year;
    }
}
