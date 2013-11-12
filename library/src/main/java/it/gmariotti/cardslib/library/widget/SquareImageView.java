package it.gmariotti.cardslib.library.widget;

import android.widget.ImageView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class SquareImageView extends ImageView {
    public SquareImageView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public SquareImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int min = Integer.MAX_VALUE;

        if (getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT) {
            if (getMeasuredWidth() != 0)
                min = Math.min(min, getMeasuredWidth());
        }

        if (getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT) {
            if (getMeasuredHeight() != 0)
                min = Math.max(min, getMeasuredHeight());
        }

        if (min == Integer.MAX_VALUE)
            return;

        setMeasuredDimension(min, min);
    }
}
