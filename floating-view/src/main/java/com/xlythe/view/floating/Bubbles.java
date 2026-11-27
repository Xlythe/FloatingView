package com.xlythe.view.floating;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;


class Bubbles {
  private Bubbles() {}

  // The minimum version to use Notification Bubbles.
  // Acceptable values are 29 (requires Dev Options), 30, and 999 (disabled).
  static final int MIN_SDK_BUBBLES = 30;

  static boolean canDisplayBubbles(Context context, String notificationChannelId) {
    if (Build.VERSION.SDK_INT < MIN_SDK_BUBBLES) {
      return false;
    }

    // NotificationManager#areBubblesAllowed does not check if bubbles have been globally disabled,
    // (verified on R), so we use this check as well. Luckily, ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS
    // works well for both cases.
    boolean globallyEnabled = Settings.Global.getInt(context.getContentResolver(), "notification_bubbles", 1) == 1;
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    boolean appAllowsBubbles = Build.VERSION.SDK_INT >= 31
            ? manager.getBubblePreference() != NotificationManager.BUBBLE_PREFERENCE_NONE
            : manager.areBubblesAllowed();
    android.app.NotificationChannel channel = manager.getNotificationChannel(notificationChannelId);
    return globallyEnabled && appAllowsBubbles && channel != null
            && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
  }
}
