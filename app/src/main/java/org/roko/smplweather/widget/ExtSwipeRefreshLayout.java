package org.roko.smplweather.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ExtSwipeRefreshLayout extends SwipeRefreshLayout {

    public ExtSwipeRefreshLayout(@NonNull Context context) {
        super(context);
    }

    public ExtSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isEnabled() && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return isEnabled() && super.onTouchEvent(ev);
    }
}
