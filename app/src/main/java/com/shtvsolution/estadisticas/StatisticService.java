package com.shtvsolution.estadisticas;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Sends reminder statistics to statistics receiver
 * @author Carolina on 03/10/2017.
 * @modifyBy Eduardo on 23/05/2018.
 */

public class StatisticService extends StatisticAPI {

    /** Reminder action name */
    private static final String REMINDER_ACTION_NAME="action";

    /** Additional information name */
    private static final String ADDITIONAL_INFO_NAME="additional_info";

    /** Action types over reminder */
    public static final String GENERIC_NONE="missed";
    public static final String GENERIC_BACK="rejected";
    public static final String GENERIC_NO="Reply No";
    public static final String GENERIC_YES="accepted";
    public static final String FEEDBACK_YES="Reply Yes";
    public static final String APP_LAUNCH_YES ="Reply Yes + start app";
    public static final String MOOD_SELECTED="option selected";
    public static final String CALL_NONE="missed + mark missed call";
    public static final String CALL_NO="call rejected";
    public static final String CALL_YES="call accepted";

    /** User action types */
    public static final String NUM_USED ="The user answered a remainder by using the numeric keys";
    public static final String IR_USED ="The user answered a remainder by using the ir keys";
    public static final String DPAD_USED="The user answered a remainder by using the d-pad";

    /** Action types over app */
    public static final String ON_CREATE ="onCreate";
    public static final String ON_START = "onStart";
    public static final String ON_RESUME = "onResume";
    public static final String ON_RESTART = "onRestart";
    public static final String ON_PAUSE = "onPause";
    public static final String ON_STOP = "onStop";
    public static final String ON_DESTROY = "onDestroy";

    /**
     * Constructor with params
     * @param context App context
     */
    public StatisticService(Context context) {
        super(context);
    }

    /**
     * Statistic additional information format:
     * <code>
     *     o[0]: action of reminder
     *     o[1]: additional information of reminder
     * </code>
     */

    @Override
    protected JSONObject generateAdditionalInfo(Object... o) {
        try{
            JSONObject object=new JSONObject();
            object.put(REMINDER_ACTION_NAME,o[0]);
            object.put(ADDITIONAL_INFO_NAME,o[1]);
            return object;
        }
        catch (JSONException jse){return null;}
    }
}
