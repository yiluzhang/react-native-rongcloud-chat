#import "RongCloudChat.h"
#import <RongIMKit/RongIMKit.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTUtils.h>
#import "RongCloudChatViewController.h"

@implementation RongCloudChat

static RongCloudChat *_instance = nil;

- (instancetype)init
{
  self = [super init];
  if (self) {
    _instance = self;
  }
  return self;
}

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents {
  return @[
    @"onRCIMConnectionStatusChanged",
    @"onRCIMMessageReceived",
    @"onRCIMChatClosed",
    @"onRCIMChatLatestMessage",
    @"onRCIMInfoRequested"
  ];
}

+ (void)sendEvent:(NSString *)eventName body:(NSDictionary *)body {
  if (_instance != nil && _instance.bridge != nil) {
    [_instance sendEventWithName:eventName body:body];
  }
}

- (void)onRCIMConnectionStatusChanged:(RCConnectionStatus)status {
  [RongCloudChat sendEvent:@"onRCIMConnectionStatusChanged" body:@{@"code": @((NSInteger)status)}];
}

- (void)onRCIMReceiveMessage:(RCMessage *)message left:(int)left {
  [RongCloudChat sendEvent:@"onRCIMMessageReceived" body:@{@"message":[RongCloudChat dictionaryFromRCMessage:message], @"left":@(left)}];
}

- (void)getUserInfoWithUserId:(NSString *)userId
                   completion:(void (^)(RCUserInfo *userInfo))completion {
  [RongCloudChat sendEvent:@"onRCIMInfoRequested" body:@{ @"type":@"user", @"id":userId }];
  completion(nil);
}

- (void)getGroupInfoWithGroupId:(NSString *)groupId
                     completion:(void (^)(RCGroup *groupInfo))completion {
  [RongCloudChat sendEvent:@"onRCIMInfoRequested" body:@{ @"type":@"group", @"id":groupId }];
  completion(nil);
}

RCT_EXPORT_METHOD(init:(NSString *)appKey) {
  dispatch_async(dispatch_get_main_queue(), ^{
    [[RCIM sharedRCIM] initWithAppKey:appKey option:nil];
    [RCIM sharedRCIM].enablePersistentUserInfoCache = YES;
    [RCIM sharedRCIM].connectionStatusDelegate = self;
    [RCIM sharedRCIM].receiveMessageDelegate = self;
    [[RCIM sharedRCIM] setUserInfoDataSource:(id<RCIMUserInfoDataSource>)self];
    [[RCIM sharedRCIM] setGroupInfoDataSource:(id<RCIMGroupInfoDataSource>)self];
  });
}

RCT_EXPORT_METHOD(setLocalNotificationEnabled:(BOOL)enabled) {
  dispatch_async(dispatch_get_main_queue(), ^{
    RCKitConfigCenter.message.disableMessageNotificaiton = !enabled;
  });
}

RCT_REMAP_METHOD(connect,
                 token:(NSString *)token
                 name:(NSString *)name
                 portrait:(NSString *)portrait
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
  dispatch_async(dispatch_get_main_queue(), ^{
    [[RCIM sharedRCIM] connectWithToken:token timeLimit:5 dbOpened:nil success:^(NSString * _Nonnull userId) {
      RCUserInfo *userInfo = [[RCUserInfo alloc] initWithUserId:userId name:name portrait: portrait];
      [RCIM sharedRCIM].currentUserInfo = userInfo;
      [[RCIM sharedRCIM] refreshUserInfoCache:userInfo withUserId:userId];
      resolve(@(YES));
    } error:^(RCConnectErrorCode status) {
      reject([@(status) stringValue], [NSString stringWithFormat:@"连接失败，错误码 %ld", (long)status], nil);
    }];
  });
}

RCT_EXPORT_METHOD(getConnectionStatus:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  dispatch_async(dispatch_get_main_queue(), ^{
    RCConnectionStatus status = [[RCIM sharedRCIM] getConnectionStatus];
    resolve(@{ @"code": @((NSInteger)status) });
  });
}

RCT_EXPORT_METHOD(disconnect) {
  dispatch_async(dispatch_get_main_queue(), ^{
    [[RCIM sharedRCIM] disconnect];
  });
}

RCT_EXPORT_METHOD(logout) {
  dispatch_async(dispatch_get_main_queue(), ^{
    [[RCIM sharedRCIM] logout];
  });
}

RCT_REMAP_METHOD(openChat,
                 conversationType:(NSInteger)conversationType
                 targetId:(NSString *)targetId
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
  dispatch_async(dispatch_get_main_queue(), ^{
    RongCloudChatViewController *vc = [[RongCloudChatViewController alloc] init];
    vc.conversationType = (RCConversationType)conversationType;
    vc.targetId = targetId;

    UINavigationController *nav = [[UINavigationController alloc] initWithRootViewController:vc];
    nav.modalPresentationStyle = UIModalPresentationFullScreen;

    UIViewController *rootVC = RCTPresentedViewController();
    [rootVC presentViewController:nav animated:YES completion:^{
      resolve(@(YES));
    }];
  });
}

RCT_EXPORT_METHOD(refreshInfoCache:(NSDictionary *)info) {
  dispatch_async(dispatch_get_main_queue(), ^{
    NSString *type = info[@"type"];
    NSString *identifier = info[@"id"];
    NSString *name = info[@"name"];
    NSString *portrait = info[@"portrait"];
    
    if (identifier == nil || identifier.length < 1) {
      return;
    }
    
    if ([type isEqualToString:@"user"]) {
      RCUserInfo *user = [[RCUserInfo alloc] initWithUserId:identifier name:name portrait:portrait];
      [[RCIM sharedRCIM] refreshUserInfoCache:user withUserId:identifier];
    } else if ([type isEqualToString:@"group"]) {
      RCGroup *group = [[RCGroup alloc] initWithGroupId:identifier groupName:name portraitUri:portrait];
      [[RCIM sharedRCIM] refreshGroupInfoCache:group withGroupId:identifier];
    }
  });
}

RCT_EXPORT_METHOD(clearInfoCache) {
  dispatch_async(dispatch_get_main_queue(), ^{
    [[RCIM sharedRCIM] clearUserInfoCache];
    [[RCIM sharedRCIM] clearGroupInfoCache];
  });
}

RCT_EXPORT_METHOD(clearHistoryMessages:(NSInteger)conversationType
                  targetId:(NSString *)targetId
                  recordTime:(NSNumber *)recordTime
                  clearRemote:(BOOL)clearRemote
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  dispatch_async(dispatch_get_main_queue(), ^{
    long long time = [recordTime longLongValue];
    [[RCCoreClient sharedCoreClient] clearHistoryMessages:(RCConversationType)conversationType targetId:targetId recordTime:time clearRemote:clearRemote success:^{
      resolve(@(YES));
    } error:^(RCErrorCode status) {
      reject([@(status) stringValue], @"清除历史消息失败", nil);
    }];
  });
}

RCT_EXPORT_METHOD(clearMessagesUnreadStatus:(NSInteger)conversationType
                  targetId:(NSString *)targetId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  dispatch_async(dispatch_get_main_queue(), ^{
    [[RCCoreClient sharedCoreClient] clearMessagesUnreadStatus:(RCConversationType)conversationType targetId:targetId completion:^(BOOL ret) {
      if (ret) {
        resolve(@(ret));
      } else {
        reject(@"-1", @"清除消息未读状态失败", nil);
      }
    }];
  });
}

RCT_EXPORT_METHOD(markAllConversationsAsRead:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  dispatch_async(dispatch_get_main_queue(), ^{
    [[RCCoreClient sharedCoreClient] getConversationList:@[
      @(ConversationType_PRIVATE),
      @(ConversationType_DISCUSSION),
      @(ConversationType_GROUP),
      @(ConversationType_CHATROOM),
      @(ConversationType_CUSTOMERSERVICE),
      @(ConversationType_SYSTEM),
      @(ConversationType_APPSERVICE),
      @(ConversationType_PUBLICSERVICE),
      @(ConversationType_PUSHSERVICE)
    ] completion:^(NSArray<RCConversation *> * _Nullable conversationList) {
      if (conversationList == nil) {
        reject(@"-1", @"会话列表为空或获取失败", nil);
        return;
      }

      for (RCConversation *c in conversationList) {
        [[RCCoreClient sharedCoreClient] clearMessagesUnreadStatus:c.conversationType targetId:c.targetId completion:nil];
      }
      resolve(@(YES));
    }];
  });
}

RCT_EXPORT_METHOD(sendMessage:(NSInteger)conversationType
                  targetId:(NSString *)targetId
                  contentMap:(NSDictionary *)contentMap
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  dispatch_async(dispatch_get_main_queue(), ^{
    RCConversationType type = (RCConversationType)conversationType;

    NSString *objectName = contentMap[@"objectName"];
    if (![objectName isKindOfClass:[NSString class]] || objectName.length == 0) {
      reject(@"invalid_object_name", @"Missing or invalid objectName in contentMap", nil);
      return;
    }

    RCMessageContent *content = nil;

    if ([objectName isEqualToString:@"RC:TxtMsg"]) {
      NSString *text = contentMap[@"content"] ?: @"";
      content = [RCTextMessage messageWithContent:text];
    } else {
      reject(@"unsupported_type", [NSString stringWithFormat:@"Unsupported message type: %@", objectName], nil);
      return;
    }

    RCMessage *message = [[RCMessage alloc] initWithType:type
                                                targetId:targetId
                                               direction:MessageDirection_SEND
                                               messageId:-1
                                                 content:content];

    [[RCIM sharedRCIM] sendMessage:type
                          targetId:targetId
                           content:content
                       pushContent:nil
                          pushData:nil
                           success:^(long messageId) {
      RCMessage *sentMessage = [[RCCoreClient sharedCoreClient] getMessage:messageId];
      NSDictionary *map = [RongCloudChat dictionaryFromRCMessage:sentMessage];
      resolve(map);
    } error:^(RCErrorCode nErrorCode, long messageId) {
      reject([NSString stringWithFormat:@"%ld", (long)nErrorCode],
             [NSString stringWithFormat:@"发送失败: %ld", (long)nErrorCode],
             nil);
    }];
  });
}

+ (NSDictionary *)dictionaryFromRCMessage:(RCMessage *)message {
  NSMutableDictionary *dict = [NSMutableDictionary dictionary];
  dict[@"conversationType"] = @(message.conversationType);
  dict[@"targetId"] = message.targetId;
  dict[@"channelId"] = message.channelId;
  dict[@"objectName"] = message.objectName;
  dict[@"messageId"] = @(message.messageId);
  dict[@"messageUId"] = message.messageUId;
  dict[@"messageDirection"] = @(message.messageDirection);
  dict[@"senderUserId"] = message.senderUserId;
  dict[@"sentTime"] = @(message.sentTime);
  dict[@"sentStatus"] = @(message.sentStatus);
  dict[@"receivedTime"] = @(message.receivedTime);
  dict[@"receivedStatus"] = @(message.receivedStatus);
  dict[@"extra"] = message.extra;
  
  RCMessageContent *content = message.content;
  
  if ([content isKindOfClass:[RCMediaMessageContent class]]) {
    RCMediaMessageContent *msg = (RCMediaMessageContent *)content;
    dict[@"name"] = msg.name;
    dict[@"localPath"] = msg.localPath;
    dict[@"remoteUrl"] = msg.remoteUrl;
  }

  if ([content isKindOfClass:[RCTextMessage class]]) {
    RCTextMessage *textMsg = (RCTextMessage *)content;
    dict[@"content"] = textMsg.content ?: @"";
  } else if ([content isKindOfClass:[RCReferenceMessage class]]) {
    RCReferenceMessage *refMsg = (RCReferenceMessage *)content;
    dict[@"content"] = refMsg.content;
    dict[@"referMsgUserId"] = refMsg.referMsgUserId;
    dict[@"referMsgUid"] = refMsg.referMsgUid;
  } else if ([content isKindOfClass:[RCFileMessage class]]) {
    RCFileMessage *fileMsg = (RCFileMessage *)content;
    dict[@"size"] = @(fileMsg.size);
    dict[@"type"] = fileMsg.type;
  } else if ([content isKindOfClass:[RCVoiceMessage class]]) {
    RCVoiceMessage *voiceMsg = (RCVoiceMessage *)content;
    dict[@"duration"] = @(voiceMsg.duration);
  } else if ([content isKindOfClass:[RCHQVoiceMessage class]]) {
    RCHQVoiceMessage *voiceMsg = (RCHQVoiceMessage *)content;
    dict[@"duration"] = @(voiceMsg.duration);
  } else if ([content isKindOfClass:[RCSightMessage class]]) {
    RCSightMessage *sightMessage = (RCSightMessage *)content;
    dict[@"size"] = @(sightMessage.size);
    dict[@"duration"] = @(sightMessage.duration);
  }

  return dict;
}

@end
