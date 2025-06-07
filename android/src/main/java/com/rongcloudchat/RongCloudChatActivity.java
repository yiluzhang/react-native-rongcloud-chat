package com.rongcloudchat;

import android.Manifest;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.util.Arrays;
import java.util.List;

import io.rong.imkit.conversation.ConversationFragment;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;

public class RongCloudChatActivity extends AppCompatActivity {

    private PermissionCheckUtil.IPermissionEventCallback permissionCallback;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.WHITE);
        setContentView(R.layout.rong_chat_activity);

        // 注册权限回调
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    TopPermissionTip.dismiss(this);
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (permissionCallback != null) {
                        if (allGranted) {
                            permissionCallback.confirmed();
                        } else {
                            permissionCallback.cancelled();
                        }
                        permissionCallback = null;
                    }
                }
        );

        // 设置权限监听
        PermissionCheckUtil.setRequestPermissionListListener((context, permissionsNotGranted, callback) -> {
            permissionCallback = callback;
            List<String> albumPermission = Arrays.asList(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
            if (albumPermission.contains(permissionsNotGranted.get(0))) {
                permissionsNotGranted.add(Manifest.permission.CAMERA);
            }
            TopPermissionTip.show(context, getPermissionTip(permissionsNotGranted));
            permissionLauncher.launch(permissionsNotGranted.toArray(new String[0]));
        });

        // 获取目标用户信息
        String targetId = getIntent().getStringExtra("targetId");
        int conversationType = getIntent().getIntExtra("conversationType", 1);
        Conversation.ConversationType type = Conversation.ConversationType.setValue(conversationType);

        if (targetId == null) {
            finish();
            return;
        }

        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        TextView title = new TextView(this);
        title.setText(targetId);
        title.setTextColor(Color.parseColor("#222222"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.create(title.getTypeface(), Typeface.NORMAL), Typeface.NORMAL);
        title.setGravity(Gravity.CENTER);

        if (type == Conversation.ConversationType.PRIVATE) {
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(targetId);
            title.setText(userInfo != null ? userInfo.getName() : "");
        } else if (type == Conversation.ConversationType.GROUP) {
            Group group = RongUserInfoManager.getInstance().getGroupInfo(targetId);
            title.setText(group != null ? group.getName() : "群聊");
        } else if (type == Conversation.ConversationType.DISCUSSION) {
            title.setText("讨论组");
        } else {
            title.setText(targetId);
        }

        Toolbar.LayoutParams lp = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );

        Drawable upArrow = AppCompatResources.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        if (upArrow != null) {
            upArrow.setTint(Color.BLACK);
            toolbar.setNavigationIcon(upArrow);
        }
        toolbar.addView(title, lp);

        ConversationFragment fragment = new ConversationFragment();
        fragment.initConversation(
                targetId,
                type,
                null
        );
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.rong_container, fragment)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        WritableMap data = Arguments.createMap();
        data.putInt("conversationType", getIntent().getIntExtra("conversationType", 1));
        data.putString("targetId", getIntent().getStringExtra("targetId"));
        RongCloudChatModule.sendEvent("onRCIMChatClosed", data);
        super.onDestroy();
    }

    private String getPermissionTip(List<String> permissions) {
        for (String permission : permissions) {
            String res = "";
            if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
                res = "请授权使用麦克风，用于语音聊天";
            } else if (Manifest.permission.CAMERA.equals(permission)) {
                res = "请授权使用相机，用于拍摄照片或视频";
            } else if (
                    Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission) ||
                            Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission) ||
                            Manifest.permission.READ_MEDIA_IMAGES.equals(permission) ||
                            Manifest.permission.READ_MEDIA_VIDEO.equals(permission) ||
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED.equals(permission)
            ) {
                res = "请授权使用相机、相册和视频，用于图片聊天";
            } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
                res = "请授权获取位置，用于聊天发送位置信息";
            }

            if (!res.isEmpty()) {
                return res;
            }
        }
        return "为了正常使用聊天，请允许授予";
    }
}