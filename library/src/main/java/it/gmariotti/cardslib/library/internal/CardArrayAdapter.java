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

package it.gmariotti.cardslib.library.internal;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.HashMap;
import java.util.List;

import org.lucasr.twowayview.TwoWayView;

import it.gmariotti.cardslib.library.R;
import it.gmariotti.cardslib.library.internal.base.BaseCardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardView;
import it.gmariotti.cardslib.library.view.listener.SwipeDismissAdapterViewTouchListener;
import it.gmariotti.cardslib.library.view.listener.SwipeDismissListViewTouchListener;
import it.gmariotti.cardslib.library.view.listener.SwipeDismissTwoWayViewTouchListener;
import it.gmariotti.cardslib.library.view.listener.UndoBarController;
import it.gmariotti.cardslib.library.view.listener.UndoCard;

/**
 * Array Adapter for {@link Card} model
 * <p/>
 * Usage:
 * <pre><code>
 * ArrayList<Card> cards = new ArrayList<Card>();
 * for (int i=0;i<1000;i++){
 *     CardExample card = new CardExample(getActivity(),"My title "+i,"Inner text "+i);
 *     cards.add(card);
 * }
 *
 * CardArrayAdapter mCardArrayAdapter = new CardArrayAdapter(getActivity(),cards);
 *
 * CardListView listView = (CardListView) getActivity().findViewById(R.id.listId);
 * listView.setAdapter(mCardArrayAdapter); *
 * </code></pre>
 * It provides a default layout id for each row @layout/list_card_layout
 * Use can easily customize it using card:list_card_layout_resourceID attr in your xml layout:
 * <pre><code>
 *    <it.gmariotti.cardslib.library.view.CardListView
 *      android:layout_width="match_parent"
 *      android:layout_height="match_parent"
 *      android:id="@+id/carddemo_list_gplaycard"
 *      card:list_card_layout_resourceID="@layout/list_card_thumbnail_layout" />
 * </code></pre>
 * or:
 * <pre><code>
 * adapter.setRowLayoutId(list_card_layout_resourceID);
 * </code></pre>
 * </p>
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class CardArrayAdapter extends BaseCardArrayAdapter implements UndoBarController.UndoListener {

    protected static String TAG = "CardArrayAdapter";

    /**
     * {@link AdapterView}
     */
    protected AdapterView<?> mParentView;

    protected CardView.OnExpandListAnimatorListener mExpandListAnimatorListener;

    /**
     * Listener invoked when a card is swiped
     */
    protected SwipeDismissAdapterViewTouchListener mOnTouchListener;

    /**
     * Used to enable an undo message after a swipe action
     */
    protected boolean mEnableUndo=false;

    /**
     * Undo Controller
     */
    protected UndoBarController mUndoBarController;

    /**
     * Internal Map with all Cards.
     * It uses the card id value as key.
     */
    protected HashMap<String /* id */,Card>  mInternalObjects;


    // -------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------

    /**
     * Constructor
     *
     * @param context The current context.
     * @param cards   The cards to represent in the ListView.
     */
    public CardArrayAdapter(Context context, List<Card> cards) {
        super(context, cards);
    }

    // -------------------------------------------------------------
    // Views
    // -------------------------------------------------------------

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        CardView mCardView;
        Card mCard;

        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //Retrieve card from items
        mCard = (Card) getItem(position);
        if (mCard != null) {

            int layout = mRowLayoutId;
            boolean recycle = false;

            //Inflate layout
            if (view == null) {
                recycle = false;
                view = mInflater.inflate(layout, parent, false);
            } else {
                recycle = true;
            }

            //Setup card
            mCardView = (CardView) view.findViewById(R.id.list_cardId);
            if (mCardView != null) {
                //It is important to set recycle value for inner layout elements
                mCardView.setForceReplaceInnerLayout(Card.equalsInnerLayout(mCardView.getCard(),mCard));

                //It is important to set recycle value for performance issue
                mCardView.setRecycle(recycle);

                //Save original swipeable to prevent cardSwipeListener (listView requires another cardSwipeListener)
                boolean origianlSwipeable = mCard.isSwipeable();
                mCard.setSwipeable(false);

                mCardView.setCard(mCard);

                //Set originalValue
                mCard.setSwipeable(origianlSwipeable);

                //If card has an expandable button override animation
                if (mCard.getCardHeader() != null && mCard.getCardHeader().isButtonExpandVisible()) {
                    setupExpandCollapseListAnimation(mCardView);
                }

                //Setup swipeable animation
                setupSwipeableAnimation(mCard, mCardView);

            }
        }

        return view;
    }



    /**
     * Sets SwipeAnimation on List
     *
     * @param card {@link Card}
     * @param cardView {@link CardView}
     */
    protected void setupSwipeableAnimation(final Card card, CardView cardView) {

        if (card.isSwipeable()){
            if (mOnTouchListener == null){
                // Setting this scroll listener is required to ensure that during
                // ListView scrolling, we don't look for swipes.
                if (mParentView instanceof ListView) {
                    mOnTouchListener = new SwipeDismissListViewTouchListener((ListView)mParentView, mCallback);
                    ((ListView)mParentView).setOnScrollListener(mOnTouchListener.makeScrollListener());
                } else if (mParentView instanceof TwoWayView) {
                    mOnTouchListener = new SwipeDismissTwoWayViewTouchListener((TwoWayView)mParentView, mCallback);
                    ((TwoWayView)mParentView).setOnScrollListener(mOnTouchListener.makeScrollListener());
                }
            }

            cardView.setOnTouchListener(mOnTouchListener);

        }else{
            //prevent issue with recycle view
            cardView.setOnTouchListener(null);
        }
    }

    /**
     * Overrides the default collapse/expand animation in a List
     *
     * @param cardView {@link CardView}
     */
    protected void setupExpandCollapseListAnimation(CardView cardView) {

        if (cardView == null) return;
        cardView.setOnExpandListAnimatorListener(mExpandListAnimatorListener);
    }

    // -------------------------------------------------------------
    //  SwipeListener and undo action
    // -------------------------------------------------------------
    /**
     * Listener invoked when a card is swiped
     */
    SwipeDismissListViewTouchListener.DismissCallbacks mCallback = new SwipeDismissAdapterViewTouchListener.DismissCallbacks() {

        @Override
        public boolean canDismiss(int position, Card card) {
            return card.isSwipeable();
        }

        @Override
        public void onDismiss(View view, int[] reverseSortedPositions) {

            int[] itemPositions=new int[reverseSortedPositions.length];
            String[] itemIds=new String[reverseSortedPositions.length];
            int i=0;

            //Remove cards and notifyDataSetChanged
            for (int position : reverseSortedPositions) {
                Card card = getItem(position);
                itemPositions[i]=position;
                itemIds[i]=card.getId();
                i++;

                remove(card);
                if (card.getOnSwipeListener() != null){
                        card.getOnSwipeListener().onSwipe(card);
                }
            }
            notifyDataSetChanged();

            //Check for a undo message to confirm
            if (isEnableUndo() && mUndoBarController!=null){

                //Show UndoBar
                UndoCard itemUndo=new UndoCard(itemPositions,itemIds);

                if (getContext()!=null){
                    Resources res = getContext().getResources();
                    if (res!=null){
                        String messageUndoBar = res.getQuantityString(R.plurals.list_card_undo_items, reverseSortedPositions.length, reverseSortedPositions.length);

                        mUndoBarController.showUndoBar(false, messageUndoBar, itemUndo, CardArrayAdapter.this);
                    }
                }

            }
        }
    };

    // -------------------------------------------------------------
    //  Undo Default Listener
    // -------------------------------------------------------------

    @Override
    public void onUndo(Parcelable token, boolean timeout) {
        //Restore items in lists (use reverseSortedOrder)
        if (token != null) {

            UndoCard item = (UndoCard) token;
            int[] itemPositions = item.itemPosition;
            String[] itemIds = item.itemId;

            if (itemPositions != null) {
                int end = itemPositions.length;

                for (int i = end - 1; i >= 0; i--) {
                    int itemPosition = itemPositions[i];
                    String id= itemIds[i];

                    if (id==null){
                        Log.w(TAG, "You have to set a id value to use the undo action");
                    }else{
                        Card card = mInternalObjects.get(id);
                        if (card!=null){
                            if (!timeout) {
                                insert(card, itemPosition);
                                notifyDataSetChanged();
                            }
                            if (card.getOnUndoSwipeListListener()!=null)
                                card.getOnUndoSwipeListListener().onUndoSwipe(card, timeout);
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    //  Getters and Setters
    // -------------------------------------------------------------

    /**
     * @return {@link AdapterView}
     */
    public AdapterView<?> getParentView() {
        return mParentView;
    }

    /**
     * Sets the {@link AdapterView}
     *
     * @param AdapterView view
     */
    public void setParentView(AdapterView<?> parent) {
        this.mParentView = parent;
    }

    /**
     * Sets the {@link CardView.OnExpandListAnimatorListener}
     *
     * @param CardView.OnExpandListAnimatorListener listener
     */
    public void setExpandListAnimatorListener(CardView.OnExpandListAnimatorListener listener) {
        this.mExpandListAnimatorListener = listener;
    }

    /**
     * Indicates if the undo message is enabled after a swipe action
     *
     * @return <code>true</code> if the undo message is enabled
     */
    public boolean isEnableUndo() {
        return mEnableUndo;
    }

    /**
     * Enables an undo message after a swipe action
     *
     * @param enableUndo <code>true</code> to enable an undo message
     */
    public void setEnableUndo(boolean enableUndo) {
        mEnableUndo = enableUndo;
        if (enableUndo) {
            mInternalObjects = new HashMap<String, Card>();
            for (int i=0;i<getCount();i++) {
                Card card = getItem(i);
                mInternalObjects.put(card.getId(), card);
            }

            //Create a UndoController
            if (mUndoBarController==null){
                View undobar = ((Activity)mContext).findViewById(R.id.list_card_undobar);
                if (undobar != null) {
                    mUndoBarController = UndoBarController.getInstance(undobar);
                }
            }
        }else{
            mUndoBarController=null;
        }
    }

    /**
     * Return the UndoBarController for undo action
     *
     * @return {@link UndoBarController}
     */
    public UndoBarController getUndoBarController() {
        return mUndoBarController;
    }

    @Override
    public void clear() {
        super.clear();
        mUndoBarController.hideUndoBar(false);
    }
}
