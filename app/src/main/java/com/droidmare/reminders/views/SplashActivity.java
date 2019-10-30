package com.droidmare.reminders.views;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.droidmare.reminders.R;
import com.droidmare.reminders.services.ReminderReceiverService;
import com.shtvsolution.common.utils.ServiceUtils;

/**
 * Starts the application.
 * This class is a test for saving alarms on Android clock
 *
 * @author enavas on 05/09/2017
 */

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        this.getCalendarPermissions();

        String receiverService = ReminderReceiverService.class.getCanonicalName();

        Intent reminderIntent = this.getIntent();

        if (reminderIntent != null && receiverService != null) {

            reminderIntent.setComponent(new ComponentName(getPackageName(), receiverService));

            ServiceUtils.startService(getApplicationContext(), reminderIntent);
        }

        finish();
    }

    /**
     * Asks user for calendar permissions
     */
    private void getCalendarPermissions() {
        int readSdPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);
        int writeSdPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR);
        if (readSdPermission == PackageManager.PERMISSION_DENIED || writeSdPermission == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, 0);
    }
}
