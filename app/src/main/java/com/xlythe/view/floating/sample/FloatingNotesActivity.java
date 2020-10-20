package com.xlythe.view.floating.sample;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.xlythe.view.floating.FloatingActivity;

public class FloatingNotesActivity extends FloatingActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.floating_notes);
    }
}
