import { NativeEventEmitter, NativeModules, type EmitterSubscription } from 'react-native';

const { RongCloudChat } = NativeModules;

export type ObjectName = 'RC:TxtMsg' | 'RC:ReferenceMsg' | 'RC:FileMsg' | 'RC:ImgMsg' | 'RC:HQVCMsg' | 'RC:VcMsg' | 'RC:GIFMsg' | 'RC:LBSMsg' | 'RC:SightMsg';

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
  name?: string; // 媒体类消息的文件名称
  localPath?: string; // 媒体类消息的本地路径
  remoteUrl?: string; // 媒体类消息的远程路径
  duration?: number; // 媒体类消息的时长
  size?: number; // 文件消息或视频消息的文件大小
  type?: string; // 文件消息的文件类型
}

type RongCloudChatType = {
  init(appKey: string): void;
  connect(token: string, name: string, portrait: string): Promise<boolean>;
  disconnect(): void;
  logout(): void;
  openChat(conversationType: number, target: string): Promise<boolean>;
  getConnectionStatus(): Promise<{ code: number }>;
  // 设置用户信息或群组信息缓存
  refreshInfoCache(userInfo: { type: 'user' | 'group'; id: string; name: string; portrait: string }): void;
  // 清空用户信息或群组信息缓存，Android 端不支持
  clearInfoCache(): void;
  // 清除历史消息
  clearHistoryMessages(conversationType: number, targetId: string, recordTime: number, clearRemote: boolean): Promise<boolean>;
  // 清除未读消息数量
  clearMessagesUnreadStatus(conversationType: number, targetId: string): Promise<boolean>;
  // 连接状态变化时会触发此事件
  addConnectionStatusListener(listener: (event: { code: number }) => void): EmitterSubscription;
  // 收到新消息时会触发此事件
  addMessageReceivedListener(listener: (event: { message: RCIMMessage; left: number }) => void): EmitterSubscription;
  // 聊天窗口关闭时会触发此事件
  addChatClosedListener(listener: (event: { conversationType: number; targetId: string }) => void): EmitterSubscription;
  // SDK 请求用户信息或群组信息时会触发此事件
  addInfoRequestedListener(listener: (event: { type: 'user' | 'group'; id: string }) => void): EmitterSubscription;
};

const emitter = new NativeEventEmitter(RongCloudChat);

const addConnectionStatusListener: RongCloudChatType['addConnectionStatusListener'] = (listener) => emitter.addListener('onRCIMConnectionStatusChanged', listener);

const addMessageReceivedListener: RongCloudChatType['addMessageReceivedListener'] = (listener) => emitter.addListener('RCIMMessageReceived', listener);

const addChatClosedListener: RongCloudChatType['addChatClosedListener'] = (listener) => emitter.addListener('onRCIMChatClosed', listener);

const addInfoRequestedListener: RongCloudChatType['addInfoRequestedListener'] = (listener) => emitter.addListener('onRCIMInfoRequested', listener);

const RongCloudChatModule: RongCloudChatType = {
  init: RongCloudChat.init,
  connect: RongCloudChat.connect,
  getConnectionStatus: RongCloudChat.getConnectionStatus,
  disconnect: RongCloudChat.disconnect,
  logout: RongCloudChat.logout,
  openChat: RongCloudChat.openChat,
  refreshInfoCache: RongCloudChat.refreshInfoCache,
  clearInfoCache: RongCloudChat.clearInfoCache,
  clearHistoryMessages: RongCloudChat.clearHistoryMessages,
  clearMessagesUnreadStatus: RongCloudChat.clearMessagesUnreadStatus,
  addConnectionStatusListener,
  addMessageReceivedListener,
  addChatClosedListener,
  addInfoRequestedListener,
};

export default RongCloudChatModule;
