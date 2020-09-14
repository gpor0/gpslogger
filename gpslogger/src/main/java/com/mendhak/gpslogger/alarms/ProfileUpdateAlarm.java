package com.mendhak.gpslogger.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.ProfileEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import de.greenrobot.event.EventBus;

public class ProfileUpdateAlarm extends BroadcastReceiver {

    private static final Logger LOG = Logs.of(ProfileUpdateAlarm.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        PreferenceHelper instance = PreferenceHelper.getInstance();
        String profileUrl = instance.getProfileUrl();

        if (Strings.isNullOrEmpty(profileUrl)) {
            return;
        }

        EventBus.getDefault().post(new ProfileEvents.DownloadProfile(profileUrl, false));
    }

    public static Integer interval() {

        Integer interval = 6 * 60; //6h
        try {
            PreferenceHelper instance = PreferenceHelper.getInstance();
            if (instance.getProfileUrlInterval() != null)
                interval = Integer.parseInt(instance.getProfileUrlInterval());
        } catch (NumberFormatException e) {
        }

        return 60000 * interval;
    }

    public void setAlarm(Context context) {
        cancelAlarm(context);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, ProfileUpdateAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval(), pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context) {
        Intent intent = new Intent(context, ProfileUpdateAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}