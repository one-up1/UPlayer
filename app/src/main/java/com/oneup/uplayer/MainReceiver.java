package com.oneup.uplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MainReceiver extends BroadcastReceiver {
    private static final String TAG = "UPlayer";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "MainReceiver.onReceive(" + action + ")");

        if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
            onHeadsetPlug(context, intent.getIntExtra("state", -1));
        }
    }

    private void onHeadsetPlug(Context context, int state) {
        Log.d(TAG, "MainReceiver.onHeadsetPlug(" + state + ")");

        switch (state) {
            case 0:
                Log.d(TAG, "Headset unplugged");
                context.stopService(new Intent(context, MainService.class));
                break;
        }
    }
}
