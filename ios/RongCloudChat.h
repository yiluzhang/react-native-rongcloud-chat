#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <RongIMKit/RongIMKit.h>

@interface RongCloudChat : RCTEventEmitter <RCTBridgeModule, RCIMConnectionStatusDelegate, RCIMReceiveMessageDelegate>

+ (void)sendEvent:(NSString *)eventName body:(NSDictionary *)body;
+ (NSDictionary *)dictionaryFromRCMessage:(RCMessage *)message;

@end
