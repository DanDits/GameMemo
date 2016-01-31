package dan.dit.gameMemo.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import dan.dit.gameMemo.R;

/**
 * Created by daniel on 30.01.16.
 */
public class SlidingNumberPicker extends TextView {
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int DEFAULT_DELTA = 10;
    private int mMin = DEFAULT_MIN_VALUE;
    private int mMax = DEFAULT_MAX_VALUE;
    private int mValue = Integer.MIN_VALUE;
    private int mDelta = DEFAULT_DELTA;
    private float mSlideStartY;
    private int mSlideStartValue;
    private float mRequiredHeightFractionForFullDelta = 0.5f;
    private boolean mEnforceDelta = true;
    private OnValueChangedListener mListener;
    private boolean mHideArrows;
    private CharSequence mMinText;
    private CharSequence mMaxText;

    public SlidingNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setValue(DEFAULT_MIN_VALUE);
        applyAttributes(context, attrs);
        initSlidingListener();
    }
    public SlidingNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setValue(DEFAULT_MIN_VALUE);
        applyAttributes(context, attrs);
        initSlidingListener();
    }
    public SlidingNumberPicker(Context context) {
        super(context);
        setValue(DEFAULT_MIN_VALUE);
        applyAttributes(context, null);
        initSlidingListener();
    }

    public void setMinText(CharSequence text) {
        mMinText = text;
    }

    public void setMaxText(CharSequence text) {
        mMaxText = text;
    }

    public void setHideArrows(boolean hide) {
        mHideArrows = hide;
        if (hide) {
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        } else {
            updateIndicator();
        }
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        mListener = listener;
    }

    public void addDelta() {
        setValue(mValue + mDelta);
    }

    public int getMinValue() {
        return mMin;
    }

    public int getMaxValue() {
        return mMax;
    }

    public interface OnValueChangedListener {
        void onValueChanged(SlidingNumberPicker view, int newValue);
    }

    private void applyAttributes(Context context, AttributeSet attrs) {
        int gravity = Gravity.CENTER_VERTICAL;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.SlidingNumberPicker,
                    0, 0);
            try {
                int min = a.getInteger(R.styleable.SlidingNumberPicker_minValue,
                        DEFAULT_MIN_VALUE);
                int max = a.getInteger(R.styleable.SlidingNumberPicker_maxValue,
                        DEFAULT_MAX_VALUE);
                int delta = a.getInteger(R.styleable.SlidingNumberPicker_deltaValue,
                        DEFAULT_DELTA);
                setBounds(min, max, delta);
                gravity = a.getInteger(R.styleable.SlidingNumberPicker_android_gravity, gravity);
                mMinText = a.getString(R.styleable.SlidingNumberPicker_minText);
                mMaxText = a.getString(R.styleable.SlidingNumberPicker_maxText);
            } finally {
                a.recycle();
            }
        }
        setGravity(gravity);
    }

    private void initSlidingListener() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                onSlide(event);
                return true;
            }
        });
    }

    private void notifyListener() {
        OnValueChangedListener listener = mListener;
        if (listener != null) {
            listener.onValueChanged(this, mValue);
        }
    }

    private void startSlide(float y) {
        mSlideStartY = y;
        mSlideStartValue = mValue;
    }

    private void onSlide(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startSlide(event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                float heightDelta = mSlideStartY - event.getY(); //positive if moving up
                int valueDelta = (int) (mDelta * heightDelta / getHeight()
                        / mRequiredHeightFractionForFullDelta);
                if (mEnforceDelta && Math.abs(mDelta) > 0) {
                    valueDelta = valueDelta -
                            (int) (Math.signum(valueDelta) * (Math.abs(valueDelta) % Math.abs(mDelta)));
                }
                int newValue = mSlideStartValue + valueDelta;
                if ((newValue > mMax && mValue == mMax)
                        || (newValue < mMin && mValue == mMin)) {
                    // when already at bound we do not need to set new value
                    // instead to help for views positioned at edge of display restart the slide
                    // start position to more easily change max/min value
                    startSlide(event.getY());
                } else {
                    setValue(newValue);
                }
                break;
        }
    }


    public void setBounds(int min, int max, int delta) {
        mMin = min;
        mMax = Math.max(mMin, max); // ensure min<=max
        if (mMax - mMin == 0) {
            mDelta = 0;
        } else {
            mDelta = (int) (Math.signum(delta) * Math.min(Math.abs(delta), mMax - mMin));
        }
        setValue(mValue);
    }

    public void setValue(int value) {
        int oldValue = mValue;
        mValue = Math.max(mMin, Math.min(mMax, value));
        if (oldValue != mValue) {
            CharSequence text;
            if (mValue == mMin && mMinText != null) {
                text = mMinText;
            } else if (mValue == mMax && mMaxText != null) {
                text = mMaxText;
            } else {
                text = String.valueOf(mValue);
            }
            setText(text);
            updateIndicator();
            notifyListener();
        }
    }

    private void updateIndicator() {
        if (mHideArrows) {
            return;
        }
        int indicatorResId = R.drawable.sliding_number_picker_arrow_up_down;
        if (mValue == mMin) {
            indicatorResId = R.drawable.sliding_number_picker_arrow_up;
        } else if (mValue == mMax) {
            indicatorResId = R.drawable.sliding_number_picker_arrow_down;
        }
        if (mMin == mMax) {
            indicatorResId = 0;
        }
        setCompoundDrawablesWithIntrinsicBounds(indicatorResId, 0, 0, 0);
    }

    public void setRequiredHeightFractionForFullDelta(float fraction) {
        mRequiredHeightFractionForFullDelta = Math.max(0.01f, fraction);
    }

    public void setDoNotEnforceDelta(boolean doNotEnforce) {
        mEnforceDelta = !doNotEnforce;
    }
}
