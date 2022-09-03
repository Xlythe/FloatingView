package com.xlythe.view.floating.sample;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;

import com.xlythe.view.floating.CreateShortcutActivity;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;

/**
 * Creates the shortcut icon
 */
public class CreateActivity extends CreateShortcutActivity {
    @Override
    public CharSequence getShortcutName() {
        return getString(R.string.app_name);
    }

    @DrawableRes
    @Override
    public int getShortcutIcon() {
        return R.mipmap.ic_launcher;
    }

    @Override
    public Intent getOpenShortcutActivityIntent() {
        return new Intent(this, OpenActivity.class);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected NotificationChannel createNotificationChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(FloatingNotesService.CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN);
        notificationChannel.setAllowBubbles(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);
        return notificationChannel;
    }
}
