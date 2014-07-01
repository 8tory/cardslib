package it.gmariotti.cardslib.library.view;

import it.gmariotti.cardslib.library.R;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardArrayRecyclerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class CardRecyclerView extends RecyclerView implements CardView.OnExpandListAnimatorListener {

    public CardRecyclerView(Context context) {
        super(context);
        init(null, 0);
    }

    public CardRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CardRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Initialize
     *
     * @param attrs
     * @param defStyle
     */
    protected void init(AttributeSet attrs, int defStyle) {

        //Init attrs
        initAttrs(attrs,defStyle);

        //Set divider to 0dp
        //setDividerHeight(0);

    }


    /**
     * Init custom attrs.
     *
     * @param attrs
     * @param defStyle
     */
    protected void initAttrs(AttributeSet attrs, int defStyle) {

        list_card_layout_resourceID = R.layout.list_card_layout;

        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.card_options, defStyle, defStyle);

        try {
            list_card_layout_resourceID = a.getResourceId(R.styleable.card_options_list_card_layout_resourceID, this.list_card_layout_resourceID);
        } finally {
            a.recycle();
        }
    }

    //--------------------------------------------------------------------------
    // Adapter
    //--------------------------------------------------------------------------


    /**
     * Set {@link CardArrayAdapter} and layout used by items in TwoWayView
     *
     * @param adapter {@link CardArrayAdapter}
     */
    public void setAdapter(CardArrayAdapter adapter) {
        super.setAdapter(new CardArrayRecyclerAdapter(adapter));

        //Set Layout used by items
        adapter.setRowLayoutId(list_card_layout_resourceID);

        //adapter.setParentView(this); RecyclerView ScrollListener?
        adapter.setExpandListAnimatorListener(this);
        mAdapter = adapter;
    }

    /**
     *  Card Array Adapter
     */
    protected CardArrayAdapter mAdapter;

    /**
     * Default layout to apply to card
     */
    protected int list_card_layout_resourceID = R.layout.list_card_layout;

    /**
     *  CardView.OnExpandListAnimatorListener
     */
    @Override
    public void onExpandStart(CardView viewCard, View expandingLayout) {
    }

    @Override
    public void onCollapseStart(CardView viewCard, View expandingLayout) {
    }
}

