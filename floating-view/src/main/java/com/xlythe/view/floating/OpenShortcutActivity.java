package com.xlythe.view.floating;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

/**
 * When the shortcut icon is pressed, use this Activity to launch the overlay Service
 */
public abstract class OpenShortcutActivity extends Activity {
    // The minimum version to use Notification Bubbles.
    // Acceptable values are 29 (requires Dev Options), 30, and 999 (disabled).
    private static final int MIN_SDK_BUBBLES = 999;

    private static final int REQUEST_CODE_WINDOW_OVERLAY_PERMISSION = 10001;
    private static final int REQUEST_CODE_BUBBLES_PERMISSION = 10002;

    public static final String ACTION_OPEN = FloatingView.ACTION_OPEN;

    protected abstract Intent createServiceIntent();

    @RequiresApi(MIN_SDK_BUBBLES)
    protected abstract Intent createActivityIntent();

    @RequiresApi(MIN_SDK_BUBBLES)
    protected abstract Notification createNotification();

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
            PendingIntent bubbleIntent =
                    PendingIntent.getActivity(this, 0, intent, 0);
            Notification.BubbleMetadata bubbleData =
                    new Notification.BubbleMetadata.Builder()
                            .setIntent(bubbleIntent)
                            .setIcon(getActivityIcon(intent))
                            .setDesiredHeight(600)
                            .setAutoExpandBubble(true)
                            .setSuppressNotification(true)
                            .build();
            Person person = new Person.Builder().setIcon(getActivityIcon(intent)).setName(getActivityName(intent)).build();
            Notification notification = createNotification();
            Notification.Builder builder =
                    new Notification.Builder(this, notification.getChannelId())
                            .setContentIntent(notification.contentIntent)
                            .setSmallIcon(notification.getSmallIcon())
                            .setBubbleMetadata(bubbleData)
                            .addPerson(person);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.notify(0, builder.build());
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
        try {
            bubblesEnabledGlobally = Settings.Global.getInt(getContentResolver(), "notification_bubbles") == 1;
        } catch (Settings.SettingNotFoundException e) {
            // If we're not able to read the system setting, just assume the best case.
            bubblesEnabledGlobally = true;
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        return bubblesEnabledGlobally && notificationManager.areBubblesAllowed();
    }
}
