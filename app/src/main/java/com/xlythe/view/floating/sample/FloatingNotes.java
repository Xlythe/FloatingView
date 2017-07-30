package com.xlythe.view.floating.sample;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xlythe.view.floating.FloatingView;

public class FloatingNotes extends FloatingView {
    @NonNull
    @Override
    public View inflateButton(@NonNull ViewGroup parent) {
        return LayoutInflater.from(getContext()).inflate(R.layout.floating_icon, parent, false);
    }

    @NonNull
    @Override
    public View inflateView(@NonNull ViewGroup parent) {
        return LayoutInflater.from(getContext()).inflate(R.layout.floating_notes, parent, false);
    }

    @NonNull
    @Override
    protected Notification createNotification() {
        Intent intent = new Intent(this, FloatingNotes.class).setAction(ACTION_OPEN);
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.floating_notification_description))
                .setContentIntent(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    @Override
    public void onShow() {

    }

    @Override
    public void onHide() {

    }
}
