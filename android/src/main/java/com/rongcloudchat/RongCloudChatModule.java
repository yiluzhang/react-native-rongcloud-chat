package com.rongcloudchat;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.List;
import java.util.Objects;

import io.rong.imkit.GlideKitImageEngine;
import io.rong.imkit.RongIM;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;

public class RongCloudChatModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext;

    public RongCloudChatModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        RongCloudMessageListener.registerMessageListener();
        RongCloudConnectionListener.registerConnectionStatusListener();
        RongConfigCenter.notificationConfig().setInterceptor(new MyNotificationInterceptor());
    }

    @NonNull
    @Override
    public String getName() {
        return "RongCloudChat";
    }

    public static void sendEvent(String eventName, @Nullable WritableMap data) {
        WritableMap copiedMap = null;
        if (data != null) {
            copiedMap = Arguments.createMap();
            copiedMap.merge(data);
        }

        if (reactContext != null && reactContext.hasActiveReactInstance()) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, copiedMap);
        } else {
            Log.w("RongCloudChatModule", "Cannot send event: " + eventName + " - reactContext is not ready");
        }
    }

    @ReactMethod
    public void init(String appKey) {
        RongIM.init((Application) getReactApplicationContext().getApplicationContext(), appKey);

        RongIM.setUserInfoProvider(userId -> {
            WritableMap params = Arguments.createMap();
            params.putString("type", "user");
            params.putString("id", userId);
            sendEvent("onRCIMInfoRequested", params);
            return null;
        }, true);

        RongIM.setGroupInfoProvider(groupId -> {
            WritableMap params = Arguments.createMap();
            params.putString("type", "group");
            params.putString("id", groupId);
            sendEvent("onRCIMInfoRequested", params);
            return null;
        }, true);
    }

    @ReactMethod
    public void setLocalNotificationEnabled(boolean enabled) {
        MyNotificationInterceptor.setNotificationDisabled(!enabled);
    }

    @ReactMethod
    public void connect(String token, String name, String portrait, Promise promise) {
        RongIM.connect(token, new RongIMClient.ConnectCallback() {
            @Override
            public void onSuccess(String userId) {
                UserInfo userInfo = new UserInfo(userId, name, android.net.Uri.parse(portrait));
                RongIM.getInstance().setCurrentUserInfo(userInfo);
                RongIM.getInstance().refreshUserInfoCache(userInfo);
                promise.resolve(true);
            }

            @Override
            public void onError(RongIMClient.ConnectionErrorCode errorCode) {
                promise.reject(String.valueOf(errorCode.getValue()), "连接失败，错误码 " + errorCode.getValue());
            }

            @Override
            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus status) {
                // optional
            }
        });
    }

    @ReactMethod
    public void getConnectionStatus(Promise promise) {
        RongIMClient.ConnectionStatusListener.ConnectionStatus status = RongIMClient.getInstance().getCurrentConnectionStatus();
        WritableMap data = Arguments.createMap();
        data.putInt("code", status.getValue());
        promise.resolve(data);
    }

    @ReactMethod
    public void disconnect() {
        RongIM.getInstance().disconnect();
    }

    @ReactMethod
    public void logout() {
        RongIM.getInstance().logout();
    }

    @ReactMethod
    public void openChat(int conversationType, String targetId, Promise promise) {
        RongConfigCenter.conversationConfig().setShowMoreClickAction(false);
        RongConfigCenter.featureConfig().setKitImageEngine(new GlideKitImageEngine() {
            @Override
            public void loadConversationPortrait(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView, Message message) {
                int cornerRadiusPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6, context.getResources().getDisplayMetrics());
                Glide.with(context).load(url)
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(cornerRadiusPx)))
                        .into(imageView);
            }
        });

        Intent intent = new Intent(getReactApplicationContext(), RongCloudChatActivity.class);
        intent.putExtra("targetId", targetId);
        intent.putExtra("conversationType", conversationType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getReactApplicationContext().startActivity(intent);

        promise.resolve(true);
    }

    @ReactMethod
    public void refreshInfoCache(ReadableMap userMap) {
        String type = userMap.getString("type");
        String id = userMap.getString("id");
        String name = userMap.getString("name");
        String portrait = userMap.getString("portrait");

        if (Objects.equals(type, "user")) {
            UserInfo userInfo = new UserInfo(id, name, android.net.Uri.parse(portrait));
            RongIM.getInstance().refreshUserInfoCache(userInfo);
        } else if (Objects.equals(type, "group")) {
            Group group = new Group(id, name, android.net.Uri.parse(portrait));
            RongIM.getInstance().refreshGroupInfoCache(group);
        }
    }

    @ReactMethod
    public void clearHistoryMessages(int conversationType, String targetId, double recordTime, boolean clearRemote, Promise promise) {
        long time = (long) recordTime;
        Conversation.ConversationType type = Conversation.ConversationType.setValue(conversationType);

        RongCoreClient.getInstance().cleanHistoryMessages(type, targetId, time, clearRemote, new IRongCoreCallback.OperationCallback() {
            @Override
            public void onSuccess() {
                promise.resolve(true);
            }

            @Override
            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                promise.reject(String.valueOf(coreErrorCode.getValue()), coreErrorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void clearMessagesUnreadStatus(int conversationType, String targetId, Promise promise) {
        Conversation.ConversationType type = Conversation.ConversationType.setValue(conversationType);
        RongIMClient.getInstance().clearMessagesUnreadStatus(type, targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                promise.resolve(true);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(String.valueOf(errorCode.getValue()), errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void markAllConversationsAsRead(Promise promise) {
        RongIMClient.getInstance().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                for (Conversation c : conversations) {
                    RongIMClient.getInstance().clearMessagesUnreadStatus(c.getConversationType(), c.getTargetId(), null);
                }
                promise.resolve(true);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(String.valueOf(errorCode.getValue()), "获取会话列表失败");
            }
        });
    }
}