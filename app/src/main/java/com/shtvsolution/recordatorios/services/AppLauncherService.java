package com.shtvsolution.recordatorios.services;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import com.shtvsolution.recordatorios.R;
import com.shtvsolution.recordatorios.utils.ToastUtils;

import static com.shtvsolution.recordatorios.utils.ToastUtils.DEFAULT_TOAST_DURATION;
import static com.shtvsolution.recordatorios.utils.ToastUtils.DEFAULT_TOAST_SIZE;

//Service that starts an external app
//@author Eduardo on 26/07/2018.

public class AppLauncherService extends IntentService {

    public AppLauncherService() {
        super("AppLauncherService");
    }

    @Override
    public void onHandleIntent(Intent intent) {

        String packageName = intent.getStringExtra("package");

        Log.d("STIMULUS", packageName);

        final Intent externalAppIntent = new Intent();
        externalAppIntent.setComponent(new ComponentName(packageName, intent.getStringExtra("class")));

        externalAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (intent.hasExtra("action"))
            externalAppIntent.putExtra("action", intent.getStringExtra("action"));

        startActivity(externalAppIntent);

        stopSelf();
    }
}