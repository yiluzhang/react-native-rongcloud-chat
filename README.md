# react-native-rongcloud-chat

融云 IM KIT 5.12.5 简易封装，支持 Android 和 iOS 平台。

## 功能
- 支持新消息接收
- 支持单聊、群聊、讨论组，聊天页支持发送语音、图片，Android 端支持发送文件

## Installation

```sh
yarn add react-native-rongcloud-chat
```

## Usage


```js
import RongCloudChat from 'react-native-rongcloud-chat';

// 初始化
RongCloudChat.init('xxxxxx');

// 连接
RongCloudChat.connect(token, 'Nickname', 'https://image.cn/avatar.png');

// 断开
RongCloudChat.disconnect();

// 登出
RongCloudChat.logout();

// 设置用户信息或群组信息
RongCloudChat.refreshInfoCache({ type: 'user', id: 'user_identifier', name: 'Nickname', portrait: 'https://image.cn/avatar.png' });

// 接收新消息
RongCloudChat.addMessageReceivedListener((res) => { console.log('收到新消息：', res); });

// 打开聊天页
RongCloudChat.openChat(1, 'user_identifier');

// 删除历史消息
RongCloudChat.clearHistoryMessages(1, 'user_identifier', 0, true);

```

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
