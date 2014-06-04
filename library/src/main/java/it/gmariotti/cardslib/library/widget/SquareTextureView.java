package it.gmariotti.cardslib.library.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.ViewGroup;

public class SquareTextureView extends TextureView {
    public SquareTextureView(Context context) {
        super(context);
    }

    public SquareTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
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

