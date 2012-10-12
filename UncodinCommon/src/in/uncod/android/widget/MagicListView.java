package in.uncod.android.widget;

import in.uncod.android.view.DragToDeleteManager;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.globalmentor.android.content.res.Themes;

/**
 * This class extends ListView to allow drag-and-reorder and drag-to-delete operations
 * 
 * @author cwc
 */
public class MagicListView extends ListView {
    /**
     * This interface provides callbacks for objects that are observing a MagicListView
     * 
     * @author cwc
     */
    public interface MagicListViewListener {
        /**
         * Called when an item is deleted by the list. The listening object should have the owner of the list's adapter
         * handle the deletion appropriately (i.e. confirm deletion with user, then remove the object from the adapter).
         * 
         * @param item
         */
        void onItemDelete(Object item);

        /**
         * Called when an item has been reordered by the list. The listening object should have the owner of the list's
         * adapter handle the reorder appropriately (i.e. confirm the reordering, then adjust the ordering of the
         * objects in the adapter).
         * 
         * @param movedItem
         * @param newParent
         */
        void onItemMoved(Object movedItem, Object newParent);
    }

    private MagicListViewListener mListListener;
    private Object mDraggingItem;
    private int mDraggingItemPos;
    private int mPreferredItemHeight;
    private ViewGroup mLastExpandedItem;
    private int mTouchSlop;
    static final int LONGPRESS_THRESHOLD = 500;
    Handler mHandler = new Handler();
    Vibrator mVibrator;
    private DragToDeleteManager mDragAndDeleteManager;
    private int mExpandingLayoutResourceId = -1;
    private int mDragHandleView;

    private Timer mLongPressTimer;
    private Timer mTapTimer;
    private boolean mLongPressDrag = false;
    private int mLongPressStartX;
    private int mLongPressStartY;

    int mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
    int mTapTimeout = ViewConfiguration.getTapTimeout();

    boolean tapTimedOut = false;

    public MagicListView(Context context) {
        super(context);

        setup(context);
    }

    public MagicListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setup(context);
    }

    public MagicListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setup(context);
    }

    private void setup(Context context) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mPreferredItemHeight = (int) Themes.getListPreferredItemHeightDimension(context);
        mDragAndDeleteManager = new DragToDeleteManager(context);

        setSelector(android.R.color.transparent);
    }

    /**
     * Sets the listener that will handle reordering and deletion events
     * 
     * @param listener
     */
    public void setReorderAndDeleteListener(MagicListViewListener listener) {
        mListListener = listener;
    }

    /**
     * Sets whether this list will allow objects to deleted via dragging
     * 
     * @param allowDelete
     */
    public void setDeleteAllowed(boolean allowDelete) {
        mDragAndDeleteManager.setDeleteAllowed(allowDelete);
    }

    /**
     * Intercepts the 'down' touch event, to make sure we can handle dragging on any child view
     * 
     * @param event
     *            The motion event
     * 
     * @return The default implementation's return value
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP) {
            onTouchEvent(event);
        }

        return super.onInterceptTouchEvent(event);
    }

    /* (non-Javadoc)
     * @see android.widget.AbsListView#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;

        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int itemnum = MagicListView.this.pointToPosition(x, y);

            if (itemnum == AdapterView.INVALID_POSITION) {
                return super.onTouchEvent(event);
            }

            mLongPressStartX = x;
            mLongPressStartY = y;

            handleTouchDown(event, x, itemnum);
            handled = true;
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (mDragAndDeleteManager.isDragging()) {
                int itemnum = MagicListView.this.pointToPosition(x, y);

                handleTouchMove(y, itemnum);

                handled = mDragAndDeleteManager.onTouchEvent(event);
            }
            else {
                if (Math.abs(mLongPressStartX - x) > mTouchSlop
                        || Math.abs(mLongPressStartY - y) > mTouchSlop) {
                    if (mTapTimer != null) {
                        mTapTimer.cancel();
                    }

                    if (mLongPressTimer != null) {
                        synchronized (MagicListView.this) {
                            mLongPressTimer.cancel();
                            mLongPressTimer = null;
                        }
                    }
                }
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            handleTouchUp(x, y);
            if (tapTimedOut) {
                tapTimedOut = false;
                handled = true;
            }
        }

        if (!handled)
            handled = super.onTouchEvent(event);

        return handled;
    }

    /**
     * Sets the resource ID of the view within child item layouts to show/hide when list items are being dragged
     * 
     * @param resourceId
     */
    public void setExpandingLayoutResource(int resourceId) {
        mExpandingLayoutResourceId = resourceId;
    }

    /**
     * Sets the resource ID of the view within child item layouts that will be used as the handle for dragging
     * 
     * @param resourceId
     */
    public void setDragHandleResource(int resourceId) {
        mDragHandleView = resourceId;
    }

    public void setLongPressForDrag(boolean longPress) {
        mLongPressDrag = longPress;
    }

    private void handleTouchUp(int x, int y) {
        if (mTapTimer != null) {
            mTapTimer.cancel();
        }
        if (mLongPressTimer != null) {
            synchronized (MagicListView.this) {
                mLongPressTimer.cancel();
                mLongPressTimer = null;
            }
        }

        if (mDragAndDeleteManager.isDeleting()) {
            mListListener.onItemDelete(mDraggingItem);
        }
        else if (mDragAndDeleteManager.isDragging()) {
            // Handle the reordering after dropping an item
            int itemnum = MagicListView.this.pointToPosition(x, y);
            Object newParent = getItemAtPosition(itemnum - 1);

            mListListener.onItemMoved(mDraggingItem, newParent);
        }

        // Collapse the expanded listview item, if it exists
        if (mLastExpandedItem != null) {
            mLastExpandedItem.setVisibility(View.GONE);
        }

        mDragAndDeleteManager.stopDragging();
    }

    private void handleTouchMove(int y, int itemnum) {
        // If the user drags an item over another item that isn't the list header or the item's original position,
        // expand the designated child of the underlying item's view to give the user the impression of an empty area in
        // which to drop the currently dragged item
        if (itemnum > this.getHeaderViewsCount() - 1 && itemnum != mDraggingItemPos) {
            // Collapse the previously expanded item
            if (mLastExpandedItem != null) {
                mLastExpandedItem.setVisibility(View.GONE);
            }

            // Expand the item at the current position
            View item = MagicListView.this.getChildAt(itemnum - MagicListView.this.getFirstVisiblePosition());
            mLastExpandedItem = (ViewGroup) item.findViewById(mExpandingLayoutResourceId);

            if (mLastExpandedItem != null) {
                mLastExpandedItem.setVisibility(View.VISIBLE);
            }
        }

        // Hide the view for the currently dragged item, if it would be visible
        if (mDraggingItemPos >= getFirstVisiblePosition() && mDraggingItemPos <= getLastVisiblePosition()) {
            hideChildView(itemnum);
        }

        // Handle scrolling of the listview while dragging
        int listHeight = getHeight();
        int upperBound = Math.min(y - mTouchSlop, listHeight / 3);
        int lowerBound = Math.max(y + mTouchSlop, listHeight * 2 / 3);
        int speed = 0;
        if (y > lowerBound) {
            // scroll the list up a bit
            speed = y > (getHeight() + lowerBound) / 2 ? 16 : 4;
        }
        else if (y < upperBound) {
            // scroll the list down a bit
            speed = y < upperBound / 2 ? -16 : -4;
        }
        if (speed != 0) {
            int ref = pointToPosition(0, getHeight() / 2);
            if (ref == AdapterView.INVALID_POSITION) {
                // we hit a divider or an invisible view, check somewhere else
                ref = pointToPosition(0, getHeight() / 2 + getDividerHeight() + 64);
            }
            View v = getChildAt(ref - getFirstVisiblePosition());
            if (v != null) {
                int pos = v.getTop();
                setSelectionFromTop(ref, pos - speed);
            }
        }
    }

    private void hideChildView(int itemnum) {

        int firstVisible = MagicListView.this.getFirstVisiblePosition();

        View current = MagicListView.this.getChildAt(mDraggingItemPos - firstVisible);

        ViewGroup.LayoutParams lp = current.getLayoutParams();

        if (itemnum == mDraggingItemPos) {
            lp.height = mPreferredItemHeight;
        }
        else {
            lp.height = 1;
        }

        current.setLayoutParams(lp);
        current.setVisibility(View.INVISIBLE);
        layoutChildren();
    }

    private void handleTouchDown(final MotionEvent event, int x, final int itemnum) {

        // Test that the user pressed on the drag handle for the item at these coordinates
        final View item = MagicListView.this.getChildAt(itemnum
                - MagicListView.this.getFirstVisiblePosition());

        if (!mLongPressDrag) {
            View dragger = item.findViewById(mDragHandleView);

            if (dragger != null) {
                Rect r = new Rect();

                dragger.getDrawingRect(r);

                mDraggingItem = getItemAtPosition(itemnum);
                mDraggingItemPos = itemnum;

                r.left = dragger.getLeft();
                r.right = dragger.getRight();
                r.top = dragger.getTop();
                r.bottom = dragger.getBottom();

                if ((r.left < x) && (x < r.right)) {
                    // Tell the drag manager that the user is dragging an item
                    startDrag(event, item, itemnum);
                }
            }
        }
        else {
            synchronized (MagicListView.this) {
                // Only initiate the long-press timer if one isn't already active
                if (mLongPressTimer == null) {
                    mLongPressTimer = new Timer();
                    mLongPressTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // Task finished, so clear the timer
                                    synchronized (MagicListView.this) {
                                        mLongPressTimer = null;
                                    }

                                    mDraggingItem = getItemAtPosition(itemnum);
                                    mDraggingItemPos = itemnum;
                                    startDrag(event, item, itemnum);
                                }
                            });
                        }
                    }, mLongPressTimeout + mTapTimeout);
                }
            }
        }
    }

    private void startDrag(MotionEvent event, View item, int itemnum) {
        mDraggingItem = getItemAtPosition(itemnum);
        hideChildView(itemnum);
        mDragAndDeleteManager.startDragging(item, event, mPreferredItemHeight);
    }

    /* (non-Javadoc)
     * @see android.widget.AbsListView#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Tell the drag manager to clean itself up, since this view is no longer visible
        this.mDragAndDeleteManager.release();
    }
}
