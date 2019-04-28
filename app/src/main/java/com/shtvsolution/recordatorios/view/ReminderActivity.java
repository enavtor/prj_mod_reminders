package com.shtvsolution.recordatorios.view;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shtvsolution.estadisticas.StatisticAPI;
import com.shtvsolution.estadisticas.StatisticService;
import com.shtvsolution.recordatorios.R;
import com.shtvsolution.recordatorios.model.Reminder;
import com.shtvsolution.recordatorios.receiver.ReminderReceiver;
import com.shtvsolution.recordatorios.utils.CalendarManager;
import com.shtvsolution.recordatorios.utils.IntentManager;
import com.shtvsolution.recordatorios.utils.ParcelableUtils;
import com.shtvsolution.recordatorios.utils.ToastUtils;

import static com.shtvsolution.recordatorios.utils.ToastUtils.DEFAULT_TOAST_DURATION;
import static com.shtvsolution.recordatorios.utils.ToastUtils.DEFAULT_TOAST_SIZE;

/**
 * Shows overlay reminder
 *
 * @author Carolina on 05/09/2017
 */
public class ReminderActivity extends AppCompatActivity {

    /**
     * Time for hide reminder in milliseconds
     */
    public static long HIDE_TIME = 10 * 1000;

    /**
     * Time for waiting for key event in milliseconds
     */
    private static final long WAITING_FOR_KEY_EVENT = 1000;

    /**
     * Reminder info
     */
    private Reminder reminder;

    /**
     * Reminder layout
     */
    private RelativeLayout layout;

    /**
     * External app Strings fields:
     */
    private static final String PACKAGE_FIELD = "package";
    private static final String ACTIVITY_FIELD = "activity";

    /**
     * TextView with additional information of the reminder
     */
    private TextView info;

    /**
     * True if this activity is created, false otherwise
     */
    private static boolean isCreated = false;

    /**
     * True if countdown timer is finished, false otherwise
     */
    private static boolean counterFinish;

    private boolean justCreated;

    private Handler onPauseHandler;

    private Runnable onPauseRunnable;

    /**
     * Statistic object
     */
    private StatisticService statistic;

    /**
     * Buttons for the feedback reminders
     */
    private RelativeLayout affirmative;
    private RelativeLayout negative;

    private RelativeLayout great;
    private RelativeLayout happy;
    private RelativeLayout meh;
    private RelativeLayout sad;
    private RelativeLayout bad;

    /**
     * Notification sound and count down for the reminders:
     */
    private boolean repeatNotificationSound;
    private CountDownTimer notificationCountDown;
    private AudioManager audioManager;
    private int originalMediaVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final String canonicalName =  getClass().getCanonicalName();

        onPauseHandler = new Handler();
        onPauseRunnable = new Runnable() {
            @Override
            public void run() {
                statistic.sendStatistic(StatisticAPI.StatisticType.APP_TRACK, StatisticService.ON_PAUSE, canonicalName);
            }
        };

        justCreated = true;

        statistic = new StatisticService(this);
        statistic.sendStatistic(StatisticAPI.StatisticType.APP_TRACK, StatisticService.ON_CREATE, getClass().getCanonicalName());

        //The reminder receiver and the Intent manager need tho have a reference to the ReminderActivity context so the incoming calls can be properly managed:
        ReminderReceiver.setReminderActivityReference(this);
        IntentManager.setReminderActivityReference(this);

        setContentView(R.layout.activity_reminder);
        repeatNotificationSound = false;
        counterFinish = false;
        startCountdown();

        this.reminder = ParcelableUtils.unmarshall(getIntent().getByteArrayExtra(ReminderReceiver.REMINDER), Reminder.CREATOR);
        HIDE_TIME = reminder.getAdditionalOptions().getLong("timeOut");
        boolean playNotificationSound = reminder.getAdditionalOptions().getBoolean("playNotificationSound", true);

        setReminderLayout();

        Animation up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.reminder_up);
        layout.startAnimation(up);

        layout.postDelayed(new Runnable() {
            @Override
            public void run() {
                //When the reminder time has expired and none button was pressed, the attribute isCreated value will be "true":
                if (isCreated) noneButtonPressed();
            }
        }, HIDE_TIME);

        showReminderInfo();

        if (playNotificationSound) setNotification();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!justCreated) statistic.sendStatistic(StatisticAPI.StatisticType.APP_TRACK, StatisticService.ON_RESUME, getClass().getCanonicalName());

        else justCreated = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        onPauseHandler.postDelayed(onPauseRunnable, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onPauseHandler.removeCallbacks(onPauseRunnable);
        statistic.sendStatistic(StatisticAPI.StatisticType.APP_TRACK, StatisticService.ON_DESTROY, getClass().getCanonicalName());
    }

    //Depending on the type of the reminder a different layout will be loaded:
    private void setReminderLayout() {
        RelativeLayout reminderLayout = findViewById(R.id.reminder_container);
        RelativeLayout moodSurveyLayout = findViewById(R.id.mood_survey_container);
        RelativeLayout feedbackSurveyLayout = findViewById(R.id.feedback_survey_container);
        RelativeLayout noFeedbackSurveyLayout = findViewById(R.id.no_feedback_survey_container);
        RelativeLayout incomingCallLayout = findViewById(R.id.incoming_call_container);

        if (reminder.getType() == Reminder.ReminderType.MOOD_REMINDER) {
            reminderLayout.setVisibility(View.GONE);
            feedbackSurveyLayout.setVisibility(View.GONE);
            noFeedbackSurveyLayout.setVisibility(View.GONE);
            incomingCallLayout.setVisibility(View.GONE);
            this.layout = moodSurveyLayout;
            setMoodSurveyBehaviour();
        }

        else if (reminder.getType() == Reminder.ReminderType.TEXTFEEDBACK_REMINDER) {
            reminderLayout.setVisibility(View.GONE);
            moodSurveyLayout.setVisibility(View.GONE);
            noFeedbackSurveyLayout.setVisibility(View.GONE);
            incomingCallLayout.setVisibility(View.GONE);
            this.layout = feedbackSurveyLayout;
            setFeedbackSurveyBehaviour();
        }

        else if (reminder.getType() == Reminder.ReminderType.TEXTNOFEEDBACK_REMINDER) {
            reminderLayout.setVisibility(View.GONE);
            moodSurveyLayout.setVisibility(View.GONE);
            feedbackSurveyLayout.setVisibility(View.GONE);
            incomingCallLayout.setVisibility(View.GONE);
            this.layout = noFeedbackSurveyLayout;
        }

        else if (reminder.getType() == Reminder.ReminderType.CALL_REMINDER) {
            reminderLayout.setVisibility(View.GONE);
            moodSurveyLayout.setVisibility(View.GONE);
            feedbackSurveyLayout.setVisibility(View.GONE);
            noFeedbackSurveyLayout.setVisibility(View.GONE);
            this.layout = incomingCallLayout;
            setIncomingCallBehaviour();
            //When receiving an incoming call the notification sound will be repeated:
            repeatNotificationSound = true;
        }

        else {
            moodSurveyLayout.setVisibility(View.GONE);
            feedbackSurveyLayout.setVisibility(View.GONE);
            noFeedbackSurveyLayout.setVisibility(View.GONE);
            incomingCallLayout.setVisibility(View.GONE);
            String typeSubString = reminder.getType().toString().split("_")[0];
            //When the reminder is of type measure, two action buttons must be displayed:
            if (typeSubString.equals("MEASURE") || typeSubString.equals("STIMULUS")) {
                findViewById(R.id.applaunch_options_container).setVisibility(View.VISIBLE);
                setApplauncherReminderBehaviour();
            }

            this.layout = reminderLayout;
        }
    }

    /**
     * Behaviour of the notification sound:
     */
    private void setNotification () {

        /*audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        The volume is set to the max value after storing the current value:
        if (audioManager != null) {

            originalMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.ADJUST_SAME);
        }*/

        //Now a media player is instantiated in order to play the notification sound:
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        final MediaPlayer mp = MediaPlayer.create(this, notification);

        //So as to repeat the notification sound when a video call is received, a CountDownTimer is set:
        if (mp != null) {
            notificationCountDown = new CountDownTimer(HIDE_TIME, 1000) {

                public void onTick(long millisUntilFinished) {
                    if (!mp.isPlaying()) mp.start();
                    if (!repeatNotificationSound) notificationCountDown.cancel();
                }

                public void onFinish() {
                    //When the timer ends, the volume value is reset:
                    //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMediaVolume, AudioManager.ADJUST_SAME);
                    mp.stop();
                }
            };

            notificationCountDown.start();
        }
    }

    /**
     * Behaviour of the mood survey options:
     */
    private void setMoodSurveyBehaviour() {
        great = findViewById(R.id.first_option);
        happy = findViewById(R.id.second_option);
        meh = findViewById(R.id.third_option);
        sad = findViewById(R.id.fourth_option);
        bad = findViewById(R.id.fifth_option);

        great.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(StatisticAPI.StatisticType.MOOD, StatisticService.MOOD_SELECTED, 0);
            }
        });

        happy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(StatisticAPI.StatisticType.MOOD, StatisticService.MOOD_SELECTED, 1);
            }
        });

        meh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(StatisticAPI.StatisticType.MOOD, StatisticService.MOOD_SELECTED, 2);
            }
        });

        sad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(StatisticAPI.StatisticType.MOOD, StatisticService.MOOD_SELECTED, 3);
            }
        });

        bad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(StatisticAPI.StatisticType.MOOD, StatisticService.MOOD_SELECTED, 4);
            }
        });

        great.requestFocus();
    }

    /**
     * Behaviour of the generic survey options:
     */
    private void setFeedbackSurveyBehaviour() {
        affirmative = findViewById(R.id.affirmative_option);
        negative = findViewById(R.id.negative_option);

        affirmative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(StatisticAPI.StatisticType.REMINDER_TEXT_FEEDBACK, StatisticService.FEEDBACK_YES, "yes");
            }
        });

        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(StatisticAPI.StatisticType.REMINDER_TEXT_FEEDBACK, StatisticService.GENERIC_NO, "no");
            }
        });

        affirmative.requestFocus();
    }

    /**
     * Behaviour of the incoming call reminder options:
     */
    private void setIncomingCallBehaviour() {
        affirmative = findViewById(R.id.accept_call_option);
        negative = findViewById(R.id.reject_call_option);

        final String extAppPackage = reminder.getAdditionalOptions().getString(PACKAGE_FIELD);
        final String extAppClass = reminder.getAdditionalOptions().getString(ACTIVITY_FIELD);

        final Intent serviceIntent = new Intent();
        serviceIntent.setComponent(new ComponentName("com.shtvsolution.recordatorios", "com.shtvsolution.recordatorios.services.AppLauncherService"));

        affirmative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(StatisticAPI.StatisticType.REMINDER_CALL, StatisticService.CALL_YES, reminder.getAdditionalInfo());

                serviceIntent.putExtra("action", "accepted");
                serviceIntent.putExtra("package", extAppPackage);
                serviceIntent.putExtra("class", extAppClass);

                startService(serviceIntent);

                finishReminder();
            }
        });

        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(StatisticAPI.StatisticType.REMINDER_CALL, StatisticService.CALL_NO, reminder.getAdditionalInfo());

                serviceIntent.putExtra("action", "rejected");
                serviceIntent.putExtra("package", extAppPackage);
                serviceIntent.putExtra("class", extAppClass);

                startService(serviceIntent);

                isCreated = false;

                hideReminder();
            }
        });

        affirmative.requestFocus();
    }

    /**
     * Behaviour of the measure and stimulus reminder options:
     */
    private void setApplauncherReminderBehaviour() {
        affirmative = findViewById(R.id.applaunch_affirmative);
        negative = findViewById(R.id.applaunch_negative);

        StatisticAPI.StatisticType statisticType = null;

        final String extAppPackage = reminder.getAdditionalOptions().getString(PACKAGE_FIELD);
        final String extAppClass = reminder.getAdditionalOptions().getString(ACTIVITY_FIELD);

        final Intent serviceIntent = new Intent();
        serviceIntent.setComponent(new ComponentName("com.shtvsolution.recordatorios", "com.shtvsolution.recordatorios.services.AppLauncherService"));

        switch (reminder.getType()) {
            case MEASURE_REMINDER_XX:
                statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_XX;
                break;
            case MEASURE_REMINDER_BP:
                statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_BP;
                break;
            case MEASURE_REMINDER_BG:
                statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_BG;
                break;
            case MEASURE_REMINDER_HR:
                statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_HR;
                break;
            case STIMULUS_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_STIMULUS;
                break;
        }

        final StatisticAPI.StatisticType statsType = statisticType;

        affirmative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(statsType, StatisticService.APP_LAUNCH_YES, "yes");

                if (extAppPackage != null && (getPackageManager().getLaunchIntentForPackage(extAppPackage) != null || extAppPackage.equals("mobi.stimulus.stimulustv"))) {
                    serviceIntent.putExtra("package", extAppPackage);
                    serviceIntent.putExtra("class", extAppClass);

                    startService(serviceIntent);
                }

                else {
                    String message = getResources().getString(R.string.error_launching_app) + " (" + extAppPackage + ")";
                    ToastUtils.makeCustomToast(getApplicationContext(), message, DEFAULT_TOAST_SIZE, DEFAULT_TOAST_DURATION);
                }
            }
        });

        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                statistic.sendStatistic(statsType, StatisticService.GENERIC_NO, "no");
            }
        });

        affirmative.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN)
            return super.dispatchKeyEvent(event);

        else if (counterFinish && event.getAction() == KeyEvent.ACTION_UP) {
            String feedbackAnswer = " (negative answer)";
            StatisticAPI.StatisticType statisticType;
            switch (reminder.getType()) {

                case ACTIVITY_REMINDER:
                    statisticType = StatisticAPI.StatisticType.REMINDER_ACTIVITY;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(statisticType, false);
                    break;

                case PERSONAL_REMINDER:
                    statisticType = StatisticAPI.StatisticType.REMINDER_PERSONAL;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(statisticType, false);
                    break;

                case MEDICATION_REMINDER:
                    statisticType = StatisticAPI.StatisticType.REMINDER_MEDICATION;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(statisticType, false);
                    break;

                case DOCTOR_REMINDER:
                    statisticType = StatisticAPI.StatisticType.REMINDER_DOCTOR;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(statisticType, false);
                    break;

                case MEASURE_REMINDER_XX:
                    statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_XX;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        if (getCurrentFocus().getId() == affirmative.getId()) feedbackAnswer = " (affirmative answer)";
                        statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.DPAD_USED, this.reminder.getAdditionalInfo() + feedbackAnswer);
                        return centerButtonPressed(statisticType, true);
                    }

                    else if (checkIrPressed(event, statisticType))
                        return true;

                    break;

                case MEASURE_REMINDER_BP:
                    statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_BP;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                        if (getCurrentFocus().getId() == affirmative.getId()) feedbackAnswer = " (affirmative answer)";
                        statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.DPAD_USED, this.reminder.getAdditionalInfo() + feedbackAnswer);
                        return centerButtonPressed(statisticType, true);
                    }

                    else if (checkIrPressed(event, statisticType))
                        return true;

                    break;

                case MEASURE_REMINDER_BG:
                    statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_BG;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                        if (getCurrentFocus().getId() == affirmative.getId()) feedbackAnswer = " (affirmative answer)";
                        statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.DPAD_USED, this.reminder.getAdditionalInfo() + feedbackAnswer);
                        return centerButtonPressed(statisticType, true);
                    }

                    else if (checkIrPressed(event, statisticType))
                        return true;

                    break;

                case MEASURE_REMINDER_HR:
                    statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_HR;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                        if (getCurrentFocus().getId() == affirmative.getId()) feedbackAnswer = " (affirmative answer)";
                        statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.DPAD_USED, this.reminder.getAdditionalInfo() + feedbackAnswer);
                        return centerButtonPressed(statisticType, true);
                    }

                    else if (checkIrPressed(event, statisticType))
                        return true;

                    break;

                case STIMULUS_REMINDER:
                    statisticType = StatisticAPI.StatisticType.REMINDER_STIMULUS;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                        if (getCurrentFocus().getId() == affirmative.getId()) feedbackAnswer = " (affirmative answer)";
                        statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.DPAD_USED, this.reminder.getAdditionalInfo() + feedbackAnswer);
                        return centerButtonPressed(statisticType, true);
                    }

                    else if (checkIrPressed(event, statisticType))
                        return true;

                    break;

                case MOOD_REMINDER:
                    statisticType = StatisticAPI.StatisticType.REMINDER_MOOD;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        View currentFocused = getCurrentFocus();
                        if (currentFocused.getId() == great.getId()) feedbackAnswer = " (0 answered)";
                        else if (currentFocused.getId() == happy.getId()) feedbackAnswer = " (1 answered)";
                        else if (currentFocused.getId() == meh.getId()) feedbackAnswer = " (2 answered)";
                        else if (currentFocused.getId() == sad.getId()) feedbackAnswer = " (3 answered)";
                        else if (currentFocused.getId() == bad.getId()) feedbackAnswer = " (4 answered)";
                        statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.DPAD_USED, this.reminder.getAdditionalInfo() + feedbackAnswer);
                        return centerButtonPressed(statisticType, true);
                    }
                    else if (event.getKeyCode() >= KeyEvent.KEYCODE_1 && event.getKeyCode() <= KeyEvent.KEYCODE_5) {
                        int elementPosition = event.getKeyCode() - 8;
                        RelativeLayout [] moodButtonsList = {great, happy, meh, sad, bad};
                        //Since the elements go from 1 to 5 and the numerical key's codes go from 8 to 12, when the 1 key is pressed,
                        //the element that corresponds to 1, which is the element in the position 0 of the mood list, must be selected:
                        statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.NUM_USED, this.reminder.getAdditionalInfo() + " (" + elementPosition + " answered)");
                        return focusAndSimulateCenterPressed(moodButtonsList[elementPosition] ,statisticType);
                    }
                    break;

                case TEXTFEEDBACK_REMINDER:
                    statisticType = StatisticAPI.StatisticType.REMINDER_TEXT_FEEDBACK;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                        if (getCurrentFocus().getId() == affirmative.getId()) feedbackAnswer = " (affirmative answer)";
                        statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.DPAD_USED, this.reminder.getAdditionalInfo() + feedbackAnswer);
                        return centerButtonPressed(statisticType, true);
                    }

                    else if (checkIrPressed(event, statisticType))
                        return true;

                    break;

                case TEXTNOFEEDBACK_REMINDER:
                    statisticType = StatisticAPI.StatisticType.REMINDER_TEXT_NO_FEEDBACK;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return backButtonPressed(statisticType);
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(statisticType, false);
                    break;

                case CALL_REMINDER:
                    statisticType = StatisticAPI.StatisticType.REMINDER_CALL;
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return true;
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                        if (getCurrentFocus().getId() == affirmative.getId()) feedbackAnswer = " (affirmative answer)";
                        statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.DPAD_USED, this.reminder.getAdditionalInfo() + feedbackAnswer);
                        return centerButtonPressed(statisticType, true);
                    }

                    else if (checkIrPressed(event, statisticType))
                        return true;

                    break;
            }
        }
        return false;
    }

    /**
     * Checks if a feedback reminder was answered by using the IR keys
     */
    private boolean checkIrPressed (KeyEvent event, StatisticAPI.StatisticType statisticType) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_F4 || event.getKeyCode() == KeyEvent.KEYCODE_PROG_YELLOW) {
            statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.IR_USED, this.reminder.getAdditionalInfo() + " (negative answer)");
            return focusAndSimulateCenterPressed(negative, statisticType);
        }

        else if (event.getKeyCode() == KeyEvent.KEYCODE_F3 || event.getKeyCode() == KeyEvent.KEYCODE_PROG_GREEN) {
            statistic.sendStatistic(StatisticAPI.StatisticType.USER_TRACK, StatisticService.IR_USED, this.reminder.getAdditionalInfo() + " (affirmative answer)");
            return focusAndSimulateCenterPressed(affirmative, statisticType);
        }

        return false;
    }

    /**
     * The behaviour of the application when the controller's back button is pressed:
     */
    private boolean backButtonPressed(StatisticAPI.StatisticType type){

        statistic.sendStatistic(type, StatisticService.GENERIC_BACK, this.reminder.getAdditionalInfo());

        focusAndHide();

        return true;
    }

    /**
     * The behaviour of the application when the red or green coloured keys are pressed:
     */
    private boolean focusAndSimulateCenterPressed (RelativeLayout button, StatisticAPI.StatisticType statisticType) {

        button.requestFocusFromTouch();

        return centerButtonPressed(statisticType, true);
    }

    /**
     * The behaviour of the application when the d-pad's center button is pressed:
     */
    private boolean centerButtonPressed(StatisticAPI.StatisticType type, boolean feedbackRequired){

        if (!feedbackRequired) {
            statistic.sendStatistic(type, StatisticService.GENERIC_YES, this.reminder.getAdditionalInfo());
            focusAndHide();
        }

        else {
            RelativeLayout focused = (RelativeLayout) getCurrentFocus();
            if (focused != null) focused.performClick();
            if (!reminder.getType().equals(Reminder.ReminderType.CALL_REMINDER)) hideReminder();
        }

        return true;
    }

    /**
     * The behaviour of the application when the reminder time expires and no button has been pressed:
     */
    public void noneButtonPressed(){
        StatisticAPI.StatisticType statisticType = null;
        switch (reminder.getType()) {
            case ACTIVITY_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_ACTIVITY;
                break;
            case PERSONAL_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_PERSONAL;
                break;
            case MEDICATION_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_MEDICATION;
                break;
            case DOCTOR_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_DOCTOR;
                break;
            case MEASURE_REMINDER_XX:
                statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_XX;
                break;
            case MEASURE_REMINDER_BP:
                statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_BP;
                break;
            case MEASURE_REMINDER_BG:
                statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_BG;
                break;
            case MEASURE_REMINDER_HR:
                statisticType = StatisticAPI.StatisticType.REMINDER_MEASURE_HR;
                break;
            case STIMULUS_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_STIMULUS;
                break;
            case MOOD_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_MOOD;
                break;
            case TEXTFEEDBACK_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_TEXT_FEEDBACK;
                break;
            case TEXTNOFEEDBACK_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_TEXT_NO_FEEDBACK;
                break;
            case CALL_REMINDER:
                statisticType = StatisticAPI.StatisticType.REMINDER_CALL;
                break;
        }

        statistic.sendStatistic(statisticType, StatisticService.GENERIC_NONE, reminder.getAdditionalInfo());

        hideReminder();
    }

    /**
     * Customizes the reminder view according to reminder type
     */
    private void showReminderInfo() {
        Typeface typefaceLight = Typeface.createFromAsset(getAssets(), getResources().getString(R.string.font_light));
        Typeface typefaceBold = Typeface.createFromAsset(getAssets(), getResources().getString(R.string.font_bold));

        TextView time = findViewById(R.id.reminder_time);
        time.setTypeface(typefaceBold);
        time.setText(CalendarManager.getDateFromMillis(reminder.getReminderMillis(false)));

        TextView type = findViewById(R.id.reminder_type);
        type.setTypeface(typefaceLight);

        TextView moodSurveyTitle = findViewById(R.id.mood_survey_title);
        TextView feedbackSurveyTitle = findViewById(R.id.feedback_survey_title);
        TextView noFeedbackSurveyTitle = findViewById(R.id.no_feedback_survey_title);

        TextView incomingCallTitle = findViewById(R.id.incoming_call_title);
        TextView incomingCallInfo = findViewById(R.id.incoming_call_info);
        TextView incomingCallTime = findViewById(R.id.incoming_call_time);

        info = findViewById(R.id.reminder_info);
        info.setMovementMethod(new ScrollingMovementMethod());
        info.setText(reminder.getAdditionalInfo());

        ImageView icon = findViewById(R.id.reminder_icon);

        TextView textAction = findViewById(R.id.reminder_text_action);

        TextView action = findViewById(R.id.reminder_action);

        switch (reminder.getType()) {
            case ACTIVITY_REMINDER:
                type.setText(R.string.activity_alert_title);
                icon.setImageResource(R.drawable.fisical_activity_icon);
                break;
            case PERSONAL_REMINDER:
                type.setText(R.string.personal_alert_title);
                icon.setImageResource(R.drawable.personal_event_icon);
                break;
            case MEDICATION_REMINDER:
                type.setText(R.string.medication_alert_title);
                icon.setImageResource(R.drawable.medication_icon);
                break;
            case MEASURE_REMINDER_XX:
                type.setText(R.string.measure_alert_title);
                icon.setImageResource(R.drawable.measure_xx_icon);
                action.setVisibility(View.GONE);
                action = findViewById(R.id.reminder_action_feedback);
                action.setVisibility(View.VISIBLE);
                action.setText(getString(R.string.measure_alert_text));
                action.setTypeface(typefaceLight);
                info.setMaxLines(1);
                break;
            case MEASURE_REMINDER_BP:
                type.setText(R.string.measure_alert_title);
                icon.setImageResource(R.drawable.measure_bp_icon);
                action.setVisibility(View.GONE);
                action = findViewById(R.id.reminder_action_feedback);
                action.setVisibility(View.VISIBLE);
                action.setText(getString(R.string.measure_alert_text));
                action.setTypeface(typefaceLight);
                info.setMaxLines(1);
                break;
            case MEASURE_REMINDER_BG:
                type.setText(R.string.measure_alert_title);
                icon.setImageResource(R.drawable.measure_bg_icon);
                action.setVisibility(View.GONE);
                action = findViewById(R.id.reminder_action_feedback);
                action.setVisibility(View.VISIBLE);
                action.setText(getString(R.string.measure_alert_text));
                action.setTypeface(typefaceLight);
                info.setMaxLines(1);
                break;
            case MEASURE_REMINDER_HR:
                type.setText(R.string.measure_alert_title);
                icon.setImageResource(R.drawable.measure_hr_icon);
                action.setVisibility(View.GONE);
                action = findViewById(R.id.reminder_action_feedback);
                action.setVisibility(View.VISIBLE);
                action.setText(getString(R.string.measure_alert_text));
                action.setTypeface(typefaceLight);
                info.setMaxLines(1);
                break;
            case STIMULUS_REMINDER:
                type.setText(R.string.stimulus_alert_title);
                icon.setImageResource(R.drawable.stimulus_icon);
                action.setVisibility(View.GONE);
                action = findViewById(R.id.reminder_action_feedback);
                action.setVisibility(View.VISIBLE);
                action.setText(getString(R.string.stimulus_alert_text));
                action.setTypeface(typefaceLight);
                info.setMaxLines(1);
                break;
            case DOCTOR_REMINDER:
                type.setText(R.string.doctor_alert_title);
                icon.setImageResource(R.drawable.doctor_icon);
                break;
            case MOOD_REMINDER:
                moodSurveyTitle.setText(reminder.getAdditionalInfo().toUpperCase());
                break;
            case TEXTFEEDBACK_REMINDER:
                feedbackSurveyTitle.setText(reminder.getAdditionalInfo().toUpperCase());
                break;
            case TEXTNOFEEDBACK_REMINDER:
                noFeedbackSurveyTitle.setText(reminder.getAdditionalInfo().toUpperCase());
                break;
            case CALL_REMINDER:
                incomingCallTime.setTypeface(typefaceBold);
                incomingCallTime.setText(CalendarManager.getCurrentTime());
                incomingCallTitle.setText(R.string.call_alert_title);
                String callInfo = getString(R.string.call_alert_subtitle) + reminder.getAdditionalInfo();
                incomingCallInfo.setText(callInfo);
                break;
        }
    }

    //Method that transforms a dp value into pixels:
    public int transformDipToPix (int dpValue) {
        Resources r = getResources();
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, r.getDisplayMetrics());
    }

    /**
     * Hides reminder
     */
    private void hideReminder() {

        if (layout != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Animation down = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.reminder_down);
                    layout.startAnimation(down);
                    layout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            layout.setVisibility(View.GONE);
                            finishReminder();
                        }
                    }, down.getDuration());
                }
            });
        }
    }

    /**
     * Shows focus and hide reminder
     */
    private void focusAndHide() {
        final RelativeLayout focus = findViewById(R.id.reminder_container_focus);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layout.getLayoutParams();
        focus.setLayoutParams(params);
        Animation focusIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.focus_in);
        focus.startAnimation(focusIn);
        focus.postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation focusOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.focus_out);
                focus.startAnimation(focusOut);
                focus.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideReminder();
                    }
                }, focusOut.getDuration());
            }
        }, focusIn.getDuration());
    }

    /**
     * Finish ReminderActivity
     */
    private void finishReminder() {
        if (notificationCountDown != null) {
            notificationCountDown.onFinish();
            notificationCountDown.cancel();
        }

        notCreated();
        finish();
    }

    /**
     * Sets the attribute isCreated to true
     */
    public static void created() {
        ReminderActivity.isCreated = true;
    }

    /**
     * Sets the attribute isCreated to false
     */
    public static void notCreated() {
        ReminderActivity.isCreated = false;
    }

    /**
     * Gets if this activity is created
     *
     * @return True if it is created, false otherwise
     */
    public static boolean isCreated() {
        return ReminderActivity.isCreated;
    }

    /**
     * Starts countdown for accept key events
     */
    private void startCountdown() {
        new CountDownTimer(WAITING_FOR_KEY_EVENT, WAITING_FOR_KEY_EVENT) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                counterFinish = true;
            }
        }.start();
    }
}
