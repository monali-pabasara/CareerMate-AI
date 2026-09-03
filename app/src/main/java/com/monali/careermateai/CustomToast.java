package com.monali.careermateai;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CustomToast {

    public static void showSuccess(Context context, String message) {
        showCustomToast(context, message, R.drawable.custom_toast_success_bg, "✓");
    }

    public static void showError(Context context, String message) {
        showCustomToast(context, message, R.drawable.custom_toast_error_bg, "✕");
    }

    public static void showInfo(Context context, String message) {
        showCustomToast(context, message, R.drawable.custom_toast_info_bg, "ℹ");
    }

    private static void showCustomToast(Context context, String message, int backgroundResource, String icon) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast_layout, null);

        LinearLayout toastRoot = layout.findViewById(R.id.toastRoot);
        TextView toastMessageText = layout.findViewById(R.id.toastMessageText);

        toastRoot.setBackgroundResource(backgroundResource);
        toastMessageText.setText(icon + " " + message);

        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);
        toast.setView(layout);
        toast.show();
    }
}