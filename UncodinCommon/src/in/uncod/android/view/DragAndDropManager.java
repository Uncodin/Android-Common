package in.uncod.android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * This class handles the visual portion of dragging and dropping a UI element. Refer to the startDragging(),
 * stopDragging(), onTouchEvent(), and release() methods for usage.
 *
 * @author cwc
 */
public class DragAndDropManager {
    protected Context mContext;
    protected WindowManager mWindowManager;
    private LayoutParams mWindowParams;
    private FrameLayout mDragContainer;
    private ImageView mDraggingImageView;
    private boolean mDragging;
    private int[] mOffset = {0, 0};
    private View mDraggedView;
    private boolean mContainerAttached;

    public DragAndDropManager(Context context) {
        mContext = context;

        setUpDragContainer();
    }

    /**
     * Sets up the window parameters for the drag container
     */
    protected void setUpDragContainer() {
        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;

        mWindowParams.height = WindowManager.LayoutParams.FILL_PARENT;
        mWindowParams.width = WindowManager.LayoutParams.FILL_PARENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;

        mWindowManager = (WindowManager) mContext.getSystemService("window");

        mDragContainer = new FrameLayout(mContext);
        mDragContainer.setForegroundGravity(Gravity.TOP | Gravity.LEFT);

        mWindowManager.addView(mDragContainer, mWindowParams);
        mContainerAttached = true;
    }

    /**
     * Responds to motion events and handles the display of dragged views
     *
     * @param event
     * @return true if event was handled successfully
     */
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;

        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_MOVE && isDragging()) {
            scrollDraggingImage(x, y);

            handled = true;
        }

        return handled;
    }

    protected void startDragging(Bitmap bitmap, int x, int y) {
        mDragging = true;
        mDragContainer.removeAllViews();
        mDraggingImageView = new ImageView(mContext);
        mDraggingImageView.setImageBitmap(bitmap);
        mDraggingImageView.setScaleType(ImageView.ScaleType.CENTER);
        mDragContainer.addView(mDraggingImageView);

        if (!mContainerAttached) {
            mWindowManager.addView(mDragContainer, mWindowParams);
            mContainerAttached = true;
        }
    }

    /**
     * Tells this drag manager that no views are being dragged
     */
    public void stopDragging() {
        mDragging = false;
        mDraggedView = null;
        if (mContainerAttached) {
            mWindowManager.removeView(mDragContainer);
            mContainerAttached = false;
        }
        mDragContainer.removeAllViews();
    }

    /**
     * Tells this drag manager that the given child view is being dragged
     *
     * @param draggedView
     * @param event
     */
    public void startDragging(View draggedView, MotionEvent event) {
        startDragging(draggedView, event, 0);
    }

    /**
     * Tells this drag manager that the given child view is being dragged
     *
     * @param draggedView
     * @param event
     * @param yOffset     The number of pixels to subtract from the y-position of the dragged view
     */
    public void startDragging(View draggedView, MotionEvent event, int yOffset) {
        mDraggedView = draggedView;

        int x = (int) event.getX();
        int y = (int) event.getY();

        Bitmap bitmap = createBitmapFromView(draggedView);
        startDragging(bitmap, x, y);

        // Set offsets used when scrolling the ImageView
        mOffset[0] = x + ((mDragContainer.getWidth() / 2) - (bitmap.getWidth() / 2));
        mOffset[1] = mDragContainer.getHeight() / 2 - (draggedView.getHeight() / 2);

        scrollDraggingImage(x, y - yOffset);
    }

    private void scrollDraggingImage(int x, int y) {
        if (mDraggingImageView != null) {
            mDraggingImageView.scrollTo(-1 * (x - mOffset[0]), -1 * (y - mOffset[1]));
        }
    }

    private Bitmap createBitmapFromView(View view) {
        view.setDrawingCacheEnabled(true);
        // Create a copy of the drawing cache so that it does not get recycled
        // by the framework when the list tries to clean up memory
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    /**
     * @return true if the user is dragging an item
     */
    public boolean isDragging() {
        return mDragging;
    }

    /**
     * Gets the currently dragged view
     *
     * @return The View supplied when startDragging() was called, or null if no longer dragging a View
     */
    public View getDraggedView() {
        return mDraggedView;
    }

    /**
     * Tears down this manager. Should be called by the host view in i.e. onDetachedFromWindow()
     */
    public void release() {
        if (this.mContainerAttached) {
            this.mDragContainer.removeAllViews();
            if (mContainerAttached) {
                this.mWindowManager.removeView(mDragContainer);
                this.mContainerAttached = false;
            }
        }
    }
}
