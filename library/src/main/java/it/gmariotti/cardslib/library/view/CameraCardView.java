/*
 * ******************************************************************************
 *   Copyright (c) 2013 Gabriele Mariotti.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package it.gmariotti.cardslib.library.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.GridView;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.gmariotti.cardslib.library.R;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;

public class CameraCardView extends CardView {
    // -------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------

    public CameraCardView(Context context) {
        //this(context, null);
        super(context);
        mCardThumbnail = new CardThumbnail(getContext());
        mCardThumbnail.enableCamera();
        Card card = new Card(getContext());
        CardHeader header = new CardHeader(getContext());
        header.setTitle("haha");
        card.addCardHeader(header);
        card.addCardThumbnail(mCardThumbnail);
        setCard(card);
    }

    public CameraCardView(Context context, AttributeSet attrs) {
        //this(context, attrs, 0);
        super(context, attrs);
        mCardThumbnail = new CardThumbnail(getContext());
        mCardThumbnail.enableCamera();
        Card card = new Card(getContext());
        CardHeader header = new CardHeader(getContext());
        header.setTitle("haha");
        card.addCardHeader(header);
        card.addCardThumbnail(mCardThumbnail);
        setCard(card);
    }

    public CameraCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mCardThumbnail = new CardThumbnail(getContext());
        mCardThumbnail.enableCamera();
        Card card = new Card(getContext());
        CardHeader header = new CardHeader(getContext());
        header.setTitle("haha");
        card.addCardHeader(header);
        card.addCardThumbnail(mCardThumbnail);
        Log.d("CameraCardView: CameraCardView", "" + mCardThumbnail);
        setCard(card);
    }

    /**
     * Setup the Thumbnail View
     */
    @Override
    protected void setupThumbnailView() {
        Log.d("CameraCardView: setupThumbnailView", "" + mCardThumbnail);
        super.setupThumbnailView();
    }
}
