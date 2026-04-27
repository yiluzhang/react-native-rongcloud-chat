package com.rongcloudchat;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;

import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.activity.FilePreviewActivity;

public class SafeFilePreviewActivity extends FilePreviewActivity {
    private static final String TAG = "SafeFilePreviewActivity";

    @Override
    public void openFile(String fileName, Uri uri) {
        try {
            if (openInsidePreview(fileName, uri)) {
                return;
            }

            Intent intent = createOpenFileIntent(fileName, uri);
            if (intent == null) {
                showCannotOpenToast();
                return;
            }

            Uri contentUri = intent.getData();
            if (contentUri != null) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    intent.setClipData(ClipData.newUri(getContentResolver(), fileName, contentUri));
                }
                grantUriPermissions(intent, contentUri);
            }

            Intent chooserIntent = Intent.createChooser(intent, fileName);
            chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && intent.getClipData() != null) {
                chooserIntent.setClipData(intent.getClipData());
            }

            startActivity(chooserIntent);
        } catch (ActivityNotFoundException exception) {
            System.out.println("SafeFilePreviewActivity: ActivityNotFoundException - No app handles this Intent: " + exception.getMessage());
            exception.printStackTrace();
            Toast.makeText(this, "本机暂无应用支持打开此文件 (" + fileName + ")", Toast.LENGTH_LONG).show();
        } catch (Exception exception) {
            System.out.println("SafeFilePreviewActivity: General Exception - " + exception.getMessage());
            exception.printStackTrace();
            Toast.makeText(this, "无法打开此文件: " + exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void grantUriPermissions(Intent intent, Uri contentUri) {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if ((resolveInfos == null || resolveInfos.isEmpty()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        }

        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return;
        }

        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null || resolveInfo.activityInfo.packageName == null) {
                continue;
            }
            grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        }
    }

    private Intent createOpenFileIntent(String fileName, Uri uri) {
        Uri contentUri = normalizeToContentUri(uri);
        if (contentUri == null) {
            return null;
        }

        String mimeType = resolveMimeType(fileName, contentUri);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setDataAndType(contentUri, mimeType);
        return intent;
    }

    private Uri normalizeToContentUri(Uri uri) {
        if (uri == null) {
            return null;
        }

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(scheme)) {
            return uri;
        }

        String filePath = uri.getPath();
        if (TextUtils.isEmpty(filePath)) {
            String uriText = uri.toString();
            if (!TextUtils.isEmpty(uriText) && uriText.startsWith("file://")) {
                filePath = uriText.substring(7);
            }
        }

        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        String authority = getApplicationContext().getPackageName() + ".provider";
        return FileProvider.getUriForFile(this, authority, new File(filePath));
    }

    private String resolveMimeType(String fileName, Uri contentUri) {
        String mimeType = getContentResolver().getType(contentUri);
        if (!TextUtils.isEmpty(mimeType)) {
            return mimeType;
        }

        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (!TextUtils.isEmpty(extension)) {
            String resolved = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (!TextUtils.isEmpty(resolved)) {
                return resolved;
            }
        }

        return "*/*";
    }

    private void showCannotOpenToast() {
        Toast.makeText(this, getString(R.string.rc_ac_file_preview_can_not_open_file), Toast.LENGTH_SHORT).show();
    }
}