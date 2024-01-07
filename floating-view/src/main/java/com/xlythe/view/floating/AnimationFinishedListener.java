package com.xlythe.view.floating;

import android.animation.Animator;

import androidx.annotation.NonNull;

public abstract class AnimationFinishedListener implements Animator.AnimatorListener {
    @Override
    public void onAnimationCancel(@NonNull Animator animation) {
        onAnimationFinished();
    }

    @Override
    public void onAnimationRepeat(@NonNull Animator animation) {
    }

    @Override
    public void onAnimationStart(@NonNull Animator animation) {
    }

    @Override
    public void onAnimationEnd(@NonNull Animator animation) {
        onAnimationFinished();
    }

    public abstract void onAnimationFinished();
}
