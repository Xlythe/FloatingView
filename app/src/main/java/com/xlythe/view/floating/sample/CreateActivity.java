package com.xlythe.view.floating.sample;

import android.content.Intent;

import com.xlythe.view.floating.CreateShortcutActivity;

import androidx.annotation.DrawableRes;

/**
 * Creates the shortcut icon
 */
public class CreateActivity extends CreateShortcutActivity {
    @Override
    public CharSequence getShortcutName() {
        return getString(R.string.app_name);
    }

    @DrawableRes
    @Override
    public int getShortcutIcon() {
        return R.mipmap.ic_launcher;
    }

    @Override
    public Intent getOpenShortcutActivityIntent() {
        return new Intent(this, OpenActivity.class);
    }
}
