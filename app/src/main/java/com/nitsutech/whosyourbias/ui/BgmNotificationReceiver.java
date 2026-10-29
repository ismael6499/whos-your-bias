package com.nitsutech.whosyourbias.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BgmNotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_TOGGLE_PLAY = "com.nitsutech.whosyourbias.ACTION_TOGGLE_PLAY";
    public static final String ACTION_STOP_BGM = "com.nitsutech.whosyourbias.ACTION_STOP_BGM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (ACTION_TOGGLE_PLAY.equals(action)) {
            YoutubeBgmController.handleNotificationToggle(context);
        } else if (ACTION_STOP_BGM.equals(action)) {
            YoutubeBgmController.handleNotificationStop(context);
        }
    }
}
