package test.strangegrid;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;

import java.util.Arrays;

public class StrangeGridLayoutManager extends RecyclerView.LayoutManager {

    /**
     * Additional horizontal margin between child views
     */
    private int childMarginHorizontal = 0;
    /**
     * Additional vertical margin between child views
     */
    private int childMarginVertical = 0;

    /**
     * Array of column counts for each row. If there are more rows than specified in array,
     * they will be cycled.
     */
    private int[] columnCounts = {1};
    /**
     * Maximum number of children in single row
     */
    private int maxCount = columnCounts[0];

    /**
     * Cache for quick access to row number by adapter position
     */
    private SparseIntArray rowsByPos = new SparseIntArray();
    /**
     * Child indicies within their row
     */
    private SparseIntArray indexInRow = new SparseIntArray();
    /**
     * Temporary view cache for single fill pass
     */
    private SparseArray<View> viewsCache = new SparseArray<>();
    /**
     * Child size (every child is a square)
     */
    private int childSize;
    private int childSizeSpec;
    /**
     * Amount of horizontal space available to child views
     */
    private int availableWidth;
    private Rect parentRect = new Rect();
    private Rect tmpRect = new Rect();

    /**
     * Enables adaptive children sizes
     */
    private boolean adaptive = false;
    private int adaptiveMinSize = 0;
    private int[] adaptiveOffsets;

    private LinearSmoothScroller smoothScroller;

    public StrangeGridLayoutManager(Context context) {
        smoothScroller = new LinearSmoothScroller(context) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                if (getChildCount() == 0) {
                    return null;
                }
                final int pos = getPosition(getChildAt(0));
                return new PointF(0, targetPosition < pos ? -1 : 1);
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }
        };
    }

    /**
     * Sets column counts for this layout manager.
     *
     * @param values array of ints, should not contain values < 1
     */
    public void setColumnCounts(@NonNull int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Array must contain at least one value");
        }
        int max = 0;
        for (int i = 0, l = values.length; i < l; i++) {
            int v = values[i];
            if (v <= 0) {
                throw new IllegalArgumentException(
                        "Zero and negative numbers should not be passed as a column count (found at index: " + i + ")");
            }
            if (v > max) {
                max = v;
            }
        }
        maxCount = max;
        columnCounts = Arrays.copyOf(values, values.length);
        adaptive = false;
        requestLayout();
    }

    /**
     * Setups additional margins for child views.
     *
     * @param horizontal horizonatal margin
     * @param vertical   vertical margin
     */
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

    /**
     * Setups layout manager for adaptive child sizes. If specific offset value for row happen to be
     * greater than maximum count of children withing single row, manager will assume that row will
     * contain only one child.
     *
     * @param minSize minumum size of a child
     * @param offsets offsets for column counts, must contain only zeros and positive numbers
     */
    public void setupForAdaptiveSize(int minSize, @Nullable int[] offsets) {
        if (minSize <= 0) {
            throw new IllegalArgumentException("minSize must be > 0");
        }
        if (offsets != null) {
            for (int i = 0, c = offsets.length; i < c; i++) {
                if (offsets[i] < 0) {
                    throw new IllegalArgumentException("offsets should contain only positive values," +
                            " but found " + offsets[i] + " at index " + i);
                }
            }
        }
        adaptive = true;
        adaptiveMinSize = minSize;
        adaptiveOffsets = offsets;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        parentRect.set(0, 0, getWidth(), getHeight());
        availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        if (adaptive) {
            // calculate child size
            int newCount = availableWidth / Math.min(availableWidth, adaptiveMinSize);
            if (newCount != maxCount) {
                maxCount = newCount;
                if (maxCount <= 0) {
                    throw new IllegalStateException("Cannot calculate max count for min child size: " +
                            adaptiveMinSize);
                }
                if (adaptiveOffsets != null) {
                    int c = adaptiveOffsets.length;
                    if (c == 0) {
                        columnCounts = new int[]{maxCount};
                    } else {
                        columnCounts = new int[c];
                        for (int i = 0; i < c; i++) {
                            // apply offsets
                            columnCounts[i] = Math.max(1, maxCount - adaptiveOffsets[i]);
                        }
                    }
                } else {
                    columnCounts = new int[]{maxCount};
                }
            }
        }
        childSize = (availableWidth - childMarginHorizontal * (maxCount - 1)) / maxCount;
        childSizeSpec = View.MeasureSpec.makeMeasureSpec(childSize, View.MeasureSpec.EXACTLY);

        // create caches to avoid unnecesary computation
        int row = 0;
        int count = childCountForRow(row);
        int current = 0;
        rowsByPos.clear();
        indexInRow.clear();
        for (int i = 0, c = getItemCount(); i < c; i++) {
            rowsByPos.put(i, row);
            indexInRow.put(i, current);
            current++;
            if (current == count) {
                count = childCountForRow(++row);
                current = 0;
            }
        }

        detachAndScrapAttachedViews(recycler);
        fill(recycler);
    }

    /**
     * Fills the layout.
     */
    private void fill(RecyclerView.Recycler recycler) {
        View anchorView = getAnchorView();
        viewsCache.clear();

        // gather currently visible views into cache
        for (int i = 0, c = getChildCount(); i < c; i++) {
            View view = getChildAt(i);
            int pos = getPosition(view);
            viewsCache.put(pos, view);
        }
        // detach visible views
        for (int i = 0; i < viewsCache.size(); i++) {
            detachView(viewsCache.valueAt(i));
        }

        // fill the layout
        fillUp(recycler, anchorView);
        fillDown(recycler, anchorView);

        // recycle any views, which were not attached to layout
        for (int i = 0; i < viewsCache.size(); i++) {
            recycler.recycleView(viewsCache.valueAt(i));
        }

    }

    /**
     * Fills the layout in up direction starting at anchor view position.
     *
     * @param recycler   recycler
     * @param anchorView anchor view
     */
    private void fillUp(RecyclerView.Recycler recycler, View anchorView) {
        int anchorBottom = 0;
        int anchorPos = 0;
        if (anchorView != null) {
            anchorBottom = anchorView.getBottom();
            anchorPos = getPosition(anchorView);
        }

        int bottomMargin = 0;
        int bottom = anchorBottom; // current bottom position
        int currentRow = rowsByPos.get(anchorPos);
        int count = childCountForRow(currentRow);
        int currentIndex = indexInRow.get(anchorPos); // current view index within its row
        int leftOffset = getChildLeftOffset(count, currentIndex);

        int pos = anchorPos;
        while (bottom > bottomMargin && pos >= 0) {
            View view = viewsCache.get(pos);
            if (view != anchorView) {
                // view should be added/attached at index 0
                if (view == null) {
                    view = recycler.getViewForPosition(pos);
                    addView(view, 0);
                    view.measure(childSizeSpec, childSizeSpec);
                    layoutDecorated(view, leftOffset, bottom - childSize, leftOffset + childSize, bottom);
                } else {
                    attachView(view, 0);
                    viewsCache.remove(pos);
                }
            }
            pos--;
            // check if we have done with current row
            if (--currentIndex < 0) {
                // return if the the first row was processed
                if (currentRow == 0) {
                    return;
                }
                currentRow--;
                count = childCountForRow(currentRow);
                currentIndex = count - 1;
                bottom -= childSize + childMarginVertical;
                leftOffset = getChildLeftOffset(count, currentIndex);
            } else {
                // shift left margin
                leftOffset -= childSize + childMarginHorizontal;
            }
        }
    }

    /**
     * Fills the layout in down direction, starting at anchor view position.
     *
     * @param recycler   recycler
     * @param anchorView anchor view
     */
    private void fillDown(RecyclerView.Recycler recycler, View anchorView) {
        int anchorTop = getPaddingTop();
        int anchorPos = 0;
        if (anchorView != null) {
            anchorTop = anchorView.getTop();
            anchorPos = getPosition(anchorView);
        }

        int topMargin = getHeight() - getPaddingBottom();
        int top = anchorTop; // current top position
        int currentRow = rowsByPos.get(anchorPos);
        int countForRow = childCountForRow(currentRow);
        int currentIndex = indexInRow.get(anchorPos);
        int leftOffset = getChildLeftOffset(countForRow, currentIndex);
        int itemCount = getItemCount();

        int pos = anchorPos;
        while (top <= topMargin && pos < itemCount) {
            View view = viewsCache.get(pos);
            if (view == null) {
                view = recycler.getViewForPosition(pos);
                addView(view);
                view.measure(childSizeSpec, childSizeSpec);
                layoutDecorated(view, leftOffset, top, leftOffset + childSize, top + childSize);
            } else {
                attachView(view);
                viewsCache.remove(pos);
            }
            pos++;
            // check if we have reached the end of the row
            if (++currentIndex == countForRow) {
                countForRow = childCountForRow(++currentRow);
                currentIndex = 0;
                top += childSize + childMarginVertical;
                leftOffset = getChildLeftOffset(countForRow, currentIndex);
            } else {
                leftOffset += childSize + childMarginHorizontal;
            }
        }
    }

    /**
     * Find view with maximum visible area. If there are views with the same areas, return only the
     * first found.
     *
     * @return view with most visible area
     */
    private View getAnchorView() {
        int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }

        View anchorView = null;
        int maxArea = 0;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            tmpRect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            // intersect child rect with parent rect
            if (tmpRect.intersect(parentRect)) {
                // find the area of intersection
                int area = tmpRect.width() * tmpRect.height();
                if (area > maxArea) { // use >= to return last found view
                    anchorView = child;
                    maxArea = area;
                }
            }
        }

        return anchorView;
    }

    /**
     * Calculate left offset for view at index within row.
     *
     * @param count maximum children count in row
     * @param index specific child position within row
     * @return left offset, includes padding
     */
    private int getChildLeftOffset(int count, int index) {
        return getPaddingLeft() + centerOffset(count) +
                (childSize + childMarginHorizontal) * index;
    }

    /**
     * Calculate lateral offset to center the views within row.
     *
     * @param count maximum children count in row
     * @return offset
     */
    private int centerOffset(int count) {
        return (availableWidth - childSize * count -
                childMarginHorizontal * (count - 1)) / 2;
    }

    /**
     * @param row row to get child count for
     * @return max child count of the row
     */
    private int childCountForRow(int row) {
        return columnCounts[row % columnCounts.length];
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public void scrollToPosition(int position) {
        if (position >= getItemCount()) {
            return;
        }
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        scrollToPosition(position);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        View topView = getChildAt(0);
        View bottomView = getChildAt(getChildCount() - 1);

        int bottom = bottomView.getBottom();
        int top = topView.getTop();
        int viewSpan = bottom - top;
        // check if all views are fit into the parent
        if (viewSpan <= getHeight() - getPaddingTop() - getPaddingBottom() - childSize) {
            return 0;
        }

        int delta = 0;
        if (dy < 0) {
            // scrolling towards begining of list
            int position = getPosition(topView);
            if (position > 0) {
                delta = dy;
            } else {
                delta = Math.max(top - getPaddingTop(), dy);
            }
        } else if (dy > 0) {
            // scrolling towards end of list
            int position = getPosition(bottomView);
            if (position < getItemCount() - 1) {
                delta = dy;
            } else {
                delta = Math.min(bottom - getHeight() + getPaddingBottom(), dy);
            }
        }

        if (delta != 0) {
            // scroll children
            offsetChildrenVertical(-delta);
            // if scroll position changed, perhaps we need to fill layout
            fill(recycler);
        }

        return delta;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
    }

}
