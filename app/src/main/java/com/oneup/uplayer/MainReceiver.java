package com.oneup.uplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.oneup.uplayer.activity.PlaylistActivity;
import com.oneup.uplayer.util.Settings;

public class MainReceiver extends BroadcastReceiver {
    private static final String TAG = "UPlayer";

    private static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
    private static final String STATE = "state";

    private Context context;
    private Settings settings;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        settings = Settings.get(context);

        String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "No broadcast action");
            return;
        }

        switch (action) {
            case Intent.ACTION_SCREEN_ON:
            case Intent.ACTION_USER_PRESENT:
                screenOn();
                break;
            case Intent.ACTION_HEADSET_PLUG:
                headsetPlug(intent.getIntExtra(STATE, -1));
                break;
            case ACTION_PHONE_STATE:
                phoneState(intent.getStringExtra(STATE));
                break;
        }
    }

    private void screenOn() {
        Log.d(TAG, "MainReceiver.screenOn()");
        MainService.update(context, false, null);
    }

    private void headsetPlug(int state) {
        Log.d(TAG, "MainReceiver.headsetPlug(" + state + ")");

        int prevState = settings.getInt(R.string.key_headset_state, 0);
        Log.d(TAG, "prevState=" + prevState);

        // Stop playback when the headset is unplugged and was previously plugged in.
        if (state == 0 && prevState == 1) {
            PlaylistActivity.finishIfRunning();
            context.stopService(new Intent(context, MainService.class));
        }

        settings.edit().putInt(R.string.key_headset_state, state).apply();
    }

    private void phoneState(String state) {
        Log.d(TAG, "MainReceiver.phoneState(" + state + ")");
        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            MainService.pause(context);
        }
    }
}
