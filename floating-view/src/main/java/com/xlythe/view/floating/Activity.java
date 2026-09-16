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

/**
 * Asks for whatever permission the floating view needs, then reports back once through
 * {@link #onSuccess()} or {@link #onFailure()}.
 *
 * <p>The answer is read from the permission itself when the user comes back, rather than from the
 * result of the screen that asked. Settings screens are opened into a task of their own, and a
 * caller in a task of its own - a singleInstance activity, say - is told the request was cancelled
 * before the user has so much as seen the screen. Waiting to look until the user returns is right
 * whichever way the activity was launched.
 */
abstract class Activity extends android.app.Activity {
  private static final String KEY_ASKED = "com.xlythe.view.floating.ASKED";
  private static final String KEY_SHOWN = "com.xlythe.view.floating.SHOWN";
  private static final String KEY_ANSWERED = "com.xlythe.view.floating.ANSWERED";

  private static final int REQUEST_CODE_WINDOW_OVERLAY_PERMISSION = 10001;
  private static final int REQUEST_CODE_BUBBLES_PERMISSION = 10002;
  private static final int REQUEST_CODE_POST_NOTIFICATION_PERMISSION = 10003;

  /** Something has been asked for and no answer has been read yet. */
  private boolean mAsked;

  /** The screen doing the asking has been in front of us, so coming back means an answer. */
  private boolean mShown;

  /** {@link #onSuccess()} or {@link #onFailure()} has run. Neither runs twice. */
  private boolean mAnswered;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);

    if (state != null) {
      mAsked = state.getBoolean(KEY_ASKED);
      mShown = state.getBoolean(KEY_SHOWN);
      mAnswered = state.getBoolean(KEY_ANSWERED);
    }

    if (hasPermissions()) {
      succeed();
    } else if (!mAsked) {
      // Asking again on every recreation would stack up a second settings screen behind the first.
      askForPermissions();
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle state) {
    super.onSaveInstanceState(state);
    state.putBoolean(KEY_ASKED, mAsked);
    state.putBoolean(KEY_SHOWN, mShown);
    state.putBoolean(KEY_ANSWERED, mAnswered);
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Pausing for a configuration change is this activity going away and coming straight back, not
    // the user going to the settings screen and coming back from it with an answer.
    if (mAsked && !isChangingConfigurations()) {
      mShown = true;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mAsked && mShown && !mAnswered) {
      if (hasPermissions()) {
        succeed();
      } else {
        fail();
      }
    }
  }

  private void succeed() {
    if (!mAnswered) {
      mAnswered = true;
      onSuccess();
    }
  }

  private void fail() {
    if (!mAnswered) {
      mAnswered = true;
      onFailure();
    }
  }

  @SuppressLint("NewApi")
  private boolean hasPermissions() {
    // From M~Q, we use window overlays to draw the floating view. From R+ we use bubbles.
    return Build.VERSION.SDK_INT >= Bubbles.MIN_SDK_BUBBLES
            ? hasBubblePermissions() : hasOverlayPermissions();
  }

  @SuppressLint("NewApi")
  private void askForPermissions() {
    // Each ask starts the wait over, since granting one permission can lead straight to asking for
    // the next and the screen for that one has not been shown yet.
    mAsked = true;
    mShown = false;
    if (Build.VERSION.SDK_INT >= Bubbles.MIN_SDK_BUBBLES) {
      requestBubblePermissions();
    } else {
      requestOverlayPermissions();
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
    startActivityForResult(
            new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())),
            REQUEST_CODE_WINDOW_OVERLAY_PERMISSION);
  }

  @SuppressLint("NewApi")
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CODE_POST_NOTIFICATION_PERMISSION) {
      // These arrive empty when the request was interrupted rather than answered, so there is not
      // necessarily a result to read.
      boolean granted = grantResults.length > 0
              && grantResults[0] == PackageManager.PERMISSION_GRANTED;
      if (!granted) {
        fail();
      } else if (hasBubblePermissions()) {
        succeed();
      } else {
        // Notifications were the first of two. Ask for the other, and start the wait over.
        askForPermissions();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != REQUEST_CODE_WINDOW_OVERLAY_PERMISSION
            && requestCode != REQUEST_CODE_BUBBLES_PERMISSION) {
      super.onActivityResult(requestCode, resultCode, data);
    }
    // Nothing for our own requests: what a settings screen returns says nothing about what the
    // user did there, and onResume reads the permission once they are back.
  }

  private boolean canDrawOverlays() {
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
