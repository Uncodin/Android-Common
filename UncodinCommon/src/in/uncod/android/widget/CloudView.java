package in.uncod.android.widget;

import in.uncod.android.view.DragToDeleteManager;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

/**
 * This view displays children from an adapter in a horizontal flow (wrapping to a new row when a child would go past
 * the right edge of this view). It also supports deletion of child items from within the view via drag-to-delete (the 
 * owner of the data source should register as a deletion listener, so the view can notify it when the user has 
 * requested a deletion).
 * 
 * @author cwc
 * 
 * @param <T>
 *            The type of Adapter from which this view will receive child views
 */
/**
 * @author cwc
 * 
 * @param <T>
 */
public class CloudView<T extends Adapter> extends AdapterView<T> {
    private T mAdapter;
    private int mSelectedViewPosition = -1;
    private Context mContext;
    private List<OnItemDeletionListener<T>> mItemDeletionListeners = new ArrayList<OnItemDeletionListener<T>>();
    private DragToDeleteManager mDeletionManager;
    private int mDraggedPosition;
    private DataSetObserver mObserver;

    public CloudView(Context context) {
        super(context);

        init(context);
    }

    public CloudView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mDeletionManager = new DragToDeleteManager(mContext);

        // Create observer to refresh the view when an adapter's data has changed
        mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();

                refreshView();
            }
        };
    }

    private void refreshView() {
        removeAllViewsInLayout();
        requestLayout();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.widget.AdapterView#getAdapter()
     */
    @Override
    public T getAdapter() {
        return mAdapter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.widget.AdapterView#setAdapter(android.widget.Adapter)
     */
    @Override
    public void setAdapter(T adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }

        mAdapter = adapter;
        mAdapter.registerDataSetObserver(mObserver);

        refreshView();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.widget.AdapterView#getSelectedView()
     */
    @Override
    public View getSelectedView() {
        if (mSelectedViewPosition > -1) {
            return mAdapter.getView(mSelectedViewPosition, null, null); // TODO What do we use instead of nulls here?
        }
        else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.widget.AdapterView#setSelection(int)
     */
    @Override
    public void setSelection(int position) {
        mSelectedViewPosition = position;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.widget.AdapterView#onLayout(boolean, int, int, int, int)
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mAdapter == null) {
            return;
        }

        if (getChildCount() == 0) {
            int position = 0;
            while (position < mAdapter.getCount()) {
                // Add each item's view to the cloud view
                View child = mAdapter.getView(position, null, this);
                addViewInLayout(child, -1, new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT), true);

                child.measure(MeasureSpec.AT_MOST | getWidth(), MeasureSpec.UNSPECIFIED);
                position++;
            }
        }

        positionItems();
    }

    /**
     * Positions the children
     */
    private void positionItems() {
        int top = 0; // Distance from top of screen
        int usedWidth = 0; // The amount of horizontal space used so far

        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);

            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();

            // If we can't fit this child on the current row...
            if (usedWidth + width > getWidth()) {
                // ...move down a row
                top += height;
                usedWidth = 0;
            }

            child.layout(usedWidth, top, usedWidth += width, top + height);
        }
    }

    /**
     * Registers the given object to receive notification when a child object of this cloud has been deleted
     * 
     * @param itemDeletedListener
     */
    public void addOnItemDeletionListener(OnItemDeletionListener<T> itemDeletedListener) {
        mItemDeletionListeners.add(itemDeletedListener);
    }

    /**
     * Deregisters the given listener so that it no longer receives notification when a child is deleted
     * 
     * @param itemDeletedListener
     */
    public void removeOnItemDeletionListener(OnItemDeletionListener<T> itemDeletedListener) {
        mItemDeletionListeners.remove(itemDeletedListener);
    }

    /**
     * An interface for responding to deletion of items in an AdapterView
     * 
     * @author cwc
     * 
     * @param <T>
     *            The type of Adapter used in the AdapterView
     */
    public interface OnItemDeletionListener<T extends Adapter> {
        void onItemDeletion(AdapterView<T> parent, View deletedView, int position, long id);
    }

    /* (non-Javadoc)
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            handled = handleTouchDown(event);
            break;
        case MotionEvent.ACTION_UP:
            handled = handleTouchUp(event);
            break;
        case MotionEvent.ACTION_MOVE:
            handled = handleTouchMove(event);
            break;
        }

        if (!handled)
            handled = super.onTouchEvent(event);

        return handled;
    }

    private boolean handleTouchMove(MotionEvent event) {
        return mDeletionManager.onTouchEvent(event);
    }

    private boolean handleTouchUp(MotionEvent event) {
        boolean handled = false;

        if (mDeletionManager.isDeleting()) {
            onItemDeleted(mDeletionManager.getDraggedView(), mDraggedPosition);

            handled = true;
        }

        View draggedView = mDeletionManager.getDraggedView();
        if (draggedView != null) {
            draggedView.setVisibility(VISIBLE);
            mDeletionManager.stopDragging();
            mDraggedPosition = -1;
        }

        return handled;
    }

    private void onItemDeleted(View deletedView, int position) {
        for (OnItemDeletionListener<T> listener : mItemDeletionListeners) {
            listener.onItemDeletion(this, deletedView, position, -1);
        }

        invalidate(); // Force redraw under assumption the item will be removed from the adapter
    }

    private boolean handleTouchDown(MotionEvent event) {
        boolean handled = false;

        int x = (int) event.getX();
        int y = (int) event.getY();

        int outPosition = getChildPositionAtCoords(x, y);
        View touchedView = getChildAt(outPosition);
        if (touchedView != null) {
            touchedView.setVisibility(GONE);
            mDeletionManager.startDragging(touchedView, event);
            mDraggedPosition = outPosition;
            handled = true;
        }

        return handled;
    }

    private int getChildPositionAtCoords(int x, int y) {
        int childPosition = -1;

        for (int position = 0; position < mAdapter.getCount(); position++) {
            View child = getChildAt(position);

            if (child == null)
                continue;

            // Test that coords are within child's bounding box
            if (child.getLeft() <= x && child.getTop() <= y && child.getRight() >= x
                    && child.getBottom() >= y) {
                childPosition = position;
                break;
            }
        }

        return childPosition;
    }
}
