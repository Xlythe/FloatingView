package com.xlythe.view.floating.sample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.xlythe.view.floating.OpenShortcutActivity;

/**
 * When the shortcut icon is pressed, use this Activity to launch the overlay Service
 */
public class OpenActivity extends OpenShortcutActivity {
    @Override
    public Intent createServiceIntent() {
        return new Intent(this, FloatingNotesService.class);
    }

    @Override
    public Intent createActivityIntent() {
        return new Intent(this, FloatingNotesActivity.class);
    }

    @RequiresApi(29)
    @Override
    protected Notification createNotification() {
        NotificationChannel notificationChannel = new NotificationChannel(FloatingNotesService.CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN);
        notificationChannel.setAllowBubbles(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        Intent intent = new Intent(ACTION_OPEN).setPackage(getPackageName());
        return new NotificationCompat.Builder(this, FloatingNotesService.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.floating_notification_description))
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }
}
