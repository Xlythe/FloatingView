package com.xlythe.view.floating;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.Collections;

/**
 * When the shortcut icon is pressed, use this Activity to launch the overlay Service
 */
public abstract class OpenShortcutActivity extends Activity {
    // A fake ID used to create a shortcut. This is required in order to display a Bubble.
    private static final String SHORTCUT_ID = "floating.shortcutId";

    public static final String ACTION_OPEN = FloatingView.ACTION_OPEN;

    protected abstract Intent createServiceIntent();

    @RequiresApi(Bubbles.MIN_SDK_BUBBLES)
    protected abstract Intent createActivityIntent();

    @RequiresApi(Bubbles.MIN_SDK_BUBBLES)
    protected abstract Notification createNotification();

    @RequiresApi(Bubbles.MIN_SDK_BUBBLES)
    @Override
    String ensureNotificationChannel() {
        return createNotification().getChannelId();
    }

    @Override
    void onSuccess() {
        if (Build.VERSION.SDK_INT >= Bubbles.MIN_SDK_BUBBLES) {
            // On R+, we launch the floating view as a bubble
            Intent intent = createActivityIntent();
            Icon icon = getActivityIcon(intent);
            String name = getActivityName(intent);

            @SuppressLint("InlinedApi") PendingIntent bubbleIntent =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
            Notification.BubbleMetadata bubbleData =
                    new Notification.BubbleMetadata.Builder()
                            .setIntent(bubbleIntent)
                            .setIcon(icon)
                            .setDesiredHeight(600)
                            .setAutoExpandBubble(true)
                            .setSuppressNotification(true)
                            .build();

            // Bubbles require a MessagingStyle on earlier versions of R (see https://issuetracker.google.com/issues/150857757)
            Person person = new Person.Builder().setIcon(icon).setName(name).build();
            Notification.MessagingStyle style = new Notification.MessagingStyle(person);

            // Bubbles require a long lived shortcut on R+ (see https://developer.android.com/guide/topics/ui/conversations#bubbles)
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, SHORTCUT_ID)
                    .setIcon(icon)
                    .setShortLabel(name)
                    .setPerson(person)
                    .setLongLived(true)
                    .setIntent(new Intent(ACTION_OPEN).setComponent(getComponentName()))
                    .build();
            shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcutInfo));

            // Bubbles are launched by showing a notification.
            Notification notification = createNotification();
            @SuppressLint("InlinedApi") Notification.Builder builder =
                    new Notification.Builder(this, notification.getChannelId())
                            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(ACTION_OPEN).setComponent(getComponentName()), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE))
                            .setContentTitle(notification.extras.getCharSequence(Notification.EXTRA_TITLE))
                            .setContentText(notification.extras.getCharSequence(Notification.EXTRA_TEXT))
                            .setSmallIcon(notification.getSmallIcon())
                            .setBubbleMetadata(bubbleData)
                            .addPerson(person)
                            .setShortcutId(SHORTCUT_ID)
                            .setStyle(style);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.notify(0, builder.build());

            // Clean up the shortcuts once we're done. Although it works if we immediately remove the
            // shortcut, the Bubble icon is loaded lazily and gets corrupted if we do so. Adding a short
            // delay fixes this problem.
            new Handler().postDelayed(() -> shortcutManager.removeDynamicShortcuts(Collections.singletonList(SHORTCUT_ID)), 5000);
        } else {
            // On pre-R, we launch the floating view as a service
            Intent intent = createServiceIntent();
            if (ACTION_OPEN.equals(getIntent().getAction()) && intent.getAction() == null) {
                intent.setAction(ACTION_OPEN);
            }
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
        finish();
    }

    @Override
    void onFailure() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.blank, R.anim.blank);
    }

    @RequiresApi(26)
    public Icon getActivityIcon(Intent intent) {
        PackageManager packageManager = getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
        Preconditions.checkNotNull(resolveInfo, "Failed to resolve " + intent);
        Drawable drawable = resolveInfo.loadIcon(packageManager);
        return Icon.createWithAdaptiveBitmap(toBitmap(drawable));
    }

    public String getActivityName(Intent intent) {
        PackageManager packageManager = getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
        Preconditions.checkNotNull(resolveInfo, "Failed to resolve " + intent);
        return getString(resolveInfo.activityInfo.labelRes);
    }

    public static Bitmap toBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
