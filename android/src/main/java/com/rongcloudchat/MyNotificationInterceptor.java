package com.rongcloudchat;

import io.rong.imkit.notification.DefaultInterceptor;
import io.rong.imlib.model.Message;

public class MyNotificationInterceptor extends DefaultInterceptor {
    private static boolean disableNotification = false;

    public static void setNotificationDisabled(boolean disabled) {
        disableNotification = disabled;
    }

    @Override
    public boolean isNotificationIntercepted(Message message) {
        return disableNotification;
    }
}