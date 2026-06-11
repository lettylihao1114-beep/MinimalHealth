package com.minimalhealth.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机自启广播接收器 — 系统启动完成后自动启动健康提醒服务。
 * 注册在 AndroidManifest.xml 中，需 RECEIVE_BOOT_COMPLETED 权限。
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "系统启动完成，启动健康提醒服务");
            Intent serviceIntent = new Intent(context, HealthReminderService.class);
            context.startService(serviceIntent);
        }
    }
}