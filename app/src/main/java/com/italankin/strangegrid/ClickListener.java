package com.italankin.strangegrid;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class ClickListener extends RecyclerView.SimpleOnItemTouchListener {

    private final GestureDetector gestureDetector;
    private final OnItemClickListener itemClickListener;

    public ClickListener(Context context, @NonNull OnItemClickListener listener) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
        itemClickListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        View view = rv.findChildViewUnder(e.getX(), e.getY());
        if (view != null && gestureDetector.onTouchEvent(e)) {
            int pos = rv.getChildAdapterPosition(view);
            if (pos != RecyclerView.NO_POSITION) {
                itemClickListener.onItemClick(rv, view, pos);
            }
        }
        return false;
    }

    public interface OnItemClickListener {
        void onItemClick(RecyclerView recyclerView, View view, int position);
    }

}
