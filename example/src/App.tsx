import React, { useEffect } from 'react';
import { Alert, StatusBar, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import RongCloudChat from 'react-native-rongcloud-chat';
import type { RCIMMessage } from 'react-native-rongcloud-chat';

RongCloudChat.init('xxxxxx');

type User = {
  token: string;
  userId: string;
  name: string;
  portrait: string;
};

const users: User[] = [
  {
    token: 'xxx',
    userId: 'xxxx',
    name: 'Nick',
    portrait: 'https://zos.alipayobjects.com/rmsportal/jkjgkEfvpUPVyRjUImniVslZfWPnJuuZ.png',
  },
  {
    token: 'xxx',
    name: 'Lucy',
    userId: 'xxxx',
    portrait: 'https://zos.alipayobjects.com/rmsportal/jkjgkEfvpUPVyRjUImniVslZfWPnJuuZ.png',
  },
];

const getUserName = (userId: string) => {
  const user = users.find((o) => o.userId === userId);
  return user?.name || userId.substring(0, 6);
};

const getContent = (message: RCIMMessage) => {
  switch (message.objectName) {
    case 'RC:TxtMsg':
    case 'RC:ReferenceMsg':
      return message.content;
    case 'RC:FileMsg':
      return `[文件 ${message.name}]`;
    case 'RC:ImgMsg':
      return `[图片 ${message.name}]`;
    case 'RC:VcMsg':
    case 'RC:HQVCMsg':
      return `[语音消息 ${message.duration}'']`;
    case 'RC:GIFMsg':
      return '[GIF消息]';
    case 'RC:LBSMsg':
      return '[位置]';
    case 'RC:SightMsg':
      return '[视频]';
    default:
      return '[未知消息]';
  }
};

const STATE_MAP: Record<string, string> = {
  0: '连接成功',
  1: '网络不可用',
  10: '连接中...',
  11: '连接失败或未连接',
  12: '已登出',
  13: '连接挂起',
  14: '自动连接超时',
  15: 'Token无效',
};

function App(): React.JSX.Element {
  const [user, setUser] = React.useState<User>();
  const [state, setState] = React.useState<number>();
  const [message, setMessage] = React.useState<RCIMMessage>();

  useEffect(() => {
    const sub1 = RongCloudChat.addConnectionStatusListener((res) => {
      console.log('连接状态更新', res.code);
      setState(res.code);
    });

    const sub2 = RongCloudChat.addMessageReceivedListener((res) => {
      console.log('收到新消息', res);
      setMessage(res.message);
    });

    const sub3 = RongCloudChat.addChatClosedListener((res) => {
      console.log('聊天结束', res);
    });

    const sub4 = RongCloudChat.addInfoRequestedListener((res) => {
      console.log('需要信息', res);
    });

    return () => {
      sub1.remove();
      sub2.remove();
      sub3.remove();
      sub4.remove();
    };
  }, []);

  const login = (data: User) => {
    return RongCloudChat.connect(data.token, data.name, data.portrait)
      .then(() => {
        setUser(data);
      })
      .catch((err) => {
        Alert.alert('提示', err.message);
      });
  };

  const logout = () => {
    RongCloudChat.disconnect();
    setUser(undefined);
    setMessage(undefined);
  };

  const setUserInfoCache = () => {
    users.forEach((o) =>
      RongCloudChat.refreshInfoCache({
        type: 'user',
        id: o.userId,
        name: o.name,
        portrait: o.portrait,
      })
    );
    Alert.alert('提示', '设置用户信息成功');
  };

  const cleanUserInfoCache = () => {
    RongCloudChat.clearInfoCache();
    Alert.alert('提示', '清空用户信息成功');
  };

  const getStatus = () => {
    RongCloudChat.getConnectionStatus().then((res) => {
      setState(res.code);
    });
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#fff" />
      <View style={styles.tipContainer}>
        <Text style={[styles.txt, { paddingBottom: 12 }]}>
          {state === undefined && '未连接'}
          {state === 0 && '开始聊天'}
          {state ? STATE_MAP[state] || `未知状态 ${state}` : null}
        </Text>
        {message && (
          <View>
            <Text style={styles.txt}>收到新消息</Text>
            <Text style={styles.txt}>
              {getUserName(message.senderUserId)}：{getContent(message)}
            </Text>
          </View>
        )}
      </View>
      {state === 0 ? (
        <TouchableOpacity onPress={logout} style={styles.btn}>
          <Text style={styles.txt}>断开连接</Text>
        </TouchableOpacity>
      ) : (
        users.map((o) => (
          <TouchableOpacity key={o.userId} onPress={() => login(o)} style={styles.btn}>
            <Text style={styles.txt}>登录{o.name}</Text>
          </TouchableOpacity>
        ))
      )}
      {state === 0 && !!user
        ? users.map((o) =>
            o.userId === user?.userId ? null : (
              <TouchableOpacity key={o.userId} onPress={() => RongCloudChat.openChat(1, o.userId)} style={styles.btn}>
                <Text style={styles.txt}>和{o.name}聊天</Text>
              </TouchableOpacity>
            )
          )
        : null}
      <TouchableOpacity onPress={setUserInfoCache} style={styles.btn}>
        <Text style={styles.txt}>设置用户信息</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={cleanUserInfoCache} style={styles.btn}>
        <Text style={styles.txt}>清空用户信息</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={getStatus} style={styles.btn}>
        <Text style={styles.txt}>刷新连接状态</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  tipContainer: {
    height: 120,
    width: '100%',
    alignItems: 'center',
  },
  btn: {
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 18,
    width: 180,
    height: 50,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
  },
  txt: {
    fontSize: 16,
    color: '#000',
  },
});

export default App;
