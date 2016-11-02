package test.stupidgrid;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;

import java.util.Arrays;

public class StupidLayoutManager extends RecyclerView.LayoutManager {

    private LinearSmoothScroller scroller;

    private int childMarginHorizontal = 0;
    private int childMarginVertical = 0;

    private int[] columnCounts = {1};
    private int maxCount = columnCounts[0];
    private SparseIntArray rowsByPositions = new SparseIntArray();
    private SparseIntArray indexInRow = new SparseIntArray();

    private Rect parentRect = new Rect();
    private Rect tmpRect = new Rect();

    private boolean centerRemainingViews = true;
    private SparseArray<View> viewsCache = new SparseArray<>();
    private int childSize;
    private int availableWidth;
    private int childSizeSpec;

    public StupidLayoutManager(Context context) {
        scroller = new LinearSmoothScroller(context) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                if (getChildCount() == 0) {
                    return null;
                }
                final int firstChildPos = getPosition(getChildAt(0));
                final int direction = targetPosition < firstChildPos ? -1 : 1;
                return new PointF(0, direction);
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }
        };
    }

    public void setColumnCounts(@NonNull int[] values) {
        int max = 0;
        for (int i = 0, l = values.length; i < l; i++) {
            int v = values[i];
            if (v == 0) {
                throw new IllegalArgumentException(
                        "Zero should not be passed as a column count (found at index: " + i + ")");
            }
            if (v > max) {
                max = v;
            }
        }
        maxCount = max;
        columnCounts = Arrays.copyOf(values, values.length);
        requestLayout();
    }

    public void setChildMargins(int horizontal, int vertical) {
        if (horizontal < 0) {
            throw new IllegalArgumentException("horizontal margin must be >= 0, found: " + horizontal);
        }
        if (vertical < 0) {
            throw new IllegalArgumentException("vertical margin must be >= 0, found: " + vertical);
        }
        if (childMarginHorizontal != horizontal || childMarginVertical != vertical) {
            childMarginHorizontal = horizontal;
            childMarginVertical = vertical;
            requestLayout();
        }
    }

    public void setCenterRemainingViews(boolean center) {
        if (centerRemainingViews != center) {
            centerRemainingViews = center;
            requestLayout();
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);

        int itemCount = getItemCount();
        int row = 0;
        int count = columnCounts[row % columnCounts.length];
        int current = 0;
        for (int i = 0; i < itemCount; i++) {
            rowsByPositions.put(i, row);
            indexInRow.put(i, current);
            current++;
            if (current == count) {
                count = columnCounts[++row % columnCounts.length];
                current = 0;
            }
        }

        parentRect.set(0, 0, getWidth(), getHeight());

        availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        childSize = (availableWidth - childMarginHorizontal * (maxCount - 1)) / maxCount;
        childSizeSpec = View.MeasureSpec.makeMeasureSpec(childSize, View.MeasureSpec.EXACTLY);

        fill(recycler);
    }

    private void fill(RecyclerView.Recycler recycler) {
        View anchorView = getAnchorView();
        viewsCache.clear();

        for (int i = 0, c = getChildCount(); i < c; i++) {
            View view = getChildAt(i);
            int pos = getPosition(view);
            viewsCache.put(pos, view);
        }
        for (int i = 0; i < viewsCache.size(); i++) {
            detachView(viewsCache.valueAt(i));
        }

        fillUp(recycler, anchorView);
        fillDown(recycler, anchorView);

        for (int i = 0; i < viewsCache.size(); i++) {
            recycler.recycleView(viewsCache.valueAt(i));
        }

    }

    private void fillDown(RecyclerView.Recycler recycler, View anchorView) {
        int anchorTop = 0;
        int anchorPos = 0;
        if (anchorView != null) {
            anchorTop = getDecoratedTop(anchorView);
            anchorPos = getPosition(anchorView);
        }

        int bottomMargin = getHeight();
        int topOffset = anchorTop;
        int currentRow = rowsByPositions.get(anchorPos);
        int countForRow = childCountForRow(currentRow);
        int currentIndex = indexInRow.get(anchorPos);
        int leftOffset = getRowLeftOffset(countForRow, currentIndex);
        int itemCount = getItemCount();
        boolean fill = topOffset <= bottomMargin;

        int pos = anchorPos;
        while (fill && pos < itemCount) {
            View view = viewsCache.get(pos);
            if (view == null) {
                view = recycler.getViewForPosition(pos);
                addView(view);
                view.measure(childSizeSpec, childSizeSpec);
                layoutDecorated(view, leftOffset, topOffset, leftOffset + view.getMeasuredWidth(),
                        topOffset + view.getMeasuredHeight());
            } else {
                attachView(view);
                viewsCache.remove(pos);
            }
            pos++;
            if (++currentIndex == countForRow) {
                countForRow = childCountForRow(++currentRow);
                currentIndex = 0;
                topOffset += childSize + childMarginVertical;
                leftOffset = getRowLeftOffset(countForRow, currentIndex);
            } else {
                leftOffset += childSize + childMarginHorizontal;
            }
            fill = topOffset <= bottomMargin;
        }
    }

    private void fillUp(RecyclerView.Recycler recycler, View anchorView) {
        int anchorBottom = 0;
        int anchorPos = 0;
        if (anchorView != null) {
            anchorBottom = getDecoratedBottom(anchorView);
            anchorPos = getPosition(anchorView);
        }

        int topMargin = 0;
        int bottomOffset = anchorBottom;
        int currentRow = rowsByPositions.get(anchorPos);
        int countForRow = childCountForRow(currentRow);
        int currentIndex = indexInRow.get(anchorPos);
        int leftOffset = getRowLeftOffset(countForRow, currentIndex);
        boolean fill = topMargin <= bottomOffset;

        int pos = anchorPos;
        while (fill && pos >= 0) {
            View view = viewsCache.get(pos);
            if (view == null) {
                view = recycler.getViewForPosition(pos);
                addView(view);
                view.measure(childSizeSpec, childSizeSpec);
                layoutDecorated(view, leftOffset, bottomOffset - childSize,
                        leftOffset + childSize, bottomOffset);
            } else {
                attachView(view);
                viewsCache.remove(pos);
            }
            pos--;
            currentIndex--;
            if (currentIndex < 0) {
                if (currentRow == 0) {
                    return;
                }
                currentRow--;
                countForRow = childCountForRow(currentRow);
                currentIndex = countForRow - 1;
                bottomOffset -= childSize + childMarginVertical;
                leftOffset = getRowLeftOffset(countForRow, currentIndex);
            } else {
                leftOffset -= childSize + childMarginHorizontal;
            }
            fill = topMargin <= bottomOffset;
        }
    }

    private View getAnchorView() {
        int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }

        View anchorView = null;
        int maxSquare = 0;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            tmpRect.set(getDecoratedLeft(child), getDecoratedTop(child),
                    getDecoratedRight(child), getDecoratedBottom(child));
            if (tmpRect.intersect(parentRect)) {
                int square = tmpRect.width() * tmpRect.height();
                if (square > maxSquare) {
                    anchorView = child;
                    maxSquare = square;
                }
            }
        }

        return anchorView;
    }

    private int getRowLeftOffset(int countForRow, int index) {
        return getPaddingLeft() + centerOffset(countForRow) +
                (childSize + childMarginHorizontal) * index;
    }

    private int centerOffset(int countForRow) {
        return (availableWidth - childSize * countForRow -
                childMarginHorizontal * (countForRow - 1)) / 2;
    }

    private int childCountForRow(int row) {
        return columnCounts[row % columnCounts.length];
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        if (position >= getItemCount()) {
            return;
        }
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (false) {
            offsetChildrenVertical(-dy);
            fill(recycler);
            return dy;
        }

        if (getChildCount() == 0) {
            return 0;
        }

        View topView = getChildAt(0);
        View bottomView = getChildAt(getChildCount() - 1);

        int viewSpan = getDecoratedBottom(bottomView) - getDecoratedTop(topView);
        int availableHeight = getHeight();
        if (viewSpan <= availableHeight) {
//            return 0;
        }

        int delta = 0;
        if (dy < 0) {
            int position = getPosition(topView);
            if (position > 0) {
                delta = dy;
            } else {
                int decoratedTop = getDecoratedTop(topView);
                Log.d("StupidLayoutManager", "scrollVerticallyBy: " + decoratedTop);
                delta = Math.max(decoratedTop, dy);
            }
        } else if (dy > 0) {
            int position = getPosition(bottomView);
            if (position < getItemCount() - 1) {
                delta = dy;
            } else {
                delta = Math.min(getDecoratedBottom(bottomView) - getHeight(), dy);
            }
        }

        if (delta != 0) {
            offsetChildrenVertical(-delta);
            fill(recycler);
        }

        return delta;
    }

}
