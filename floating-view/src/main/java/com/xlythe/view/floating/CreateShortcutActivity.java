package com.xlythe.view.floating;

import android.app.Activity;
import android.app.NotificationChannel;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;

/**
 * Creates the shortcut icon
 */
public abstract class CreateShortcutActivity extends Activity {
    private static final String TAG = "CreateShortcutActivity";

    private static final int REQUEST_CODE_WINDOW_OVERLAY_PERMISSION = 10001;
    private static final int REQUEST_CODE_BUBBLES_PERMISSION = 10002;

    @DrawableRes
    public abstract int getShortcutIcon();

    public abstract CharSequence getShortcutName();

    public abstract Intent getOpenShortcutActivityIntent();

    @RequiresApi(Bubbles.MIN_SDK_BUBBLES)
    protected abstract NotificationChannel createNotificationChannel();

    public void onCreate(Bundle state) {
        super.onCreate(state);

        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            // From M~Q, we use window overlays to draw the floating view. From R+ we use bubbles.
            // Note that the notification channel must be created before we launch the Bubbles settings activity.
            if (Build.VERSION.SDK_INT >= Bubbles.MIN_SDK_BUBBLES && !Bubbles.canDisplayBubbles(this, createNotificationChannel().getId())) {
                startActivityForResult(
                        new Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()),
                        REQUEST_CODE_BUBBLES_PERMISSION);
            } else if (Build.VERSION.SDK_INT < Bubbles.MIN_SDK_BUBBLES && Build.VERSION.SDK_INT >= 23 && !canDrawOverlays()) {
                startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())),
                        REQUEST_CODE_WINDOW_OVERLAY_PERMISSION);
            } else {
                onSuccess();
            }
        } else {
            Log.w(TAG, "CreateShortcutActivity called with unexpected Action " + getIntent().getAction());
            onFailure();
        }
    }

    private void onSuccess() {
        Intent.ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(this, getShortcutIcon());

        Intent intent = new Intent();

        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, getOpenShortcutActivityIntent());
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getShortcutName());
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

        setResult(RESULT_OK, intent);
        finish();
    }

    private void onFailure() {
        setResult(RESULT_CANCELED);
        finish();
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
            if (Build.VERSION.SDK_INT >= Bubbles.MIN_SDK_BUBBLES && Bubbles.canDisplayBubbles(this, createNotificationChannel().getId())) {
                onSuccess();
            } else {
                onFailure();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT < 23) {
            // Before 23, just adding the permission to the manifest was enough.
            return true;
        }

        return Settings.canDrawOverlays(this);
    }
}
