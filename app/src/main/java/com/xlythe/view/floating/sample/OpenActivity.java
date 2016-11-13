package com.xlythe.view.floating.sample;

import android.content.Intent;

import com.xlythe.view.floating.OpenShortcutActivity;

/**
 * When the shortcut icon is pressed, use this Activity to launch the overlay Service
 */
public class OpenActivity extends OpenShortcutActivity {
    @Override
    public Intent createServiceIntent() {
        return new Intent(this, FloatingNotes.class);
    }
}
