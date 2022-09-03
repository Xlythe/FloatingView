package com.xlythe.view.floating;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import static com.xlythe.view.floating.FloatingView.DEBUG;
import static com.xlythe.view.floating.FloatingView.TAG;

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
    boolean bubblesEnabledGlobally;
    if (Build.VERSION.SDK_INT >= 30) {
      // In R+, the system setting is stored in Global.
      bubblesEnabledGlobally = Settings.Global.getInt(context.getContentResolver(), "notification_bubbles", 1) == 1;
    } else {
      // In Q, the system setting is stored in Secure.
      bubblesEnabledGlobally = Settings.Secure.getInt(context.getContentResolver(), "notification_bubbles", 1) == 1;
    }

    NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
    if (DEBUG) {
      Log.d(TAG, "Bubbles are " + (bubblesEnabledGlobally ? "" : "not ") + "enabled globally");
      Log.d(TAG, "Bubbles are " + (notificationManager.areBubblesAllowed() ? "" : "not ") + "enabled locally");

      // This boolean is supposed to be set to map to the current state of the Bubble notification.
      // True when the notification is displayed as a bubble and false when it's displayed as a notification.
      // This is set inside of BubbleController#onUserChangedBubble. However, every time I query this, it returns false.
      boolean channelCanBubble = notificationManager.getNotificationChannel(notificationChannelId).canBubble();
      Log.d(TAG, "Bubbles are " + (channelCanBubble ? "" : "not ") + "enabled for channel");
    }
    return bubblesEnabledGlobally && notificationManager.areBubblesAllowed();
  }
}
