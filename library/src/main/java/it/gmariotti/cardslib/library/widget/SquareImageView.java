package it.gmariotti.cardslib.library.widget;

import android.widget.ImageView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;

public class SquareImageView extends ImageView {
    private Drawable mDrawableOld;

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

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();

        if ((mDrawableOld != drawable) && drawable instanceof BitmapDrawable &&
                (((BitmapDrawable)drawable).getBitmap() != null)) {
            mDrawableOld = drawable;

            AlphaAnimation animation = new AlphaAnimation(0, 1);
            animation.setDuration(800);
            this.startAnimation(animation);
        } else {
            super.onDraw(canvas);
        }
    }
}
