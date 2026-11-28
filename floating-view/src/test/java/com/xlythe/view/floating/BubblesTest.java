package com.xlythe.view.floating;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowNotificationManager;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk = 31, shadows = BubblesTest.BubbleSettings.class)
public class BubblesTest {
    // Robolectric stores channels but doesn't implement the system's bubble preference APIs.
    @Implements(NotificationManager.class)
    public static class BubbleSettings extends ShadowNotificationManager {
        static boolean enabled;
        static int preference;

        @Implementation(minSdk = 31)
        protected boolean areBubblesEnabled() { return enabled; }

        @Implementation(minSdk = 31)
        protected int getBubblePreference() { return preference; }

        @Implementation(minSdk = 30)
        protected boolean areBubblesAllowed() {
            return preference == NotificationManager.BUBBLE_PREFERENCE_ALL;
        }
    }

    private Context context;
    private NotificationManager manager;
    private ComponentName opener;
    private static final String CHANNEL = "windows";

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        manager = context.getSystemService(NotificationManager.class);
        opener = new ComponentName(context, "example.Window");
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Windows",
                NotificationManager.IMPORTANCE_DEFAULT));
        BubbleSettings.enabled = true;
        BubbleSettings.preference = NotificationManager.BUBBLE_PREFERENCE_ALL;
    }

    @Test
    public void allConversationsAllowsADefaultChannel() {
        assertFalse(manager.getNotificationChannel(CHANNEL).canBubble());
        assertTrue(Bubbles.canDisplayBubbles(context, CHANNEL, opener));
    }

    @Test
    public void selectedOnlyAllowsTheChosenOpener() {
        BubbleSettings.preference = NotificationManager.BUBBLE_PREFERENCE_SELECTED;
        assertFalse(Bubbles.canDisplayBubbles(context, CHANNEL, opener));
        NotificationChannel selected = new NotificationChannel("selected", "Selected",
                NotificationManager.IMPORTANCE_DEFAULT);
        selected.setConversationId(CHANNEL, Bubbles.shortcutId(opener));
        selected.setAllowBubbles(true);
        manager.createNotificationChannel(selected);
        assertTrue(Bubbles.canDisplayBubbles(context, CHANNEL, opener));
        assertFalse(Bubbles.canDisplayBubbles(context, CHANNEL, new ComponentName(context, "example.Other")));
        BubbleSettings.preference = NotificationManager.BUBBLE_PREFERENCE_NONE;
        assertFalse(Bubbles.canDisplayBubbles(context, CHANNEL, opener));
        BubbleSettings.preference = NotificationManager.BUBBLE_PREFERENCE_ALL;
        BubbleSettings.enabled = false;
        assertFalse(Bubbles.canDisplayBubbles(context, CHANNEL, opener));
    }

    @Test
    public void disabledAppNotificationsPreventBubbles() {
        shadowOf(manager).setNotificationsEnabled(false);
        assertFalse(Bubbles.canDisplayBubbles(context, CHANNEL, opener));
    }

    @Test
    public void missingAndBlockedChannelsCannotBubble() {
        assertFalse(Bubbles.canDisplayBubbles(context, "missing", opener));
        manager.createNotificationChannel(new NotificationChannel("blocked", "Blocked",
                NotificationManager.IMPORTANCE_NONE));
        assertFalse(Bubbles.canDisplayBubbles(context, "blocked", opener));
    }

    @Test
    @Config(minSdk = 30, maxSdk = 30)
    public void android11ChecksBothTheGlobalSwitchAndTheAppChoice() {
        Settings.Global.putInt(context.getContentResolver(), "notification_bubbles", 0);
        assertFalse(Bubbles.canDisplayBubbles(context, CHANNEL, opener));
        Settings.Global.putInt(context.getContentResolver(), "notification_bubbles", 1);
        assertTrue(Bubbles.canDisplayBubbles(context, CHANNEL, opener));
        BubbleSettings.preference = NotificationManager.BUBBLE_PREFERENCE_SELECTED;
        assertFalse(Bubbles.canDisplayBubbles(context, CHANNEL, opener));
    }
}
