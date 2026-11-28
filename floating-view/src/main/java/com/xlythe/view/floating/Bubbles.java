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

  // Q supported experimental bubbles through Developer Options. The shortcut-based
  // BubbleMetadata.Builder used here starts in R, so regular Q builds use overlays.
  static final int MIN_SDK_BUBBLES = Build.VERSION_CODES.R;

  static String shortcutId(ComponentName opener) {
    return "floating.shortcutId." + opener.flattenToShortString();
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  static boolean areBubblesEnabled(Context context, NotificationManager manager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return manager.areBubblesEnabled();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return Settings.Global.getInt(context.getContentResolver(), "notification_bubbles", 1) == 1;
    }
    // Android 10's Developer Options bubble switch used the per-user Secure setting.
    return Settings.Secure.getInt(context.getContentResolver(), "notification_bubbles", 1) == 1;
  }

  @RequiresApi(MIN_SDK_BUBBLES)
  static boolean canDisplayBubbles(Context context, String channelId, ComponentName opener) {
    NotificationManager manager = context.getSystemService(NotificationManager.class);
    if (!manager.areNotificationsEnabled() || !areBubblesEnabled(context, manager)) return false;

    NotificationChannel channel = opener == null ? null
            : manager.getNotificationChannel(channelId, shortcutId(opener));
    if (channel == null) channel = manager.getNotificationChannel(channelId);
    if (channel == null || channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
    if (channel.getGroup() != null) {
      NotificationChannelGroup group = manager.getNotificationChannelGroup(channel.getGroup());
      if (group != null && group.isBlocked()) return false;
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      // R exposes only the app's "all conversations" preference.
      return manager.areBubblesAllowed();
    }
    int preference = manager.getBubblePreference();
    // In ALL mode the framework accepts a default channel. canBubble() only reports explicit opt-in.
    return preference == NotificationManager.BUBBLE_PREFERENCE_ALL
            || (preference == NotificationManager.BUBBLE_PREFERENCE_SELECTED && channel.canBubble());
  }
}
