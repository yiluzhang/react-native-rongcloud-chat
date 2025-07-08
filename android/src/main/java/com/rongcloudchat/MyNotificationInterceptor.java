package com.rongcloudchat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.facebook.react.bridge.ReactApplicationContext;

import io.rong.imkit.notification.DefaultInterceptor;

public class MyNotificationInterceptor extends DefaultInterceptor {

    private static boolean disableBar = false;
    private static boolean disableSound = false;
    private final ReactApplicationContext reactContext;

    public MyNotificationInterceptor(ReactApplicationContext context) {
        this.reactContext = context;
    }

    public static void setDisableNotificationBar(boolean disabled) {
        disableBar = disabled;
    }

    public static void setDisableNotificationSound(boolean disabled) {
        disableSound = disabled;
    }

    @Override
    public NotificationChannel onRegisterChannel(NotificationChannel channel) {
        if (!disableBar) {
            if (disableSound) {
                channel.setSound(null, null);
            }

            return channel;
        }

        if (!disableSound) {
            playSound();
        }

        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        return channel;
    }

    private void playSound() {
        try {
            if (reactContext != null) {
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone ringtone = RingtoneManager.getRingtone(reactContext, soundUri);
                if (ringtone != null) {
                    ringtone.play();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}