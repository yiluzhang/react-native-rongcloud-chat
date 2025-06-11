package com.rongcloudchat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

public class TopPermissionTip {
    private static TextView tipView;

    public static void show(Context context, String permissionTip) {
        if (tipView != null) return;

        ViewGroup rootView = ((Activity) context).findViewById(android.R.id.content);
        tipView = new TextView(context);
        tipView.setText(permissionTip);
        tipView.setBackgroundColor(Color.parseColor("#FFF3CD"));
        tipView.setTextColor(Color.parseColor("#856404"));
        tipView.setPadding(30, 60, 30, 60);
        tipView.setGravity(Gravity.CENTER);
        rootView.addView(tipView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
    }

    public static void dismiss(Context context) {
        if (tipView != null) {
            ViewGroup rootView = ((Activity) context).findViewById(android.R.id.content);
            rootView.removeView(tipView);
            tipView = null;
        }
    }
}