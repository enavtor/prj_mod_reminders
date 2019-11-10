package com.droidmare.reminders.services;

import android.content.ComponentName;
import android.content.Intent;

import com.droidmare.common.models.ConstantValues;
import com.droidmare.common.services.CommonIntentService;

//Service that starts an external app
//@author Eduardo on 26/07/2018.
public class AppLauncherService extends CommonIntentService {

    public AppLauncherService() {
        super("AppLauncherService");
    }

    @Override
    public void onHandleIntent(Intent intent) {

        COMMON_TAG = getClass().getCanonicalName();

        super.onHandleIntent(intent);

        String packageName = intent.getStringExtra(ConstantValues.PACKAGE_NAME);

        final Intent externalAppIntent = new Intent();
        externalAppIntent.setComponent(new ComponentName(packageName, intent.getStringExtra(ConstantValues.ACTIVITY_NAME)));

        externalAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(externalAppIntent);
    }
}