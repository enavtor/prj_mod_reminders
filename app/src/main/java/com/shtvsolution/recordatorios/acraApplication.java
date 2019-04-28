package com.shtvsolution.recordatorios;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        formUri="http://logs.shtvsolution.com:5984/acra-aal/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin = "aal_reporter",
        formUriBasicAuthPassword = "losPPyc0",
        // Your usual ACRA configuration
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.app_name,
        logcatArguments = {"-v", "threadtime", "-d"}

)

public class acraApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        try{
            ACRA.init(this);
        }catch (Exception e){

        }

        super.onCreate();
        acraApplication.context = getApplicationContext();
    }

    public static Context getAppliContext() {
        return acraApplication.context;
    }
}