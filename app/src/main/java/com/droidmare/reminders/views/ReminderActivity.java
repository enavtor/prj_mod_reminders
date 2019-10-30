package com.droidmare.reminders.views;

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

import com.droidmare.reminders.R;
import com.droidmare.reminders.model.Reminder;
import com.droidmare.reminders.receiver.ReminderReceiver;
import com.droidmare.reminders.utils.CalendarManager;
import com.droidmare.reminders.utils.IntentManager;
import com.droidmare.reminders.utils.ParcelableUtils;
import com.droidmare.reminders.utils.ToastUtils;
import com.shtvsolution.common.utils.ServiceUtils;

import static com.droidmare.reminders.utils.ToastUtils.DEFAULT_TOAST_DURATION;
import static com.droidmare.reminders.utils.ToastUtils.DEFAULT_TOAST_SIZE;

/**
 * Shows overlay reminder
 *
 * @author enavas on 05/09/2017
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
     * True if this activity is created, false otherwise
     */
    private static boolean isCreated = false;

    /**
     * True if countdown timer is finished, false otherwise
     */
    private static boolean counterFinish;
    /**
     * Buttons for the feedback reminders
     */
    private RelativeLayout affirmative;
    private RelativeLayout negative;

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

    //Depending on the type of the reminder a different layout will be loaded:
    private void setReminderLayout() {
        RelativeLayout reminderLayout = findViewById(R.id.reminder_container);

        String typeSubString = reminder.getType().toString().split("_")[0];
        //When the reminder is of type measure, two action buttons must be displayed:
        if (typeSubString.equals("STIMULUS")) {
            findViewById(R.id.applaunch_options_container).setVisibility(View.VISIBLE);
            setAppLauncherReminderBehaviour();
        }

        this.layout = reminderLayout;
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
     * Behaviour of the measure and stimulus reminder options:
     */
    private void setAppLauncherReminderBehaviour() {
        affirmative = findViewById(R.id.applaunch_affirmative);
        negative = findViewById(R.id.applaunch_negative);

        final String extAppPackage = reminder.getAdditionalOptions().getString(PACKAGE_FIELD);
        final String extAppClass = reminder.getAdditionalOptions().getString(ACTIVITY_FIELD);

        final Intent serviceIntent = new Intent();
        serviceIntent.setComponent(new ComponentName("com.shtvsolution.recordatorios", "com.shtvsolution.recordatorios.services.AppLauncherService"));

        affirmative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (extAppPackage != null && (getPackageManager().getLaunchIntentForPackage(extAppPackage) != null || extAppPackage.equals("mobi.stimulus.stimulustv"))) {
                    serviceIntent.putExtra("package", extAppPackage);
                    serviceIntent.putExtra("class", extAppClass);

                    ServiceUtils.startService(getApplicationContext(), serviceIntent);
                }

                else {
                    String message = getResources().getString(R.string.error_launching_app) + " (" + extAppPackage + ")";
                    ToastUtils.makeCustomToast(getApplicationContext(), message, DEFAULT_TOAST_SIZE, DEFAULT_TOAST_DURATION);
                }
            }
        });

        affirmative.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN)
            return super.dispatchKeyEvent(event);

        else if (counterFinish && event.getAction() == KeyEvent.ACTION_UP) {

            switch (reminder.getType()) {

                case ACTIVITY_REMINDER:
                case PERSONAL_REMINDER:
                case MEDICATION_REMINDER:
                case DOCTOR_REMINDER:
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed();
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(false);
                    break;

                case STIMULUS_REMINDER:
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed();

                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(true);

                    else if (checkIrPressed(event))
                        return true;

                    break;

                case TEXTNOFEEDBACK_REMINDER:
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return backButtonPressed();
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(false);
                    break;
            }
        }
        return false;
    }

    /**
     * Checks if a feedback reminder was answered by using the IR keys
     */
    private boolean checkIrPressed (KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_F4 || event.getKeyCode() == KeyEvent.KEYCODE_PROG_YELLOW)
            return focusAndSimulateCenterPressed(negative);


        else if (event.getKeyCode() == KeyEvent.KEYCODE_F3 || event.getKeyCode() == KeyEvent.KEYCODE_PROG_GREEN)
            return focusAndSimulateCenterPressed(affirmative);

        return false;
    }

    /**
     * The behaviour of the application when the controller's back button is pressed:
     */
    private boolean backButtonPressed(){

        focusAndHide();

        return true;
    }

    /**
     * The behaviour of the application when the red or green coloured keys are pressed:
     */
    private boolean focusAndSimulateCenterPressed (RelativeLayout button) {

        button.requestFocusFromTouch();

        return centerButtonPressed(true);
    }

    /**
     * The behaviour of the application when the d-pad's center button is pressed:
     */
    private boolean centerButtonPressed(boolean feedbackRequired){

        if (!feedbackRequired) focusAndHide();

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
    public void noneButtonPressed(){ hideReminder(); }

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

        TextView noFeedbackSurveyTitle = findViewById(R.id.no_feedback_survey_title);

        /**
         * TextView with additional information of the reminder
         */
        TextView info = findViewById(R.id.reminder_info);
        info.setMovementMethod(new ScrollingMovementMethod());
        info.setText(reminder.getAdditionalInfo());

        ImageView icon = findViewById(R.id.reminder_icon);

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
            case TEXTNOFEEDBACK_REMINDER:
                noFeedbackSurveyTitle.setText(reminder.getAdditionalInfo().toUpperCase());
                break;
        }
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
