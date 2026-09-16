package com.xlythe.view.floating;

import static org.junit.Assert.assertEquals;

import android.animation.ValueAnimator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Cancelling an animator delivers a cancel and then an end. Answering both ran the listener's body
 * twice, which in the calculator meant an interrupted panel animation finished itself twice on the
 * way past, flickering the button it hides and leaving the state machine briefly wrong.
 */
@RunWith(RobolectricTestRunner.class)
public class AnimationFinishedListenerTest {
    private int mTimesFinished;

    private final AnimationFinishedListener mListener = new AnimationFinishedListener() {
        @Override
        public void onAnimationFinished() {
            mTimesFinished++;
        }
    };

    @Test
    public void anAnimationThatRunsToTheEnd_finishesOnce() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addListener(mListener);

        mListener.onAnimationStart(animator);
        mListener.onAnimationEnd(animator);

        assertEquals(1, mTimesFinished);
    }

    @Test
    public void anAnimationThatIsCancelled_finishesOnce() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addListener(mListener);

        // What the framework sends on cancel, in this order.
        mListener.onAnimationStart(animator);
        mListener.onAnimationCancel(animator);
        mListener.onAnimationEnd(animator);

        assertEquals("a cancelled animation finished itself twice", 1, mTimesFinished);
    }

    @Test
    public void aRealCancel_finishesOnce() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(1000);
        animator.addListener(mListener);
        animator.start();

        animator.cancel();

        assertEquals(1, mTimesFinished);
    }

    @Test
    public void repeating_doesNotCountAsFinishing() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addListener(mListener);

        mListener.onAnimationStart(animator);
        mListener.onAnimationRepeat(animator);

        assertEquals(0, mTimesFinished);
    }
}
