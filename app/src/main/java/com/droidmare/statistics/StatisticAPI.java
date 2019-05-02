package com.droidmare.statistics;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * Model for sending statistics.
 * All the application which want to send statistics in this project, has to implement <code>generateAdditionalInfo</code> abstract method.
 * Format of statistic JSON object is next:
 * <code>{
 *     {
 *         "type":"statistic type according to StatisticType enum",
 *         "date":UTC time in milliseconds,
 *         "country":"country name",
 *         "device_id":device id,
 *         "user_id":user id,
 *         "current_app_package_name":"package name of app which sends statistic",
 *         "current_app_version":"app version code of current app",
 *         "info":{
 *             additional information according to statistic type definition
 *         }
 *     }
 * }</code>
 * @author Carolina on 25/09/2017.
 */

public abstract class StatisticAPI {

    private static final String TAG=StatisticAPI.class.getCanonicalName();

    /** App context */
    private Context context;

    /** Broadcast action */
    private static final String STATISTICS_ACTION="com.shtvsolution.estadisticas.statistic_action";

    /** Name of extra statistic */
    public static final String STATISTICS_EXTRA_ARRAY="com.shtvsolution.statistics_array";

    /** Package for sending statistics to server */
    private static final String STATISTICS_PACKAGE="com.shtvsolution.estadisticas";

    /** Main class for sending statistics to server */
    private static final String STATISTICS_CLASS=STATISTICS_PACKAGE+".receiver.StatisticReceiver";

    /** Type JSONObject name */
    private static final String TYPE_NAME="type";

    /** Date JSONOObject name */
    private static final String DATE_NAME="date";

    /** Country JSONObject name */
    private static final String COUNTRY_NAME="country";

    /** Device identification JSONObject name */
    private static final String DEVICE_ID_NAME="device_id";

    /** User identification JSONObject name */
    private static final String USER_ID_NAME="user_id";

    /** App package JSONObject name */
    private static final String APP_PACKAGE_NAME="current_app_package_name";

    /** Current app version code JSONObject name */
    private static final String CURRENT_APP_VERSION_NAME="current_app_version";

    /** Additional information JSONObject name */
    private static final String ADDITIONAL_INFO_NAME="info";

    /** Types of statistic */
    public enum StatisticType{
        //STB actions:
        STB_TRACK,
        //Specific app actions:
        APP_TRACK,
        USER_TRACK,
        COGNITIVE,
        SHTVHOME,
        MEASURE_XX,
        MEASURE_BP,
        MEASURE_BG,
        MEASURE_HR,
        MOOD,
        //Reminders statistics:
        REMINDER_MEDICATION,
        REMINDER_DOCTOR,
        REMINDER_MEASURE_XX,
        REMINDER_MEASURE_BP,
        REMINDER_MEASURE_BG,
        REMINDER_MEASURE_HR,
        REMINDER_STIMULUS,
        REMINDER_PERSONAL,
        REMINDER_ACTIVITY,
        REMINDER_MOOD,
        REMINDER_TEXT_FEEDBACK,
        REMINDER_TEXT_NO_FEEDBACK,
        REMINDER_CALL,
        //Video conference statistics:
        VIDEOCONFERENCE_INCOMING_CALL_DROPED,
        VIDEOCONFERENCE_INCOMING_CALL_MISSED,
        VIDEOCONFERENCE_OUTCOMING_CALL_OK,
        VIDEOCONFERENCE_OUTCOMING_CALL_DROPED,
        VIDEOCONFERENCE_OUTCOMING_CALL_MISSED,
        VIDEOCONFERENCE_INCOMING_CALL_OK
    }

    /**
     * Constructor with params
     * @param context App context
     */
    public StatisticAPI(Context context){
        this.context=context;
    }

    /**
     * Generates additional information associated with the application that sends this statistic
     * @param o Arguments with the information for generation JSONObject
     * @return JSON Object with statistic information
     */
    protected abstract JSONObject generateAdditionalInfo(Object... o);

    /**
     * Sends broadcast message with statistic
     * @param type Statistic type
     * @param o Information for generating JSON object with statistic information
     */
    public void sendStatistic(StatisticType type,Object... o){

        //Statistic generation
        JSONObject statistic=generateJSON(type,generateAdditionalInfo(o));

        //Send broadcast message
        Intent intent=new Intent();

        intent.setAction(STATISTICS_ACTION);
        intent.putExtra(STATISTICS_EXTRA_ARRAY,statistic.toString());
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        intent.setComponent(new ComponentName(STATISTICS_PACKAGE,STATISTICS_CLASS));

        context.sendBroadcast(intent);
    }

    /**
     * Generates JSON object with statistic information
     * @param additionalInfo Additional information associated with the application that sends this statistic
     * @return JSON Object with statistic information
     */
    private JSONObject generateJSON(StatisticType type,JSONObject additionalInfo){

        JSONObject statistic=new JSONObject();

        try{
            statistic.put(TYPE_NAME,type);
            statistic.put(DATE_NAME,getCurrentDate());
            statistic.put(COUNTRY_NAME,getCountryName());
            statistic.put(DEVICE_ID_NAME,getSerialNumber());
            statistic.put(USER_ID_NAME,getUserId());
            statistic.put(APP_PACKAGE_NAME,getAppPackageName());
            statistic.put(CURRENT_APP_VERSION_NAME,getAppVersionNumber());
            statistic.put(ADDITIONAL_INFO_NAME,additionalInfo);
        }catch (JSONException jse){
            Log.e(TAG,"Exception at generateJSON: "+jse.getMessage());
        }

        return statistic;
    }

    /**
     * Gets current date and time
     * @return Current date and time in milliseconds
     */
    private long getCurrentDate(){
        return Calendar.getInstance().getTimeInMillis();
    }

    /**
     * Gets the country of the device
     * @return Country name
     */
    private String getCountryName(){
        return Locale.getDefault().getCountry();
    }

    /**
     * Gets serial number of current device
     * @return Device serial number
     */
    private String getSerialNumber(){
        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
                return Build.getSerial();
            else
                return Build.SERIAL;

        }catch (Exception e){
            return Build.DISPLAY;
        }
    }

    /**
     * Gets current user id
     * @return Current user id
     */
    private String getUserId(){
        //This part has yet to be implemented:
        return "-1";
    }

    /**
     * Gets app package name
     * @return App package name
     */
    private String getAppPackageName(){
        return context.getPackageName();
    }

    /**
     * Gets current app version number
     * @return App version number of current application
     */
    private int getAppVersionNumber() {
        try{
            return context.getPackageManager().getPackageInfo(getAppPackageName(),0).versionCode;
        }
        catch(PackageManager.NameNotFoundException nfe){
            return 0;
        }
    }
}
