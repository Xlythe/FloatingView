package com.xlythe.view.floating;

import android.app.Activity;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.Collections;

import static com.xlythe.view.floating.FloatingView.DEBUG;
import static com.xlythe.view.floating.FloatingView.TAG;

/**
 * When the shortcut icon is pressed, use this Activity to launch the overlay Service
 */
public abstract class OpenShortcutActivity extends Activity {
    // The minimum version to use Notification Bubbles.
    // Acceptable values are 29 (requires Dev Options), 30, and 999 (disabled).
    private static final int MIN_SDK_BUBBLES = 30;

    private static final int REQUEST_CODE_WINDOW_OVERLAY_PERMISSION = 10001;
    private static final int REQUEST_CODE_BUBBLES_PERMISSION = 10002;

    // A fake ID used to create a shortcut. This is required in order to display a Bubble.
    private static final String SHORTCUT_ID = "floating.shortcutId";

    public static final String ACTION_OPEN = FloatingView.ACTION_OPEN;

    protected abstract Intent createServiceIntent();

    @RequiresApi(MIN_SDK_BUBBLES)
    protected abstract Intent createActivityIntent();

    @RequiresApi(MIN_SDK_BUBBLES)
    protected abstract Notification createNotification();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // From M~Q, we use window overlays to draw the floating view. From R+ we use bubbles.
        if (Build.VERSION.SDK_INT >= MIN_SDK_BUBBLES && !canDisplayBubbles()) {
            startActivityForResult(
                    new Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()),
                    REQUEST_CODE_BUBBLES_PERMISSION);
        } else if (Build.VERSION.SDK_INT < MIN_SDK_BUBBLES && Build.VERSION.SDK_INT >= 23 && !canDrawOverlays()) {
            startActivityForResult(
                    new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())),
                    REQUEST_CODE_WINDOW_OVERLAY_PERMISSION);
        } else {
            onSuccess();
        }
    }

    private void onSuccess() {
        if (Build.VERSION.SDK_INT >= MIN_SDK_BUBBLES) {
            // On R+, we launch the floating view as a bubble
            Intent intent = createActivityIntent();
            Icon icon = getActivityIcon(intent);
            String name = getActivityName(intent);

            PendingIntent bubbleIntent =
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
            Notification.Builder builder =
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

    private void onFailure() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.blank, R.anim.blank);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_WINDOW_OVERLAY_PERMISSION) {
            if (canDrawOverlays()) {
                onSuccess();
            } else {
                onFailure();
            }
        } else if (requestCode == REQUEST_CODE_BUBBLES_PERMISSION) {
            if (canDisplayBubbles()) {
                onSuccess();
            } else {
                onFailure();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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

    private boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT < 23) {
            // Before 23, just adding the permission to the manifest was enough.
            return true;
        }

        return Settings.canDrawOverlays(this);
    }

    private boolean canDisplayBubbles() {
        if (Build.VERSION.SDK_INT < MIN_SDK_BUBBLES) {
            return false;
        }

        // NotificationManager#areBubblesAllowed does not check if bubbles have been globally disabled,
        // (verified on R), so we use this check as well. Luckily, ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS
        // works well for both cases.
        boolean bubblesEnabledGlobally;
        if (Build.VERSION.SDK_INT >= 30) {
            // In R+, the system setting is stored in Global.
            bubblesEnabledGlobally = Settings.Global.getInt(getContentResolver(), "notification_bubbles", 1) == 1;
        } else {
            // In Q, the system setting is stored in Secure.
            bubblesEnabledGlobally = Settings.Secure.getInt(getContentResolver(), "notification_bubbles", 1) == 1;
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (DEBUG) {
            Log.d(TAG, "Bubbles are " + (bubblesEnabledGlobally ? "" : "not ") + "enabled globally");
            Log.d(TAG, "Bubbles are " + (notificationManager.areBubblesAllowed() ? "" : "not ") + "enabled locally");

            // This boolean is supposed to be set to map to the current state of the Bubble notification.
            // True when the notification is displayed as a bubble and false when it's displayed as a notification.
            // This is set inside of BubbleController#onUserChangedBubble. However, every time I query this, it returns false.
            boolean channelCanBubble = notificationManager.getNotificationChannel(createNotification().getChannelId()).canBubble();
            Log.d(TAG, "Bubbles are " + (channelCanBubble ? "" : "not ") + "enabled for channel");
        }
        return bubblesEnabledGlobally && notificationManager.areBubblesAllowed();
    }
}
