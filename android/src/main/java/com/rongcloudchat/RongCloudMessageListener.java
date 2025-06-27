package com.rongcloudchat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.listener.OnReceiveMessageWrapperListener;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.ReceivedProfile;
import io.rong.message.FileMessage;
import io.rong.message.HQVoiceMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.ReferenceMessage;
import io.rong.message.SightMessage;
import io.rong.message.TextMessage;
import io.rong.message.VoiceMessage;

public class RongCloudMessageListener {
    private static boolean isRegistered = false;

    private static final OnReceiveMessageWrapperListener messageListener = new OnReceiveMessageWrapperListener() {
        @Override
        public void onReceivedMessage(Message message, ReceivedProfile profile) {
            WritableMap map = messageToWritableMap(message);
            WritableMap data = Arguments.createMap();
            data.putMap("message", map);
            data.putInt("left", profile.getLeft());
            RongCloudChatModule.sendEvent("onRCIMMessageReceived", data);
        }
    };

    public static void registerMessageListener() {
        if (isRegistered) return;

        isRegistered = RongIMClient.addOnReceiveMessageListener(messageListener);
    }

    public static WritableMap messageToWritableMap(Message message) {
        WritableMap map = Arguments.createMap();
        map.putInt("conversationType", message.getConversationType().getValue());
        map.putString("targetId", message.getTargetId());
        map.putString("channelId", message.getChannelId());
        map.putString("objectName", message.getObjectName());
        map.putInt("messageId", message.getMessageId());
        map.putString("messageUId", message.getUId());
        map.putInt("messageDirection", message.getMessageDirection().getValue());
        map.putString("senderUserId", message.getSenderUserId());
        map.putDouble("sentTime", message.getSentTime());
        map.putInt("sentStatus", message.getSentStatus().getValue());
        map.putDouble("receivedTime", message.getReceivedTime());
        map.putInt("receivedStatus", message.getReceivedStatus().getFlag());
        map.putString("extra", message.getExtra());

        MessageContent content = message.getContent();

        if (content instanceof MediaMessageContent) {
            MediaMessageContent mediaMsg = (MediaMessageContent) content;
            map.putString("name", mediaMsg.getName());
            map.putString("localPath", mediaMsg.getLocalPath() == null ? "" : mediaMsg.getLocalPath().toString());
            map.putString("remoteUrl", mediaMsg.getMediaUrl() == null ? "" : mediaMsg.getMediaUrl().toString());
        }

        if (content instanceof TextMessage) {
            TextMessage textMsg = (TextMessage) content;
            map.putString("content", textMsg.getContent());
        } else if (content instanceof ReferenceMessage) {
            ReferenceMessage refMsg = (ReferenceMessage) content;
            map.putString("content", refMsg.getEditSendText());
            map.putString("referMsgUserId", refMsg.getUserId());
            map.putString("referMsgUid", refMsg.getReferMsgUid());
        } else if (content instanceof FileMessage) {
            FileMessage fileMsg = (FileMessage) content;
            map.putDouble("size", fileMsg.getSize());
            map.putString("type", fileMsg.getType());
        } else if (content instanceof VoiceMessage) {
            VoiceMessage voiceMsg = (VoiceMessage) content;
            map.putInt("duration", voiceMsg.getDuration());
        } else if (content instanceof HQVoiceMessage) {
            HQVoiceMessage voiceMsg = (HQVoiceMessage) content;
            map.putInt("duration", voiceMsg.getDuration());
        } else if (content instanceof SightMessage) {
            SightMessage sightMsg = (SightMessage) content;
            map.putDouble("size", sightMsg.getSize());
            map.putInt("duration", sightMsg.getDuration());
        }

        return map;
    }
}
