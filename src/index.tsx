import { NativeEventEmitter, NativeModules, Platform, type EmitterSubscription } from 'react-native';

const { RongCloudChat } = NativeModules;

export type ObjectName = 'RC:TxtMsg' | 'RC:ReferenceMsg' | 'RC:FileMsg' | 'RC:ImgMsg' | 'RC:HQVCMsg' | 'RC:VcMsg' | 'RC:GIFMsg' | 'RC:LBSMsg' | 'RC:SightMsg';

export const MessageDirection = {
  // 发送
  SEND: 1,
  // 接收
  RECEIVE: 2,
} as const;

export const ConversationType = {
  // 单聊
  PRIVATE: 1,
  // 讨论组
  DISCUSSION: 2,
  // 群组
  GROUP: 3,
  // 聊天室
  CHATROOM: 4,
  // 客服
  CUSTOMER_SERVICE: 5,
  // 系统
  SYSTEM: 6,
  // 应用内公众服务
  APP_PUBLIC_SERVICE: 7,
  // 跨应用公众服务
  PUBLIC_SERVICE: 8,
  // 推送服务
  PUSH_SERVICE: 9,
  // 超级群
  ULTRA_GROUP: 10,
  // 加密会话
  ENCRYPTED: 11,
  // RTC 会话
  RTC_ROOM: 12,
} as const;

export const SentStatus = {
  // 发送中
  SENDING: 10,
  // 发送失败
  FAILED: 20,
  // 已发送成功
  SENT: 30,
  // 对方已接收
  RECEIVED: 40,
  /// 对方已阅读
  READ: 50,
  // 对方已销毁
  DESTROYED: 60,
  // 发送已取消
  CANCELED: 70,
} as const;

export const ReceivedStatus = {
  // 未读
  UNREAD: 0,
  // 已读
  READ: 1,
  // 已听，仅用于语音消息
  LISTENED: 2,
  // 已下载
  DOWNLOADED: 4,
  // 该消息已被同时在线或之前登录的其他设备接收。只要任何其他设备先收到该消息，当前设备就会有该状态值。
  RETRIEVED: 8,
  // 该消息是被多端同时收取的。（即其他端正同时登录，一条消息被同时发往多端。客户可以通过这个状态值更新自己的某些 UI 状态）。
  MULTIPLERECEIVE: 16,
} as const;

export interface RCIMMessage {
  conversationType: number;
  targetId: string;
  channelId: string;
  objectName: ObjectName;
  messageId: number;
  messageUId: string;
  messageDirection: number;
  senderUserId: string;
  sentTime: number;
  sentStatus: number;
  receivedTime: number;
  receivedStatus: number;
  extra: string;
  content: string;
  // 媒体类消息的文件名称
  name?: string;
  // 媒体类消息的本地路径
  localPath?: string;
  // 媒体类消息的远程路径
  remoteUrl?: string;
  // 媒体类消息的时长
  duration?: number;
  // 文件消息或视频消息的文件大小
  size?: number;
  // 文件消息的文件类型
  type?: string;
}

type RongCloudChatType = {
  init(appKey: string): void;
  // 关闭本地通知，仅 Android 端支持
  setLocalNotificationEnabled(enabled: boolean): void;
  connect(token: string, name: string, portrait: string): Promise<boolean>;
  disconnect(): void;
  logout(): void;
  openChat(conversationType: number, target: string): Promise<boolean>;
  getConnectionStatus(): Promise<{ code: number }>;
  // 设置用户信息或群组信息缓存
  refreshInfoCache(userInfo: { type: 'user' | 'group'; id: string; name: string; portrait: string }): void;
  // 清空用户信息或群组信息缓存，仅 iOS 端支持
  clearInfoCache(): void;
  // 清除历史消息
  clearHistoryMessages(conversationType: number, targetId: string, recordTime: number, clearRemote: boolean): Promise<boolean>;
  // 清除未读消息数量
  clearMessagesUnreadStatus(conversationType: number, targetId: string): Promise<boolean>;
  // 全部已读
  markAllConversationsAsRead(): Promise<boolean>;
  // 连接状态变化时会触发此事件
  addConnectionStatusListener(listener: (event: { code: number }) => void): EmitterSubscription;
  // 收到新消息时会触发此事件
  addMessageReceivedListener(listener: (event: { message: RCIMMessage; left: number }) => void): EmitterSubscription;
  // 聊天窗口关闭时会触发此事件
  addChatClosedListener(listener: (event: { conversationType: number; targetId: string }) => void): EmitterSubscription;
  // 聊天窗口关闭时，接收最新的消息
  addChatLatestMessageListener(listener: (event: { conversationType: number; targetId: string; message: RCIMMessage }) => void): EmitterSubscription;
  // SDK 请求用户信息或群组信息时会触发此事件
  addInfoRequestedListener(listener: (event: { type: 'user' | 'group'; id: string }) => void): EmitterSubscription;
};

const setLocalNotificationEnabled: RongCloudChatType['setLocalNotificationEnabled'] = (enabled) => {
  if (Platform.OS === 'android') {
    RongCloudChat.setLocalNotificationEnabled(enabled);
  }
};

const clearInfoCache: RongCloudChatType['clearInfoCache'] = () => {
  if (Platform.OS === 'ios') {
    RongCloudChat.clearInfoCache();
  }
};

const emitter = new NativeEventEmitter(RongCloudChat);

const addConnectionStatusListener: RongCloudChatType['addConnectionStatusListener'] = (listener) => emitter.addListener('onRCIMConnectionStatusChanged', listener);

const addMessageReceivedListener: RongCloudChatType['addMessageReceivedListener'] = (listener) => emitter.addListener('onRCIMMessageReceived', listener);

const addChatClosedListener: RongCloudChatType['addChatClosedListener'] = (listener) => emitter.addListener('onRCIMChatClosed', listener);

const addChatLatestMessageListener: RongCloudChatType['addChatLatestMessageListener'] = (listener) => emitter.addListener('onRCIMChatLatestMessage', listener);

const addInfoRequestedListener: RongCloudChatType['addInfoRequestedListener'] = (listener) => emitter.addListener('onRCIMInfoRequested', listener);

const RongCloudChatModule: RongCloudChatType = {
  init: RongCloudChat.init,
  setLocalNotificationEnabled,
  connect: RongCloudChat.connect,
  getConnectionStatus: RongCloudChat.getConnectionStatus,
  disconnect: RongCloudChat.disconnect,
  logout: RongCloudChat.logout,
  openChat: RongCloudChat.openChat,
  refreshInfoCache: RongCloudChat.refreshInfoCache,
  clearInfoCache,
  clearHistoryMessages: RongCloudChat.clearHistoryMessages,
  clearMessagesUnreadStatus: RongCloudChat.clearMessagesUnreadStatus,
  markAllConversationsAsRead: RongCloudChat.markAllConversationsAsRead,
  addConnectionStatusListener,
  addMessageReceivedListener,
  addChatClosedListener,
  addChatLatestMessageListener,
  addInfoRequestedListener,
};

export default RongCloudChatModule;
