package com.oneup.uplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.oneup.uplayer.activity.PlaylistActivity;

//TODO: Test headset plugging, starting with headset plugged in or not.

public class MainReceiver extends BroadcastReceiver {
    private static final String TAG = "UPlayer";

    private static final String PREF_HEADSET_STATE = "headset_state";

    private Context context;
    private SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "MainReceiver.onReceive()");
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "No action");
            return;
        }

        switch (action) {
            case Intent.ACTION_HEADSET_PLUG:
                headsetPlug(intent.getIntExtra("state", -1));
                break;
            default:
                Log.e(TAG, "Invalid action: '" + action + "'");
                break;
        }
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
