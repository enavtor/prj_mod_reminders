package com.droidmare.reminders.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * An API for communicating with the reminder module in order to send a reminder in form of a byte array
 * that the reminder module will transform into a Reminder object again so it can be added to an alarm.
 *
 * @author Eduardo on 28/02/2018
 *
 */

/*To send reminders to the reminder module you must include this class inside your app following this specifications:

    1- The name of the package containing this class (inside your app) must be: com.shtvsolution.recordatorios.model.
    2- Once inside the package, this class must be renamed (using the refactor option) from ReminderAPI to Reminder.
    3- Add the following attributes to the activity or fragment that is going to create and send the reminder:

        //Package and main activity of the reminder module:
        private static final String REMINDER_PACKAGE = "com.shtvsolution.recordatorios";
        private static final String REMINDER_MAIN = "com.shtvsolution.recordatorios.view.SplashActivity";

    4- Add the following code within the activity or fragment from the previous step:

        //First, the reminder must be created:
        Reminder newReminder = new Reminder(...Your reminder params here);

        ***************************(EXAMPLE CODE. DO NOT COPY) *****************************
        *An example of a remainder for creating an instant alarm without additional info:  *
        *                                                                                  *
        *   String additionalInfo = "";                                                    *
        *                                                                                  *
        *   Calendar calendar = Calendar.getInstance();                                    *
        *   calendar.setTimeInMillis(System.currentTimeMillis());                          *
        *                                                                                  *
        *   Reminder newReminder = new Reminder((int)(Math.random()*100),                  *
        *       Reminder.ReminderType.ACTIVITY_REMINDER,                                   *
        *       DateUtils.currentDay,                                                      *
        *       DateUtils.currentMonth,                                                    *
        *       DateUtils.currentYear,                                                     *
        *       calendar.get(Calendar.HOUR_OF_DAY),                                        *
        *       calendar.get(Calendar.MINUTE),                                             *
        *       additionalInfo);                                                           *
        *                                                                                  *
        *****************************(END OF EXAMPLE CODE)**********************************

         //After that, the reminder must be transformed into a byte array so it can be sent to the reminders module:
        byte[] reminderBytes = newReminder.marshall();

        //Finally, an intent must be created to send the remainder (in form of byte array):
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(REMINDER_PACKAGE, REMINDER_MAIN));
        intent.putExtra("reminderBytes", reminderBytes);
        startActivity(intent);
*/

public class ReminderAPI implements Parcelable{

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

    public ReminderAPI(int number, ReminderType type, int day, int month, int year, int hour, int minute, String additionalInfo) {
        this.number = number;
        this.type = type;
        this.day = day;
        this.month = month;
        this.year = year;
        this.hour = hour;
        this.minute = minute;
        this.additionalInfo = additionalInfo;
        this.additionalOptions = null;
    }

    public ReminderAPI(int number, ReminderType type, int day, int month, int year, int hour, int minute, String additionalInfo, Bundle options) {
        this.number = number;
        this.type = type;
        this.day = day;
        this.month = month;
        this.year = year;
        this.hour = hour;
        this.minute = minute;
        this.additionalInfo = additionalInfo;
        this.additionalOptions = options;
    }

    private ReminderAPI(Parcel parcel){
        this.number = parcel.readInt();
        this.type = (ReminderType)parcel.readSerializable();
        this.day = parcel.readInt();
        this.month = parcel.readInt();
        this.year = parcel.readInt();
        this.hour = parcel.readInt();
        this.minute = parcel.readInt();
        this.additionalInfo = parcel.readString();
        this.additionalOptions = parcel.readBundle();
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
    }

    public static final Creator<ReminderAPI> CREATOR=new Creator<ReminderAPI>(){
        public ReminderAPI createFromParcel(Parcel parcel){
            return new ReminderAPI(parcel);
        }

        public ReminderAPI[] newArray(int size){
            return new ReminderAPI[size];
        }
    };

    /**
     * Converts from Parcelable to byte[]
     * @return Conversion to byte[]
     */
    public byte[] marshall() {
        Parcel parcel = Parcel.obtain();
        this.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }
}
