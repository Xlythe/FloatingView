package com.xlythe.view.floating;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

class Bubbles {
  private Bubbles() {}

  // The shortcut-based BubbleMetadata.Builder was introduced in R. Earlier versions use overlays.
  static final int MIN_SDK_BUBBLES = Build.VERSION_CODES.R;

  static String shortcutId(ComponentName opener) {
    return "floating.shortcutId." + opener.flattenToShortString();
  }

  @RequiresApi(MIN_SDK_BUBBLES)
  static boolean canDisplayBubbles(Context context, String channelId, ComponentName opener) {
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (!manager.areNotificationsEnabled()) return false;

    NotificationChannel channel = opener == null ? null
            : manager.getNotificationChannel(channelId, shortcutId(opener));
    if (channel == null) channel = manager.getNotificationChannel(channelId);
    if (channel == null || channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
    if (channel.getGroup() != null) {
      NotificationChannelGroup group = manager.getNotificationChannelGroup(channel.getGroup());
      if (group != null && group.isBlocked()) return false;
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      // R exposes only the app's "all conversations" preference and keeps the device switch in Global.
      return Settings.Global.getInt(context.getContentResolver(), "notification_bubbles", 1) == 1
              && manager.areBubblesAllowed();
    }
    if (!manager.areBubblesEnabled()) return false;
    int preference = manager.getBubblePreference();
    // In ALL mode the framework accepts a default channel. canBubble() only reports explicit opt-in.
    return preference == NotificationManager.BUBBLE_PREFERENCE_ALL
            || (preference == NotificationManager.BUBBLE_PREFERENCE_SELECTED && channel.canBubble());
  }
}
