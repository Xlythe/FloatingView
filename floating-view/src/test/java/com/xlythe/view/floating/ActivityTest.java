package com.xlythe.view.floating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.Activity;
import android.content.pm.PackageManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSettings;

/**
 * The permission dance. Run below the bubble cut-off, where the permission is the overlay one and a
 * test can say whether it has been granted.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class ActivityTest {
    /** Counts what the library tells it, so a second telling is visible. */
    public static class Subject extends com.xlythe.view.floating.Activity {
        int succeeded;
        int failed;

        @Override
        String ensureNotificationChannel() {
            return "floating";
        }

        @Override
        void onSuccess() {
            succeeded++;
        }

        @Override
        void onFailure() {
            failed++;
        }
    }

    private ActivityController<Subject> launch() {
        return Robolectric.buildActivity(Subject.class).setup();
    }

    @Test
    public void withThePermissionAlreadyGranted_itGoesStraightThrough() {
        ShadowSettings.setCanDrawOverlays(true);

        Subject subject = launch().get();

        assertEquals(1, subject.succeeded);
        assertEquals(0, subject.failed);
    }

    /**
     * The settings screen opens in a task of its own, and a caller in a task of its own is told the
     * request was cancelled before the user has even seen it. Reporting failure then is what made
     * the floating calculator need two taps: grant the permission, watch nothing happen, tap again.
     */
    @Test
    public void whileTheUserIsStillDeciding_nothingIsReported() {
        ShadowSettings.setCanDrawOverlays(false);

        ActivityController<Subject> controller = launch();

        assertEquals(0, controller.get().succeeded);
        assertEquals("it gave up before the user had answered", 0, controller.get().failed);
    }

    @Test
    public void whenTheUserComesBackHavingGrantedIt_itGoesAhead() {
        ShadowSettings.setCanDrawOverlays(false);
        ActivityController<Subject> controller = launch();

        // Off to the settings screen, and back again having said yes.
        controller.pause().stop();
        ShadowSettings.setCanDrawOverlays(true);
        controller.start().resume();

        assertEquals(1, controller.get().succeeded);
        assertEquals(0, controller.get().failed);
    }

    @Test
    public void whenTheUserComesBackHavingRefused_itGivesUp() {
        ShadowSettings.setCanDrawOverlays(false);
        ActivityController<Subject> controller = launch();

        controller.pause().stop().start().resume();

        assertEquals(0, controller.get().succeeded);
        assertEquals(1, controller.get().failed);
    }

    /** Whatever the settings screen returns says nothing about what the user did there. */
    @Test
    public void aCancelledResultFromTheSettingsScreen_isNotAnAnswer() {
        ShadowSettings.setCanDrawOverlays(false);
        ActivityController<Subject> controller = launch();

        controller.get().onActivityResult(10001, Activity.RESULT_CANCELED, null);

        assertEquals(0, controller.get().failed);
    }

    @Test
    public void comingBackTwice_onlyReportsOnce() {
        ShadowSettings.setCanDrawOverlays(false);
        ActivityController<Subject> controller = launch();

        controller.pause().stop();
        ShadowSettings.setCanDrawOverlays(true);
        controller.start().resume();
        controller.pause().stop().start().resume();

        assertEquals(1, controller.get().succeeded);
    }

    /**
     * These arrays arrive empty when the request was interrupted rather than answered, which reading
     * the first element of turned into a crash on the very first tap.
     */
    @Test
    @Config(sdk = 33)
    public void anInterruptedPermissionRequest_doesNotCrash() {
        Subject subject = Robolectric.buildActivity(Subject.class).setup().get();

        subject.onRequestPermissionsResult(10003, new String[0], new int[0]);

        assertEquals("an unanswered request is not a grant", 1, subject.failed);
    }

    @Test
    @Config(sdk = 33)
    public void aRefusedNotificationPermission_givesUp() {
        Subject subject = Robolectric.buildActivity(Subject.class).setup().get();

        subject.onRequestPermissionsResult(10003,
                new String[] {android.Manifest.permission.POST_NOTIFICATIONS},
                new int[] {PackageManager.PERMISSION_DENIED});

        assertEquals(1, subject.failed);
        assertEquals(0, subject.succeeded);
    }

    /** Recreating the activity must not stack a second settings screen behind the first. */
    @Test
    public void recreating_doesNotAskAgain() {
        ShadowSettings.setCanDrawOverlays(false);
        ActivityController<Subject> controller = launch();

        controller.recreate();

        assertFalse("it asked a second time", controller.get().succeeded > 0);
        assertEquals(0, controller.get().failed);
    }
}
