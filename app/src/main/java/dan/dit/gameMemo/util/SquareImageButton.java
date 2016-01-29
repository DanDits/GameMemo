package dan.dit.gameMemo.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Created by daniel on 29.01.16.
 */
public class SquareImageButton extends ImageButton {
    public SquareImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public SquareImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SquareImageButton(Context context) {
        super(context);
    }
    // This is used to make square buttons.
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int min = Math.min(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(min, min);
    }
}
