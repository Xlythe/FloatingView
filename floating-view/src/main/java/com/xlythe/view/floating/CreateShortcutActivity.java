package com.xlythe.view.floating;

import android.app.NotificationChannel;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;

/**
 * Creates the shortcut icon
 */
public abstract class CreateShortcutActivity extends Activity {

    @DrawableRes
    public abstract int getShortcutIcon();

    public abstract CharSequence getShortcutName();

    public abstract Intent getOpenShortcutActivityIntent();

    @RequiresApi(Bubbles.MIN_SDK_BUBBLES)
    protected abstract NotificationChannel createNotificationChannel();

    @Override
    void onSuccess() {
        Intent.ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(this, getShortcutIcon());

        Intent intent = new Intent();

        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, getOpenShortcutActivityIntent());
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getShortcutName());
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    void onFailure() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @RequiresApi(Bubbles.MIN_SDK_BUBBLES)
    @Override
    String ensureNotificationChannel() {
        return createNotificationChannel().getId();
    }
}
