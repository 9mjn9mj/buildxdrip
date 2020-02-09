package com.eveningoutpost.dexdrip.watch.miband;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Date;

import static com.eveningoutpost.dexdrip.watch.miband.MiBand.MiBandType.MI_BAND2;
// very lightweight entry point class to avoid loader overhead when not in use

public class MiBandEntry {
    public static final String PREF_MIBAND_ENABLED = "miband_enabled";
    public static final String PREF_MIBAND_MAC = "miband_data_mac";
    public static final String PREF_MIBAND_AUTH_KEY = "miband_data_authkey";
    public static final String PREF_MIBAND_SEND_READINGS = "miband_send_readings";
    public static final String PREF_MIBAND_SEND_READINGS_AS_NOTIFICATION = "miband_send_readings_as_notification";
    public static final String PREF_VIBRATE_ON_READINGS = "miband_vibrate_on_readings";
    public static final String PREF_SEND_ALARMS = "miband_send_alarms";
    public static final String PREF_CALL_ALERTS = "miband_option_call_notifications";
    public static final String PREF_MIBAND_SETTINGS = "miband_settings";
    public static final String PREF_MIBAND_PREFERENCES = "miband_preferences";
    public static final String PREF_MIBAND_UPDATE_BG = "update_miband_bg";
    public static final String PREF_MIBAND_NIGHTMODE_ENABLED = "miband_nightmode_enabled";
    public static final String PREF_MIBAND_NIGHTMODE_START = "miband_nightmode_start";
    public static final String PREF_MIBAND_NIGHTMODE_END = "miband_nightmode_end";
    public static final String PREF_MIBAND_NIGHTMODE_INTERVAL = "miband_nightmode_interval";
    public static final String PREF_MIBAND_GRAPH_HOURS = "miband_graph_hours";

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_ENABLED);
    }

    public static boolean areAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_SEND_ALARMS);
    }

    public static boolean isVibrateOnReadings() {
        return Pref.getBooleanDefaultFalse(PREF_VIBRATE_ON_READINGS);
    }

    public static boolean isNeedSendReading() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_MIBAND_SEND_READINGS);
    }

    public static boolean isNeedSendReadingAsNotification() {
        if (isEnabled() && MiBand.getMibandType() == MI_BAND2) return true;
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_MIBAND_SEND_READINGS_AS_NOTIFICATION);
    }

    public static boolean areCallAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_CALL_ALERTS);
    }

    public static boolean isNightModeEnabled() {
        if (MiBand.getMibandType() == MI_BAND2) return false;
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_NIGHTMODE_ENABLED);
    }

    public static Date getNightModeStart() {
        return new Date(Pref.getLong(PREF_MIBAND_NIGHTMODE_START, 0));
    }

    public static Date getNightModeEnd() {
        return new Date(Pref.getLong(PREF_MIBAND_NIGHTMODE_END, 0));
    }

    public static void setNightModeInterval(int val) {
        Pref.setInt(PREF_MIBAND_NIGHTMODE_INTERVAL, val);
    }

    public static int getNightModeInterval() {
        return (Pref.getInt(PREF_MIBAND_NIGHTMODE_INTERVAL, 0) + 1) * 5;
    }

    public static void initialStartIfEnabled() {
        if (isEnabled()) {
            Inevitable.task("mb-full-initial-start", 500, new Runnable() {
                @Override
                public void run() {
                    showLatestBG();
                }
            });
        }
    }

    public static Preference.OnPreferenceChangeListener sBindMibandPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            try {
                String key = preference.getKey();
                if (key.equals(MiBandEntry.PREF_MIBAND_NIGHTMODE_INTERVAL)) {
                    setNightModeInterval((int) value);
                    final String minutes = xdrip.gs(R.string.unit_minutes);
                    final String title_text = xdrip.gs(R.string.title_miband_interval_in_nightmode);
                    preference.setTitle(String.format("%s %d %s", title_text, MiBandEntry.getNightModeInterval(), minutes));
                }
            } catch (Exception e) {
                //
            }
            return true;
        }
    };

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.startsWith("miband")) {
                UserError.Log.d("miband", "Preference key: " + key);
                refresh();
            }
        }
    };

    static void refresh() {
        Inevitable.task("miband-preference-changed", 1000, () -> JoH.startService(MiBandService.class, "function", "refresh"));
    }

    public static void showLatestBG() {
        if (isNeedSendReading()) {
            JoH.startService(MiBandService.class, "function", "update_bg");
        }
    }

    public static void forceShowLatestBG() {
        if (isNeedSendReading()) {
            JoH.startService(MiBandService.class, "function", "update_bg_force");
        }
    }

    public static void sendPrefIntent(MiBandService.MIBAND_INTEND_STATES state, Integer progress, String descrText) {
        final Intent progressIntent = new Intent(Intents.PREFERENCE_INTENT);
        progressIntent.putExtra("state", state.name());
        progressIntent.putExtra("progress", progress);
        if (!descrText.isEmpty())
            progressIntent.putExtra("descr_text", descrText);
        LocalBroadcastManager.getInstance(xdrip.getAppContext()).sendBroadcast(progressIntent);
    }
}
