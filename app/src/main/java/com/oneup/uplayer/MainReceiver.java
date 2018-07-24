package com.oneup.uplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.oneup.uplayer.activity.PlaylistActivity;

public class MainReceiver extends BroadcastReceiver {
    private static final String TAG = "UPlayer";

    private static final String PREF_HEADSET_STATE = "headset_state";

    private Context context;
    private SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "No broadcast action");
            return;
        }

        switch (action) {
            case Intent.ACTION_SCREEN_ON:
                screenOn();
                break;
            case Intent.ACTION_HEADSET_PLUG:
                headsetPlug(intent.getIntExtra("state", -1));
                break;
            default:
                Log.e(TAG, "Invalid broadcast action: '" + action + "'");
                break;
        }
    }

    private void screenOn() {
        Log.d(TAG, "MainReceiver.screenOn()");

        // Update notification when the screen is turned on.
        context.startService(new Intent(context, MainService.class)
                .putExtra(MainService.EXTRA_ACTION, MainService.ACTION_UPDATE));
    }

    private void headsetPlug(int state) {
        Log.d(TAG, "MainReceiver.headsetPlug(" + state + ")");

        int prevState = preferences.getInt(PREF_HEADSET_STATE, 0);
        Log.d(TAG, "prevState=" + prevState);

        // Stop playback when the headset is unplugged and was previously plugged in.
        if (state == 0 && prevState == 1) {
            PlaylistActivity.finishIfRunning();
            context.stopService(new Intent(context, MainService.class));
        }

        preferences.edit().putInt(PREF_HEADSET_STATE, state).apply();
    }
}
