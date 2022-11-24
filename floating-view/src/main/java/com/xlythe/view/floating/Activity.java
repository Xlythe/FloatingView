package com.xlythe.view.floating;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

abstract class Activity extends android.app.Activity {
  private static final int REQUEST_CODE_WINDOW_OVERLAY_PERMISSION = 10001;
  private static final int REQUEST_CODE_BUBBLES_PERMISSION = 10002;
  private static final int REQUEST_CODE_POST_NOTIFICATION_PERMISSION = 10003;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);

    // From M~Q, we use window overlays to draw the floating view. From R+ we use bubbles.
    if (Build.VERSION.SDK_INT >= Bubbles.MIN_SDK_BUBBLES) {
      if (hasBubblePermissions()) {
        onSuccess();
      } else {
        requestBubblePermissions();
      }
    } else {
      if (hasOverlayPermissions()) {
        onSuccess();
      } else {
        requestOverlayPermissions();
      }
    }
  }

  @RequiresApi(Bubbles.MIN_SDK_BUBBLES)
  private boolean hasBubblePermissions() {
    if (Build.VERSION.SDK_INT >= 33 && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
      return false;
    }

    return Bubbles.canDisplayBubbles(this, ensureNotificationChannel());
  }

  @RequiresApi(Bubbles.MIN_SDK_BUBBLES)
  private void requestBubblePermissions() {
    if (Build.VERSION.SDK_INT >= 33 && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
      requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, REQUEST_CODE_POST_NOTIFICATION_PERMISSION);
      return;
    }

    // Note that the notification channel must be created before we launch the Bubbles settings activity.
    ensureNotificationChannel();
    startActivityForResult(
            new Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()),
            REQUEST_CODE_BUBBLES_PERMISSION);
  }

  private boolean hasOverlayPermissions() {
    return canDrawOverlays();
  }

  private void requestOverlayPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      startActivityForResult(
              new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())),
              REQUEST_CODE_WINDOW_OVERLAY_PERMISSION);
    }
  }

  @SuppressLint("NewApi")
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CODE_POST_NOTIFICATION_PERMISSION) {
      if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        onFailure();
      } else {
        if (hasBubblePermissions()) {
          onSuccess();
        } else {
          requestBubblePermissions();
        }
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_WINDOW_OVERLAY_PERMISSION) {
      if (hasOverlayPermissions()) {
        onSuccess();
      } else {
        onFailure();
      }
    } else if (requestCode == REQUEST_CODE_BUBBLES_PERMISSION) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (hasBubblePermissions()) {
          onSuccess();
        } else {
          onFailure();
        }
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

  private boolean hasPermission(String permission) {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
  }

  @RequiresApi(Bubbles.MIN_SDK_BUBBLES)
  abstract String ensureNotificationChannel();

  abstract void onSuccess();

  abstract void onFailure();
}
