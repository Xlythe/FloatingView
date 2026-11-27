package com.xlythe.view.floating;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 37)
public class BubbleShortcutTest {
    public static class Opener extends OpenShortcutActivity {
        @Override protected Intent createActivityIntent() { return new Intent(this, FloatingActivity.class); }
        @Override protected Intent createServiceIntent() { throw new AssertionError(); }
        @Override protected Notification createNotification() {
            getSystemService(NotificationManager.class).createNotificationChannel(
                    new NotificationChannel("windows", "Windows", NotificationManager.IMPORTANCE_DEFAULT));
            return new Notification.Builder(this, "windows").setSmallIcon(android.R.drawable.ic_menu_view).build();
        }
    }

    @Test public void bubbleShortcutOpensContentAndSurvivesTheOpener() {
        Opener opener = Robolectric.buildActivity(Opener.class).get();
        opener.onSuccess();
        ShortcutInfo shortcut = opener.getSystemService(ShortcutManager.class).getDynamicShortcuts().get(0);
        Notification notification = opener.getSystemService(NotificationManager.class).getActiveNotifications()[0].getNotification();
        assertEquals(shortcut.getId(), notification.getBubbleMetadata().getShortcutId());
        assertEquals(FloatingActivity.class.getName(), shortcut.getIntent().getComponent().getClassName());
        assertTrue(notification.contentIntent.isImmutable());
        assertTrue(opener.isFinishing());
    }

    @Test public void missingActivityFallsBackToApplicationLabel() {
        Opener opener = Robolectric.buildActivity(Opener.class).get();
        assertEquals(opener.getApplicationInfo().loadLabel(opener.getPackageManager()).toString(),
                opener.getActivityName(new Intent("missing.activity")));
    }

    @Test public void drawableWithoutIntrinsicSizeUsesAndPreservesBounds() {
        ColorDrawable icon = new ColorDrawable(Color.RED);
        Rect bounds = new Rect(3, 5, 27, 21);
        icon.setBounds(bounds);
        Bitmap result = OpenShortcutActivity.toBitmap(icon);
        assertEquals(24, result.getWidth());
        assertEquals(16, result.getHeight());
        assertEquals(bounds, icon.getBounds());
    }

    @Test public void unsizedDrawableProducesValidBitmap() {
        assertEquals(1, OpenShortcutActivity.toBitmap(new ColorDrawable(Color.BLUE)).getWidth());
    }
}
