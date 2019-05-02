package com.droidmare.reminders.utils;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

//Utils for creating custom Toasts
//@author Eduardo on 09/07/2018.

public class ToastUtils {

    private static Toast toast = null;

    private static CountDownTimer toastCountDown;

    public static final int DEFAULT_TOAST_SIZE = 25;
    public static final int DEFAULT_TOAST_DURATION = 5;

    //Method that creates and shows a Toast with a specific text size and duration (in seconds):
    public static void makeCustomToast(final Context context, final String text, final int size, final int seconds) {

        //If a toast is being displayed when a new toast is going to be shown, the first toast must be canceled, as well as its countdown:
        if (toastCountDown != null) {
            toastCountDown.cancel();
            toastCountDown = null;
        }

        if (toast != null) {
            toast.cancel();
            toast = null;
        }

        //The toast must always be created on the UI thread to avoid application malfunctions:
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                toast = Toast.makeText(context, text, Toast.LENGTH_LONG);

                LinearLayout toastLayout = (LinearLayout) toast.getView();

                TextView toastTextView = (TextView) toastLayout.getChildAt(0);

                toastTextView.setTextSize(size);

                //A countdown to display the toast with the specified duration is set:
                toastCountDown = new CountDownTimer(seconds * 1000, 1000) {

                    public void onTick(long millisUntilFinished) {
                        toast.show();
                    }

                    public void onFinish() {

                        if (toast != null) {
                            toast.cancel();
                            toast = null;
                        }

                        toastCountDown = null;
                    }
                };

                //Now the toast is shown and the countdown is started:
                toast.show();
                toastCountDown.start();
            }
        });
    }
}

