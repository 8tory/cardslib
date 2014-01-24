package it.gmariotti.cardslib.library.view.listener;

/* Copyright 2013 Roman Nurik, Gabriele Mariotti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lucasr.twowayview.TwoWayView;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * It is based on Roman Nurik code.
 * See this link for original code https://github.com/romannurik/Android-SwipeToDismiss
 * </p>
 * It provides a SwipeDismissViewTouchListener for a CardList.
 * </p>
 *
 * A {@link View.OnTouchListener} that makes the list items in a {@link ListView}
 * dismissable. {@link ListView} is given special treatment because by default it handles touches
 * for its list items... i.e. it's in charge of drawing the pressed state (the list selector),
 * handling list item clicks, etc.
 *
 * <p>After creating the listener, the caller should also call
 * {@link ListView#setOnScrollListener(AbsListView.OnScrollListener)}, passing
 * in the scroll listener returned by {@link #makeScrollListener()}. If a scroll listener is
 * already assigned, the caller should still pass scroll changes through to this listener. This will
 * ensure that this {@link SwipeDismissTwoWayViewTouchListener} is paused during list view
 * scrolling.</p>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * SwipeDismissListViewTouchListener touchListener =
 *         new SwipeDismissListViewTouchListener(
 *                 listView,
 *                 new SwipeDismissListViewTouchListener.OnDismissCallback() {
 *                     public void onDismiss(ListView listView, int[] reverseSortedPositions) {
 *                         for (int position : reverseSortedPositions) {
 *                             adapter.remove(adapter.getItem(position));
 *                         }
 *                         adapter.notifyDataSetChanged();
 *                     }
 *                 });
 * listView.setOnTouchListener(touchListener);
 * listView.setOnScrollListener(touchListener.makeScrollListener());
 * </pre>
 *
 * <p>This class Requires API level 12 or later due to use of {@link
 * ViewPropertyAnimator}.</p>
 *
 */
public class SwipeDismissTwoWayViewTouchListener implements SwipeDismissAdapterViewTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private TwoWayView mListView;
    private DismissCallbacks mCallbacks;
    private int mViewHeight = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private List<PendingDismissData> mPendingDismisses = new ArrayList<PendingDismissData>();
    private int mDismissAnimationRefCount = 0;
    private float mDownY;
    private boolean mSwiping;
    private boolean mItemPressed;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private View mDownView;
    private boolean mPaused;

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given list view.
     *
     * @param listView  The list view whose items should be dismissable.
     * @param callbacks The callback to trigger when the user has indicated that she would like to
     *                  dismiss one or more list items.
     */
    public SwipeDismissTwoWayViewTouchListener(TwoWayView listView, DismissCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = listView.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mListView = listView;
        mCallbacks = callbacks;
    }

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    @Override
    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    /**
     * Returns an {@link AbsListView.OnScrollListener} to be added to the {@link
     * ListView} using {@link ListView#setOnScrollListener(AbsListView.OnScrollListener)}.
     * If a scroll listener is already assigned, the caller should still pass scroll changes through
     * to this listener. This will ensure that this {@link SwipeDismissTwoWayViewTouchListener} is
     * paused during list view scrolling.</p>
     *
     * @see SwipeDismissTwoWayViewTouchListener
     */
    public SwipeDismissAdapterViewTouchListener.OnScrollListener makeScrollListener() {
        return new SwipeDismissAdapterViewTouchListener.OnScrollListener() {
            @Override
            public void onScrollStateChanged(TwoWayView twoWayView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            }

            @Override
            public void onScroll(TwoWayView twoWayView, int i, int i1, int i2) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
        };
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mViewHeight < 2) {
            mViewHeight = mListView.getHeight();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    return false;
                }

                // TODO: ensure this is a finger, and set a flag

                // Find the child view that was touched (perform a hit test)
                int index = mListView.indexOfChild(view);
                if (index != -1) {
                    mDownView = view;
                }

                if (mDownView != null) {

                    mDownY = motionEvent.getRawY();
                    mDownPosition = mListView.getPositionForView(mDownView);
                    if (mCallbacks.canDismiss(mDownPosition,(Card) mListView.getAdapter().getItem(mDownPosition))) {
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(motionEvent);
                        mItemPressed = true;
                    } else {
                        mDownView = null;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaY = motionEvent.getRawY() - mDownY;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityY = mVelocityTracker.getYVelocity();
                float absVelocityY = Math.abs(velocityY);
                float absVelocityX = Math.abs(mVelocityTracker.getXVelocity());
                boolean dismiss = false;
                boolean dismissBottom = false;
                if (Math.abs(deltaY) > mViewHeight / 2) {
                    dismiss = true;
                    dismissBottom = deltaY > 0;
                } else if (mMinFlingVelocity <= absVelocityY && absVelocityY <= mMaxFlingVelocity
                        && absVelocityX < absVelocityY) {
                    // dismiss only if flinging in the same direction as dragging
                    dismiss = (velocityY < 0) == (deltaY < 0);
                    dismissBottom = mVelocityTracker.getYVelocity() > 0;
                }
                if (dismiss) {
                    // dismiss
                    swipe(mDownView, mDownPosition, dismissBottom);
                } else {
                    // cancel
                    mDownView.animate()
                            .translationY(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownY = 0;
                mDownView = null;
                mDownPosition = ListView.INVALID_POSITION;

                if (mSwiping) {
                    if (mItemPressed) {
                        cancelItemPressed(motionEvent);
                        mItemPressed = false;
                    }

                    mListView.requestDisallowInterceptTouchEvent(false);
                    mSwiping = false;
                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null || mPaused) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaY = motionEvent.getRawY() - mDownY;
                if (!mSwiping && (Math.abs(deltaY) > mSlop)) {
                    mSwiping = true;
                }

                if (mSwiping) {
                    mListView.requestDisallowInterceptTouchEvent(true);

                    if (mItemPressed) {
                        cancelItemPressed(motionEvent);
                        mItemPressed = false;
                    }

                    mDownView.setTranslationY(deltaY);
                    mDownView.setAlpha(Math.max(0f, Math.min(1f,
                            1f - 2f * Math.abs(deltaY) / mViewHeight)));
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private void cancelItemPressed(MotionEvent motionEvent) {
        // Cancel ListView's touch (un-highlighting the item)
        MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
        cancelEvent.setAction(MotionEvent.ACTION_CANCEL | (motionEvent
                    .getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
        mListView.onTouchEvent(cancelEvent);
        cancelEvent.recycle();
    }

    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }
    }

    @Override
    public void swipe(View dismissView) {
        if (mViewHeight < 2) {
            mViewHeight = mListView.getHeight();
        }

        int dismissPosition = mListView.getPositionForView(dismissView);
        if (dismissPosition == -1) {
            return;
        }

        if (!mCallbacks.canDismiss(dismissPosition, (Card) mListView.getAdapter()
                .getItem(mDownPosition))) {
            return;
        }

        swipe(dismissView, dismissPosition, false);
    }

    private void swipe(final View dismissView, final int dismissPosition, boolean dismissBottom) {
        ++mDismissAnimationRefCount;
        dismissView.animate().translationY(dismissBottom ? mViewHeight : -mViewHeight)
                .alpha(0).setDuration(mAnimationTime).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                performDismiss(dismissView, dismissPosition);
            }
        });
    }

    private void performDismiss(final View dismissView, final int dismissPosition) {
        // Animate the dismissed list item to zero-width and fire the dismiss callback when
        // all dismissed list item animations have completed. This triggers layout on each animation
        // frame; in the future we may want to do something smarter and more performant.

        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalWidth = dismissView.getWidth();

        ValueAnimator animator = ValueAnimator.ofInt(originalWidth, 1).setDuration(mAnimationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                --mDismissAnimationRefCount;
                if (mDismissAnimationRefCount == 0) {
                    // No active animations, process all pending dismisses.
                    // Sort by descending position
                    Collections.sort(mPendingDismisses);

                    int[] dismissPositions = new int[mPendingDismisses.size()];
                    for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
                        dismissPositions[i] = mPendingDismisses.get(i).position;
                    }
                    mCallbacks.onDismiss(mListView, dismissPositions);

                    ViewGroup.LayoutParams lp;
                    for (PendingDismissData pendingDismiss : mPendingDismisses) {
                        // Reset view presentation
                        pendingDismiss.view.setAlpha(1f);
                        pendingDismiss.view.setTranslationY(0);
                        lp = pendingDismiss.view.getLayoutParams();
                        lp.width = originalWidth;
                        pendingDismiss.view.setLayoutParams(lp);
                    }

                    mPendingDismisses.clear();
                }
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.width = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
        animator.start();
    }
}
