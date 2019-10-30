package com.droidmare.reminders.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import com.droidmare.reminders.utils.CalendarManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model of reminders
 * @author enavas on 06/09/2017
 */

public class Reminder implements Parcelable{

    private static final String TAG = Reminder.class.getCanonicalName();

    /** Types of reminder */
    public enum ReminderType{
        MEDICATION_REMINDER,
        ACTIVITY_REMINDER,
        PERSONAL_REMINDER,
        MEASURE_REMINDER_XX,
        MEASURE_REMINDER_BP,
        MEASURE_REMINDER_BG,
        MEASURE_REMINDER_HR,
        DOCTOR_REMINDER,
        TEXTFEEDBACK_REMINDER,
        TEXTNOFEEDBACK_REMINDER,
        CALL_REMINDER,
        MOOD_REMINDER,
        STIMULUS_REMINDER
    }

    //Integer value for each type of repetition (daily, weekly, on alternate days, etc):
    private final int DAILY_REPETITION = 0;
    private final int ALTERNATE_REPETITION = 1;
    private final int WEEKLY_REPETITION = 2;
    private final int MONTHLY_REPETITION = 3;
    private final int ANNUAL_REPETITION = 4;
    
    /** Type of reminder */
    private ReminderType type;

    /** Number of reminder. Required for adding different alarms in AlarmManager */
    private int number;

    /** Day of reminder */
    private int day;

    /** Month of reminder */
    private int month;

    /** Year of reminder */
    private int year;

    /** Hour of reminder */
    private int hour;

    /** Minute of reminder */
    private int minute;

    /** Additional information of reminder */
    private String additionalInfo;

    /** Additional options for the reminder */
    private Bundle additionalOptions;

    /** Control variable to know if it is the first time the reminder was received */
    private int firstReceived;

    /** Set of aux values used when postponing an alarm */
    public int auxSecond;
    public int auxMinute;
    public int auxHour;
    public int auxDay;
    public int auxMonth;
    public int auxYear;

    public Reminder(int number, ReminderType type, int hour, int minute, String additionalInfo){
        this.number=number;
        this.type=type;
        this.hour = hour;
        this.minute=minute;
        this.additionalInfo=additionalInfo;
        this.day=Calendar.DAY_OF_MONTH;
        this.month=Calendar.MONTH;
        this.year=Calendar.YEAR;
        this.additionalOptions = null;
    }

    public Reminder(int number, ReminderType type, int day, int month, int year, int hour, int minute, String additionalInfo) {
        this.number=number;
        this.type=type;
        this.day=day;
        this.month=month;
        this.year=year;
        this.hour=hour;
        this.minute=minute;
        this.additionalInfo=additionalInfo;
        this.additionalOptions = null;
    }

    public Reminder(int number, ReminderType type, int day, int month, int year, int hour, int minute, String additionalInfo, Bundle options) {
        this.number=number;
        this.type=type;
        this.day=day;
        this.month=month;
        this.year=year;
        this.hour=hour;
        this.minute=minute;
        this.additionalInfo=additionalInfo;
        this.additionalOptions = options;
    }

    private Reminder(Parcel parcel){
        this.number=parcel.readInt();
        this.type=(ReminderType)parcel.readSerializable();
        this.day=parcel.readInt();
        this.month=parcel.readInt();
        this.year=parcel.readInt();
        this.hour=parcel.readInt();
        this.minute=parcel.readInt();
        this.additionalInfo=parcel.readString();
        this.additionalOptions=parcel.readBundle();

        //Retrieving the control variable
        this.firstReceived=parcel.readInt();

        //Retrieving the aux values:
        this.auxSecond=parcel.readInt();
        this.auxMinute=parcel.readInt();
        this.auxHour=parcel.readInt();
        this.auxDay=parcel.readInt();
        this.auxMonth=parcel.readInt();
        this.auxYear=parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.number);
        dest.writeSerializable(this.type);
        dest.writeInt(this.day);
        dest.writeInt(this.month);
        dest.writeInt(this.year);
        dest.writeInt(this.hour);
        dest.writeInt(this.minute);
        dest.writeString(this.additionalInfo);
        dest.writeBundle(this.additionalOptions);

        //Writing the control variable
        dest.writeInt(this.firstReceived);

        //Writing the aux values:
        dest.writeInt(this.auxSecond);
        dest.writeInt(this.auxMinute);
        dest.writeInt(this.auxHour);
        dest.writeInt(this.auxDay);
        dest.writeInt(this.auxMonth);
        dest.writeInt(this.auxYear);
    }

    public static final Parcelable.Creator<Reminder> CREATOR=new Parcelable.Creator<Reminder>(){
        public Reminder createFromParcel(Parcel parcel){
            return new Reminder(parcel);
        }
        public Reminder[] newArray(int size){
            return new Reminder[size];
        }
    };

    public String getDate() {
        Calendar calendar=Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH,this.day);
        calendar.set(Calendar.MONTH,this.month);
        calendar.set(Calendar.YEAR,this.year);
        calendar.set(Calendar.HOUR_OF_DAY,this.hour);
        calendar.set(Calendar.MINUTE,this.minute);
        return CalendarManager.getDateFromMillis(calendar.getTimeInMillis());
    }

    public String getTime(){
        Calendar calendar=Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,this.hour);
        calendar.set(Calendar.MINUTE,this.minute);
        return CalendarManager.getTimeFromMillis(calendar.getTimeInMillis());
    }

    public int getNumber() {
        return number;
    }

    public ReminderType getType() {
        return type;
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public Bundle getAdditionalOptions() { return additionalOptions; }

    //Since the default value for the function parcel.readInt() is zero, the reminder is first received when the variable firstReceived value is zero:
    public boolean isFirstReceived() { return firstReceived == 0; }

    public void isNotFirstReceived() { this.firstReceived = -1; }

    /**
     * This method will delete the additional option instantlyShown from the Bundle
     */
    public void deleteInstantlyShown() {
        this.additionalOptions.remove("instantlyShown");
    }

    /**
     * This method will update the additional option previousAlarms from the Bundle
     */
    public void updatePrevAlarms(String prevAlarms) {
        if (prevAlarms != null) this.additionalOptions.putString("previousAlarms", prevAlarms);
        else this.additionalOptions.remove("previousAlarms");
    }

    /**
     * This method will add the additional option prevAlarmMillis to the Bundle
     */
    public void addPrevAlarmMillis(long prevAlarmMillis) {
        if (prevAlarmMillis != -1) this.additionalOptions.putLong("prevAlarmMillis", prevAlarmMillis);
        else this.additionalOptions.remove("prevAlarmMillis");
    }

    /**
     * This method will return the time of the reminder in milliseconds:
     */
    public long getReminderMillis(boolean settingNewAlarm) {

        Calendar calendar = Calendar.getInstance();

        int minute = this.minute;
        int hour = this.hour;
        int day = this.day;
        int month = this.month;
        int year = this.year;

        long currentRepetition = additionalOptions.getLong("currentRepetition", -1);

        if (settingNewAlarm) currentRepetition = additionalOptions.getLong("nextRepetition", -1);

        if (currentRepetition != -1) {

            int[] auxDateArray = CalendarManager.transformFromMillis(currentRepetition);

            minute = auxDateArray[CalendarManager.MINUTE];
            hour = auxDateArray[CalendarManager.HOUR];
            day = auxDateArray[CalendarManager.DAY];
            month = auxDateArray[CalendarManager.MONTH];
            year = auxDateArray[CalendarManager.YEAR];
        }

        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.YEAR, year);

        return calendar.getTimeInMillis();
    }

    //Function that works out the next repetition for an event (only if the event has a repetition):
    public void calculateNextRepetition (boolean reminderWillBeDisplayed) {

        int intervalTime = additionalOptions.getInt("intervalTime");
        String repetitionType = additionalOptions.getString("repetitionType");
        long repetitionStop = additionalOptions.getLong("repetitionStop");

        long currentRepetition = additionalOptions.getLong("nextRepetition", -1);

        Log.d("Current", currentRepetition+"");

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //A minute will be added so the reminder is not reset six times during the current minute if the current date is the current repetition's one:
        if (reminderWillBeDisplayed) {
            int minute = calendar.get(Calendar.MINUTE);
            if (minute == 59) {
                calendar.set(Calendar.MINUTE, 0);
                int[] incrementedDateArray = CalendarManager.getIncrementedDate(calendar.getTimeInMillis(), 1);
                calendar.setTimeInMillis(CalendarManager.getMillisFromArray(incrementedDateArray));
            }
            else calendar.set(Calendar.MINUTE, ++minute);

        }

        long currentDate = calendar.getTimeInMillis();

        long nextRepetition = -1;

        if (intervalTime != 0) {

            int repType = -1;
            ArrayList<Integer> repConfig = new ArrayList<>();

            int[] auxDateArray = {minute, hour, day, month, year};
            long eventDateMillis = CalendarManager.getMillisFromArray(auxDateArray);

            JSONObject repTypeAux = new JSONObject();

            //First thing that must be done is to retrieve the repetition type and config:
            try {
                repTypeAux = new JSONObject(repetitionType);
                repType = repTypeAux.getInt("type");
                repConfig = getRepetitionConfigArray(repTypeAux.getString("config"));
            } catch (JSONException jse) {
                Log.e(TAG, "calculateNextRepetition. JSONException: " + jse.getMessage());
            }

            //Log.e("CalculateNextRepetition", "Reminder: " + number + " -- " + repTypeAux.toString());

            //Now the calculation is performed based on the repetition type and the interval:
            switch (repType) {
                case DAILY_REPETITION:
                    nextRepetition = CalendarManager.getNextDailyRepetition(currentDate, eventDateMillis, repetitionStop, intervalTime);
                    break;
                case ALTERNATE_REPETITION:
                    nextRepetition = CalendarManager.getNextAlternateRepetition(currentDate, eventDateMillis, repetitionStop, repConfig, intervalTime);
                    break;
                case WEEKLY_REPETITION:
                    nextRepetition = CalendarManager.getNextWeeklyRepetition(currentDate, eventDateMillis, repetitionStop, repConfig, intervalTime);
                    break;
                case MONTHLY_REPETITION:
                    nextRepetition = CalendarManager.getNextMonthlyRepetition(currentDate, eventDateMillis, repetitionStop, repConfig, intervalTime);
                    break;
                case ANNUAL_REPETITION:
                    nextRepetition = CalendarManager.getNextAnnualRepetition(currentDate, eventDateMillis, repetitionStop, intervalTime);
                    break;
            }

            if (nextRepetition != -1) {

                if (currentRepetition == -1) currentRepetition = nextRepetition;

                additionalOptions.putLong("currentRepetition", currentRepetition);

                additionalOptions.putLong("nextRepetition", nextRepetition);

                /*auxDateArray = CalendarManager.transformFromMillis(nextRepetition);

                int minute = auxDateArray[CalendarManager.MINUTE];
                int hour = auxDateArray[CalendarManager.HOUR];
                int day = auxDateArray[CalendarManager.DAY];
                int month = auxDateArray[CalendarManager.MONTH];
                int year = auxDateArray[CalendarManager.YEAR];

                String message = day + "/" + (month + 1) + "/" + year + " - " + hour + ":" + minute;

                Log.e("CalculateNextRepetition", message);*/
            }
        }
    }

    //This method transforms the repetition config string, inside repetitionType, into an array list:
    private ArrayList<Integer> getRepetitionConfigArray(String config) {

        ArrayList<Integer> configList = new ArrayList<>();

        if (config.length() > 2) {
            config = config.replace(" ", "");

            String [] configArray = config.substring(1, config.length() - 1).split(",");

            for (String configElement: configArray) {
                configList.add(Integer.valueOf(configElement));
            }
        }

        return configList;
    }
    
    /**
     * This method will change the parameters of a reminder according to the value of intervalTime
     * inside the additional options bundle, so that a new alarm, that will be triggered at the time
     * given by that interval, can be set (this is for repeating alarms only when the current hour
     * is equal to 23, case in which at least the day will have to be changed as well)
     */
    public void resetReminderHour(){

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        int monthNumberOfDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int intervalTime = additionalOptions.getInt("intervalTime");

        int currentMinute = calendar.get(Calendar.MINUTE);
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        //The first thing that must be checked is the number of hours between the original reminder time and the current time so that the reminder hour can be
        //appropriately reset by working out the number of hours left until the next repetition if the alarm had been repeated since its original time and date:
        long reminderTime = this.getReminderMillis(false);
        long currentTime = calendar.getTimeInMillis();

        long numberOfHours = (currentTime - reminderTime) / (1000 * 60 * 60);
        long numberOfSkippedRep = numberOfHours / intervalTime;

        long hoursUntilNextRep;

        if (numberOfSkippedRep == 0) hoursUntilNextRep = intervalTime - numberOfHours;
        else hoursUntilNextRep = intervalTime - (numberOfHours - numberOfSkippedRep * intervalTime);

        if (hoursUntilNextRep == 0) hoursUntilNextRep = intervalTime;

        Log.d("Difference", numberOfHours + " hours");
        Log.d("Skipped reps", numberOfSkippedRep + " skips");
        Log.d("Next repetition in", hoursUntilNextRep + " hours");

        //Now it is necessary to modify the reminder attributes' values with the current time and date ones (if they are different from each other).
        //Given that the reminder could have been received out of its time and date (if the device was off for a long time, for instance):

        if (this.hour != currentHour) this.hour = currentHour;
        if (this.day != currentDay) this.day = currentDay;
        if (this.month != currentMonth)this.month = currentMonth;
        if (this.year != currentYear) this.year = currentYear;

        //Now the time and date can be reset according to the interval

        //If it is the first time that the alarm is received, the reminder time will be reset according to the hoursUntilNextRep variable:
        if (firstReceived != 0) intervalTime = (int)hoursUntilNextRep;

        //If the current minute is smaller than the alarm minute and is the first time that the alarm is received, the reminder will be set without adding the interval:
        if (!(this.minute > currentMinute) && firstReceived != 0) {

            if (this.hour + intervalTime > 23) {

                this.hour = (this.hour + intervalTime) % 24;

                if (this.day == monthNumberOfDays) {

                    this.day = 1;

                    if (this.month == 11) {

                        this.month = 0;
                        this.year++;
                    }
                    else this.month++;
                }
                else this.day++;
            }
            else this.hour += intervalTime;
        }
    }

    /**
     * This method will change the aux parameters of a reminder in order to postpone its alarm X seconds when the value of the current
     * second is equal or bigger than 60 - postponementTime (case in which at least the minute will have to be changed as well)
     */
    public void postponeReminder() {

        //The number of seconds the alarm will be postponed:
        int postponementTime = 11;

        Calendar calendar = Calendar.getInstance();

        int monthNumberOfDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int currentMinute = calendar.get(Calendar.MINUTE);
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        //The attribute auxSecond is always modified based on the current second:
        this.auxSecond = calendar.get(Calendar.SECOND);

        //First of all it is necessary to modify the reminder auxiliary attributes' values with the current time and date ones (if they are different from each other).
        //On account of the fact that the reminder could have been received out of its time and date (if the date was changed during the postponement, for example):
        if (this.auxMinute != currentMinute) this.auxMinute = currentMinute;
        if (this.auxHour != currentHour) this.auxHour = currentHour;
        if (this.auxDay != currentDay) this.auxDay = currentDay;
        if (this.auxMonth != currentMonth) this.auxMonth = currentMonth;
        if (this.auxYear != currentYear) this.auxYear = currentYear;

        //Now the reminder can be properly postponed:
        if (this.auxSecond >= 60 - postponementTime) {

            this.auxSecond = (this.auxSecond + postponementTime) % 60;

            if (this.auxMinute == 59) {

                this.auxMinute = 0;

                if (this.auxHour == 23) {

                    this.auxHour = 0;

                    if (this.auxDay == monthNumberOfDays) {

                        this.auxDay = 1;

                        if (this.auxMonth == 11) {

                            this.auxMonth = 0;
                            this.auxYear++;
                        }
                        else this.auxMonth++;
                    }
                    else this.auxDay++;
                }
                else this.auxHour++;
            }
            else this.auxMinute++;
        }
        else this.auxSecond+=postponementTime;
    }

    /**
     * This method will reset the aux parameters
     */
    public void resetAuxParameters () {

        this.auxMinute = 0;
        this.auxHour = 0;
        this.auxDay = 0;
        this.auxMonth = 0;
        this.auxYear = 0;
    }
}
