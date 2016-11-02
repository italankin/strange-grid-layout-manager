package test.stupidgrid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.HashMap;

public class StupidAnimateTouchListener extends RecyclerView.SimpleOnItemTouchListener {

    private static final Interpolator INTERPOLATOR = new DecelerateInterpolator(1);

    private final HashMap<View, AnimHolder> animHolders = new HashMap<>();
    private final int elevation;
    private View current;

    public StupidAnimateTouchListener(Context context) {
        elevation = context.getResources().getDimensionPixelSize(R.dimen.touch_elevation);
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        int action = e.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            View view = rv.findChildViewUnder(e.getX(), e.getY());
            if (view == null) {
                return false;
            }
            AnimHolder animHolder = animHolders.get(view);
            if (animHolder == null) {
                animHolder = new AnimHolder(view, elevation);
                animHolders.put(view, animHolder);
            }
            animHolder.raiseUp();
            current = view;
        } else if (action == MotionEvent.ACTION_MOVE && current != null) {
            AnimHolder animHolder = animHolders.get(current);
            if (animHolder != null) {
                animHolder.lowerDown();
                current = null;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (current != null) {
                AnimHolder animHolder = animHolders.get(current);
                if (animHolder != null) {
                    animHolder.lowerDown();
                    current = null;
                }
            }
        }
        return false;
    }

    private static class AnimHolder {
        private final View view;
        private final ObjectAnimator lift;
        private final ObjectAnimator lower;
        private final int elevation;

        public AnimHolder(View view, int elevation) {
            this.view = view;
            this.elevation = elevation;
            lift = ObjectAnimator.ofFloat(this.view, "elevation", 0, 0);
            lift.setDuration(500);
            lift.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    lower.cancel();
                }
            });
            lift.setInterpolator(INTERPOLATOR);

            lower = ObjectAnimator.ofFloat(this.view, "elevation", 0, 0);
            lower.setDuration(500);
            lower.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    lift.cancel();
                }
            });

            lower.setInterpolator(INTERPOLATOR);
        }

        public void raiseUp() {
            lift.setFloatValues(view.getElevation(), elevation);
            lift.start();
        }

        public void lowerDown() {
            lower.setFloatValues(view.getElevation(), 0);
            lower.start();
        }

    }

}
