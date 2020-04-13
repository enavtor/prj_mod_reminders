package com.droidmare.reminders.views;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.droidmare.common.models.ConstantValues;
import com.droidmare.common.models.EventJsonObject;
import com.droidmare.common.utils.DateUtils;
import com.droidmare.common.utils.ToastUtils;
import com.droidmare.reminders.R;
import com.droidmare.reminders.receiver.ReminderReceiver;
import com.droidmare.reminders.services.ReminderReceiverService;

//Shows overlay reminder
//@author enavas on 05/09/2017:

public class ReminderActivity extends AppCompatActivity {

    //Time for hide reminder in milliseconds:
    public static long HIDE_TIME = 10 * 1000;

    //Time for waiting for key event in milliseconds:
    private static final long WAITING_FOR_KEY_EVENT = 1000;

    //Reminder object and info:
    private EventJsonObject eventJson;
    private String eventType;

    //Reminder layout:
    private RelativeLayout layout;

    //True if this activity is created, false otherwise;
    private static boolean isCreated = false;

    //True if countdown timer is finished, false otherwise:
    private static boolean counterFinish;

    //Buttons for the feedback reminders:
    private RelativeLayout affirmative;
    private RelativeLayout negative;

    //Notification sound and count down for the reminders:
    private boolean repeatNotificationSound;
    private CountDownTimer notificationCountDown;
    private AudioManager audioManager;
    private int originalMediaVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getIntent().getBooleanExtra("requestingPermission", false))
            getCalendarPermissions();

        else {
            setContentView(R.layout.activity_reminder);
            repeatNotificationSound = false;
            counterFinish = false;
            startCountdown();

            eventJson = EventJsonObject.createEventJson(getIntent().getStringExtra(ConstantValues.EVENT_JSON_FIELD));
            HIDE_TIME = eventJson.getLong(ConstantValues.EVENT_TIMEOUT_FIELD, 30000);
            boolean playNotificationSound = eventJson.getBoolean("playNotificationSound", true);

            setReminderLayout();

            Animation up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.reminder_up);
            layout.startAnimation(up);

            layout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //When the eventJson time has expired and none button was pressed, the attribute isCreated value will be "true":
                    if (isCreated) noneButtonPressed();
                }
            }, HIDE_TIME);

            showReminderInfo();

            if (playNotificationSound) setNotification();
        }
    }

    //Method that explicitly asks for some permissions that must be granted by the user:
    private void getCalendarPermissions() {

        int readSdPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);
        int writeSdPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR);

        if (readSdPermission == PackageManager.PERMISSION_DENIED || writeSdPermission == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, 0);

        else {
            ReminderReceiverService.PERMISSION_GRANTED = true;
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ReminderReceiverService.PERMISSION_GRANTED = true;
        finish();
    }

    //Depending on the type of the eventJson a different layout will be loaded:
    private void setReminderLayout() {
        RelativeLayout reminderLayout;
        RelativeLayout reminderContainer = findViewById(R.id.reminder_container);
        RelativeLayout reminderSurvey = findViewById(R.id.no_feedback_survey_container);

        eventType = eventJson.getString(ConstantValues.EVENT_TYPE_FIELD, "");

        if (eventType.equals(ConstantValues.TEXTNOFEEDBACK_EVENT_TYPE)) {
            reminderContainer.setVisibility(View.GONE);
            reminderLayout = reminderSurvey;
        }
        else {
            reminderSurvey.setVisibility(View.GONE);
            reminderLayout = reminderContainer;

            //When the eventJson is of type STIMULUS, two action buttons must be displayed:
            if (eventType.equals(ConstantValues.STIMULUS_EVENT_TYPE)) {
                findViewById(R.id.applaunch_options_container).setVisibility(View.VISIBLE);
                setAppLauncherReminderBehaviour();
            }
        }

        this.layout = reminderLayout;
    }

    //Behaviour of the notification sound:
    private void setNotification () {

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //The volume is set to the max value after storing the current value:
        if (audioManager != null) {

            originalMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.ADJUST_SAME);
        }

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
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMediaVolume, AudioManager.ADJUST_SAME);
                    mp.stop();
                }
            };

            notificationCountDown.start();
        }
    }

    //Behaviour of the stimulus reminder options:
    private void setAppLauncherReminderBehaviour() {
        affirmative = findViewById(R.id.applaunch_affirmative);
        negative = findViewById(R.id.applaunch_negative);

        final String extAppPackage = eventJson.getString(ConstantValues.PACKAGE_NAME, "");
        final String extAppClass = eventJson.getString(ConstantValues.ACTIVITY_NAME, "");

        final Intent externalAppIntent = new Intent();
        externalAppIntent.setComponent(new ComponentName(extAppPackage, extAppClass));

        affirmative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (getPackageManager().getLaunchIntentForPackage(extAppPackage) != null)
                    startActivity(externalAppIntent);

                else {
                    String message = getResources().getString(R.string.error_launching_app) + " (" + extAppPackage + ")";
                    ToastUtils.makeCustomToast(getApplicationContext(), message);
                }
            }
        });

        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backButtonPressed();
            }
        });

        affirmative.requestFocus();
    }

    @Override
    //Depending on the type of the reminder, the behaviour will be a little different:
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN)
            return super.dispatchKeyEvent(event);

        else if (counterFinish && event.getAction() == KeyEvent.ACTION_UP) {

            switch(eventType) {

                case ConstantValues.ACTIVITY_EVENT_TYPE:
                case ConstantValues.PERSONAL_EVENT_TYPE:
                case ConstantValues.MEDICATION_EVENT_TYPE:
                case ConstantValues.DOCTOR_EVENT_TYPE:
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed();
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(false);
                    break;

                case ConstantValues.STIMULUS_EVENT_TYPE:
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                        return backButtonPressed();

                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(true);

                    else if (checkIrPressed(event))
                        return true;

                    break;

                case ConstantValues.TEXTNOFEEDBACK_EVENT_TYPE:
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return backButtonPressed();
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        return centerButtonPressed(false);
                    break;
            }
        }
        return false;
    }

    //Checks if a feedback reminder was answered by using the IR keys:
    private boolean checkIrPressed (KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_F4 || event.getKeyCode() == KeyEvent.KEYCODE_PROG_YELLOW)
            return focusAndSimulateCenterPressed(negative);

        else if (event.getKeyCode() == KeyEvent.KEYCODE_F3 || event.getKeyCode() == KeyEvent.KEYCODE_PROG_GREEN)
            return focusAndSimulateCenterPressed(affirmative);

        return false;
    }

    //The behaviour of the application when the controller's back button is pressed:
    private boolean backButtonPressed(){
        focusAndHide();
        return true;
    }

    //The behaviour of the application when the red or green coloured keys are pressed:
    private boolean focusAndSimulateCenterPressed (RelativeLayout button) {
        button.requestFocusFromTouch();
        return centerButtonPressed(true);
    }

    //The behaviour of the application when the d-pad's center button is pressed:
    private boolean centerButtonPressed(boolean feedbackRequired){

        if (!feedbackRequired) focusAndHide();

        else {
            RelativeLayout focused = (RelativeLayout) getCurrentFocus();
            if (focused != null) focused.performClick();
        }

        return true;
    }

    //The behaviour of the application when the eventJson time expires and no button has been pressed:
    public void noneButtonPressed(){ hideReminder(); }

    //Customizes the reminder view according to eventJson type:
    private void showReminderInfo() {
        Typeface typefaceLight = Typeface.createFromAsset(getAssets(), getResources().getString(R.string.font_light));
        Typeface typefaceBold = Typeface.createFromAsset(getAssets(), getResources().getString(R.string.font_bold));

        TextView time = findViewById(R.id.reminder_time);
        time.setTypeface(typefaceBold);
        time.setText(DateUtils.getFormattedDate(DateUtils.transformFromMillis(eventJson.getReminderMillis())));

        TextView type = findViewById(R.id.reminder_type);
        type.setTypeface(typefaceLight);

        TextView noFeedbackSurveyTitle = findViewById(R.id.no_feedback_survey_title);

        //TextView with additional information of the reminder:
        TextView info = findViewById(R.id.reminder_info);
        info.setMovementMethod(new ScrollingMovementMethod());

        String description = eventJson.getString(ConstantValues.EVENT_DESCRIPTION_FIELD, "");
        info.setText(description);

        ImageView icon = findViewById(R.id.reminder_icon);

        TextView action = findViewById(R.id.reminder_action);

        switch (eventType) {
            case ConstantValues.ACTIVITY_EVENT_TYPE:
                type.setText(R.string.activity_alert_title);
                icon.setImageResource(R.drawable.fisical_activity_icon);
                break;
            case ConstantValues.PERSONAL_EVENT_TYPE:
                type.setText(R.string.personal_alert_title);
                icon.setImageResource(R.drawable.personal_event_icon);
                break;
            case ConstantValues.MEDICATION_EVENT_TYPE:
                type.setText(R.string.medication_alert_title);
                icon.setImageResource(R.drawable.medication_icon);
                break;
            case ConstantValues.STIMULUS_EVENT_TYPE:
                type.setText(R.string.stimulus_alert_title);
                icon.setImageResource(R.drawable.stimulus_icon);
                action.setVisibility(View.GONE);
                action = findViewById(R.id.reminder_action_feedback);
                action.setVisibility(View.VISIBLE);
                action.setText(getString(R.string.stimulus_alert_text));
                action.setTypeface(typefaceLight);
                info.setMaxLines(1);
                break;
            case ConstantValues.DOCTOR_EVENT_TYPE:
                type.setText(R.string.doctor_alert_title);
                icon.setImageResource(R.drawable.doctor_icon);
                break;
            case ConstantValues.TEXTNOFEEDBACK_EVENT_TYPE:
                noFeedbackSurveyTitle.setText(description.toUpperCase());
                break;
        }
    }

    //Hides the reminder overlay:
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

    //Shows focus and hides the reminder:
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

    //Finish ReminderActivity:
    private void finishReminder() {
        if (notificationCountDown != null) {
            notificationCountDown.onFinish();
            notificationCountDown.cancel();
        }
        notCreated();
        finish();
    }

    //Sets the attribute isCreated to true:
    public static void created() { ReminderActivity.isCreated = true; }

    //Sets the attribute isCreated to false:
    public static void notCreated() { ReminderActivity.isCreated = false; }

    //Gets if this activity is created:
    public static boolean isCreated() { return ReminderActivity.isCreated; }

    //Starts countdown for accept key events:
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
