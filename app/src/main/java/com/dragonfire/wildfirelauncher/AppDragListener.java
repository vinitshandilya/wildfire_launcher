package com.dragonfire.wildfirelauncher;

import android.view.MotionEvent;
import android.view.View;

public interface AppDragListener {
    void onAppDragged(AppObject appObject, View clickedView, MotionEvent event);
}
