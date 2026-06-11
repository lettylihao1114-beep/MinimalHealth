package com.minimalhealth.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String TAG = "ConnectivityReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            Log.d(TAG, "网络已连接");
            // 网络恢复时尝试启动提醒服务（如果未运行）
            HealthReminderService.startService(context);
        } else {
            Log.d(TAG, "网络已断开");
        }
    }
}