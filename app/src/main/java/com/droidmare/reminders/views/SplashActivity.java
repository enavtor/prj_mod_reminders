package com.droidmare.reminders.views;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.droidmare.statistics.StatisticAPI;
import com.droidmare.statistics.StatisticService;
import com.droidmare.reminders.R;

/**
 * Starts the application.
 * This class is a test for saving alarms on Android clock
 *
 * @author Carolina on 05/09/2017
 */

public class SplashActivity extends Activity {

    private StatisticService statistic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        statistic = new StatisticService(this);
        statistic.sendStatistic(StatisticAPI.StatisticType.APP_TRACK, StatisticService.ON_CREATE, getClass().getCanonicalName());

        setContentView(R.layout.activity_splash);
        this.getCalendarPermissions();

        Intent reminderIntent = this.getIntent();

        if (reminderIntent != null) {

            reminderIntent.setComponent(new ComponentName("com.shtvsolution.recordatorios", "com.shtvsolution.recordatorios.services.ReminderReceiverService"));

            startService(reminderIntent);
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        statistic.sendStatistic(StatisticAPI.StatisticType.APP_TRACK, StatisticService.ON_DESTROY, getClass().getCanonicalName());
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
