package com.oneup.uplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.oneup.uplayer.activity.PlaylistActivity;

public class MainReceiver extends BroadcastReceiver {
    private static final String TAG = "UPlayer";

    private static final String ARG_STATE = "state";
    private static final String KEY_HEADSET_STATE = "headsetState";

    private Context context;
    private SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        preferences = context.getSharedPreferences(TAG, 0);

        String action = intent.getAction();
        Log.d(TAG, "MainReceiver.onReceive(" + action + ")");

        if (action != null && action.equals(Intent.ACTION_HEADSET_PLUG)) {
            onHeadsetPlug(intent.getIntExtra(ARG_STATE, -1));
        }
    }

    private void onHeadsetPlug(int state) {
        Log.d(TAG, "MainReceiver.onHeadsetPlug(" + state + ")");

        switch (state) {
            case 0:
                Log.d(TAG, "Headset unplugged");
                if (preferences.getInt(KEY_HEADSET_STATE, 0) == 1) {
                    PlaylistActivity.finishIfRunning();
                    context.stopService(new Intent(context, MainService.class));
                }
                break;
        }

        preferences.edit().putInt(KEY_HEADSET_STATE, state).apply();
    }
}
