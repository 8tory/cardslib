package it.gmariotti.cardslib.library.view.listener;

/*
 * Copyright 2013 Roman Nurik, Gabriele Mariotti
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
import android.os.Handler;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.TextView;

import it.gmariotti.cardslib.library.R;

/**
 * It is based on Roman Nurik code.
 * See this link for original code:
 * https://code.google.com/p/romannurik-code/source/browse/#git%2Fmisc%2Fundobar
 *
 *
 */
public class UndoBarController {

    private View mBarView;
    private TextView mMessageView;
    private Button mButtonView;
    private ViewPropertyAnimator mBarAnimator;
    private Handler mHideHandler;

    private UndoListener mUndoListener;

    // State objects
    private Parcelable mUndoToken;
    private CharSequence mUndoMessage;

    static private UndoBarController sUndoBarController;
    static private View sBarView;

    /**
     * Interface to listen the undo controller actions
     */
    public interface UndoListener {
        /*
         *  Called when you undo the action
         */
        void onUndo(Parcelable undoToken);
    }

    private UndoBarController(View undoBarView) {
        mHideHandler = new Handler();

        mBarView = undoBarView;
        mBarAnimator = mBarView.animate();

        mMessageView = (TextView) mBarView.findViewById(R.id.list_card_undobar_message);
        hideUndoBar(true);

        mButtonView = (Button)mBarView.findViewById(R.id.list_card_undobar_button);
    }

    public static UndoBarController getInstance(View undoBarView) {
        if ((sUndoBarController == null) || (sBarView != undoBarView)) {
            sBarView = undoBarView;
            sUndoBarController = new UndoBarController(undoBarView);
        }

        return sUndoBarController;
    }

    public void showUndoBar(boolean immediate, CharSequence message, Parcelable undoToken,
            UndoListener undoListener) {

        mUndoToken = undoToken;
        mUndoMessage = message;
        mMessageView.setText(mUndoMessage);

        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable,
                mBarView.getResources().getInteger(R.integer.list_card_undobar_hide_delay));

        mUndoListener = undoListener;
        mButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUndoListener != null) {
                    mUndoListener.onUndo(mUndoToken);
                }
                hideUndoBar(false);
            }
        });

        mBarView.setVisibility(View.VISIBLE);
        if (immediate) {
            mBarView.setAlpha(1);
        } else {
            mBarAnimator.cancel();
            mBarAnimator
                    .alpha(1)
                    .setDuration(
                            mBarView.getResources()
                                    .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);
        }
    }

    public void hideUndoBar(boolean immediate) {
        mHideHandler.removeCallbacks(mHideRunnable);
        if (immediate) {
            mBarView.setVisibility(View.GONE);
            mBarView.setAlpha(0);
            mUndoMessage = null;
            mUndoToken = null;
            mUndoListener = null;
        } else {
            mBarAnimator.cancel();
            mBarAnimator
                    .alpha(0)
                    .setDuration(mBarView.getResources()
                            .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mBarView.setVisibility(View.GONE);
                            mUndoMessage = null;
                            mUndoToken = null;
                            mUndoListener = null;
                        }
                    });
        }
    }

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideUndoBar(false);
        }
    };

    public Parcelable getUndoToken(){
        return mUndoToken;
    }
}
