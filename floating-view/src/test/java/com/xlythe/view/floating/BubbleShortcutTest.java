package com.xlythe.view.floating;

import android.R;
import android.app.Notification;
import android.app.PendingIntent;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk = 30)
public class BubbleShortcutTest {
    public static class Opener extends OpenShortcutActivity {
        @Override protected Intent createActivityIntent() { return new Intent(this, FloatingActivity.class); }
        @Override protected Intent createServiceIntent() { throw new AssertionError(); }
        @Override protected Notification createNotification() {
            getSystemService(NotificationManager.class).createNotificationChannel(
                    new NotificationChannel("windows", "Windows", NotificationManager.IMPORTANCE_DEFAULT));
            return new Notification.Builder(this, "windows").setSmallIcon(R.drawable.ic_menu_view).build();
        }
    }

    public static class Creator extends CreateShortcutActivity {
        @Override public int getShortcutIcon() { return R.drawable.ic_menu_view; }
        @Override public CharSequence getShortcutName() { return "Window"; }
        @Override public Intent getOpenShortcutActivityIntent() { return new Intent(this, Opener.class); }
        @Override protected NotificationChannel createNotificationChannel() { throw new AssertionError(); }
    }

    @Test public void shortcutCreatorChecksTheSameConversationAsTheOpener() {
        Creator creator = Robolectric.buildActivity(Creator.class).get();
        Opener opener = Robolectric.buildActivity(Opener.class).get();
        assertEquals(opener.getShortcutOpener(), creator.getShortcutOpener());
    }

    @Test public void bubbleShortcutOpensContentAndSurvivesTheOpener() {
        Opener opener = Robolectric.buildActivity(Opener.class).get();
        opener.onSuccess();
        ShortcutInfo shortcut = opener.getSystemService(ShortcutManager.class).getDynamicShortcuts().get(0);
        Notification notification = opener.getSystemService(NotificationManager.class).getActiveNotifications()[0].getNotification();
        assertEquals(shortcut.getId(), notification.getBubbleMetadata().getShortcutId());
        assertEquals(FloatingActivity.class.getName(), shortcut.getIntent().getComponent().getClassName());
        assertTrue((shadowOf(notification.contentIntent).getFlags() & PendingIntent.FLAG_IMMUTABLE) != 0);
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
