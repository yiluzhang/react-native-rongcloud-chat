package com.rongcloudchat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;

public class RongCloudConnectionListener {
    private static boolean isRegistered = false;

    public static void registerConnectionStatusListener() {
        if (isRegistered) return;

        RongIM.setConnectionStatusListener(new RongIMClient.ConnectionStatusListener() {
            @Override
            public void onChanged(ConnectionStatus status) {
                WritableMap data = Arguments.createMap();
                data.putInt("code", status.getValue());
                RongCloudChatModule.sendEvent("onRCIMConnectionStatusChanged", data);
            }
        });

        isRegistered = true;
    }

}