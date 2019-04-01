package com.oneup.uplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

//TODO: contains(), StringSet, Float

public class Settings {
    private Context context;
    private SharedPreferences settings;

    private Settings(Context context, SharedPreferences settings) {
        this.context = context;
        this.settings = settings;
    }

    public String getString(int keyId, String defValue) {
        return settings.getString(getKey(keyId), defValue);
    }

    public int getInt(int keyId, int defValue) {
        return settings.getInt(getKey(keyId), defValue);
    }

    public boolean getBoolean(int keyId, boolean defValue) {
        return settings.getBoolean(getKey(keyId), defValue);
    }

    public int getXmlInt(int keyId, int defValue) {
        String s = getString(keyId, null);
        return TextUtils.isEmpty(s) ? defValue : Integer.parseInt(s);
    }

    private String getKey(int resId) {
        return context.getString(resId);
    }

    public Editor edit() {
        return new Editor(settings.edit());
    }

    public static Settings get(Context context) {
        return new Settings(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    public class Editor {
        private SharedPreferences.Editor editor;

        private Editor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        public Editor putInt(int keyId, int value) {
            editor.putInt(getKey(keyId), value);
            return this;
        }

        public Editor putBoolean(int keyId, boolean value) {
            editor.putBoolean(getKey(keyId), value);
            return this;
        }

        public void apply() {
            editor.apply();
        }
    }
}
