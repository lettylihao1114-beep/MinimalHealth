package com.minimalhealth.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class HealthReminderService extends Service {

    private static final String TAG = "HealthReminder";
    private static final String CHANNEL_ID = "health_reminder";
    private static final int NOTIFICATION_ID_BASE = 1000;
    private static final long CHECK_INTERVAL = 15 * 60 * 1000L; // 15分钟检查一次

    /** 供外部调用的便捷启动方法 */
    public static void startService(Context context) {
        Intent intent = new Intent(context, HealthReminderService.class);
        context.startService(intent);
    }

    private Handler handler = new Handler();
    private Runnable checkRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "健康提醒服务已启动");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndNotify();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.post(checkRunnable);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        Log.d(TAG, "健康提醒服务已停止");
        super.onDestroy();
    }

    private void checkAndNotify() {
        SharedPreferences prefs = getSharedPreferences("reminder_settings", MODE_PRIVATE);
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        // 饮水提醒: 9:00 - 21:00 之间每2小时提醒一次
        boolean waterEnabled = prefs.getBoolean("water_reminder", true);
        if (waterEnabled && hour >= 9 && hour <= 21 && minute == 0) {
            sendNotification("💧 饮水时间到",
                    "已经一小时没喝水了，来一杯水吧！保持每日" + prefs.getInt("water_goal", 2500) + "ml目标！",
                    NOTIFICATION_ID_BASE + 1);
        }

        // 运动提醒: 每天10:00和15:00
        boolean exerciseEnabled = prefs.getBoolean("exercise_reminder", true);
        if (exerciseEnabled && ((hour == 10 && minute == 0) || (hour == 15 && minute == 0))) {
            sendNotification("🏃 该运动了",
                    "久坐不动有害健康，站起来活动一下吧！",
                    NOTIFICATION_ID_BASE + 2);
        }

        // 睡眠提醒: 每天21:30
        boolean sleepEnabled = prefs.getBoolean("sleep_reminder", true);
        if (sleepEnabled && hour == 21 && minute == 30) {
            sendNotification("🌙 该休息了",
                    "充足的睡眠是健康的基石，建议保证7-8小时睡眠时间！",
                    NOTIFICATION_ID_BASE + 3);
        }
    }

    private void sendNotification(String title, String content, int id) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(id, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "健康提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("饮水、运动、睡眠等健康提醒通知");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}