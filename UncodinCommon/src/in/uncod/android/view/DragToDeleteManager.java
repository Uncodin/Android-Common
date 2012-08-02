package in.uncod.android.view;

import in.uncod.android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * This class attaches a delete hotspot to the window manager of the given context. Supply it with MotionEvents via the
 * onTouchEvent() method, and it will track whether the user has dragged something onto the hotspot, allowing another
 * object to check whether the item should be deleted.
 * 
 * @author cwc
 */
public class DragToDeleteManager extends DragAndDropManager {
    Vibrator mVibrator;
    ImageView mTrashImageView;
    boolean mHoveringOverTrash;
    ViewGroup mTrashContainer;
    boolean mTrashVisible;
    private boolean mTrashContainerAttached;
    private boolean mAllowDelete = true;
    private WindowManager.LayoutParams mTrashWindowParams;

    public DragToDeleteManager(Context context) {
        super(context);

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mTrashContainer = new FrameLayout(mContext);

        // Set up layout params for the trash container
        mTrashWindowParams = new WindowManager.LayoutParams();
        mTrashWindowParams.gravity = Gravity.TOP | Gravity.LEFT;

        mTrashWindowParams.height = WindowManager.LayoutParams.FILL_PARENT;
        mTrashWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mTrashWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        mTrashWindowParams.format = PixelFormat.TRANSLUCENT;
        mTrashWindowParams.windowAnimations = 0;
    }

    /**
     * Sets whether or not to display the delete hotspot
     * 
     * @param dragging
     */
    private void setDeleteHotspotVisible(boolean dragging) {
        mTrashVisible = dragging;

        mTrashContainer.removeAllViews();

        if (mTrashVisible && mAllowDelete) {
            if (!mTrashContainerAttached) {
                mTrashContainerAttached = true;
                mWindowManager.addView(mTrashContainer, mTrashWindowParams);
            }

            if (mTrashImageView == null) {
                mTrashImageView = new ImageView(mContext);
                mTrashImageView.setBackgroundColor(mContext.getResources().getColor(R.color.red));
                mTrashImageView.setPadding(10, 0, 10, 0);
            }

            mTrashImageView.setImageResource(R.drawable.ic_launcher_trashcan_normal_holo);
            mTrashContainer.addView(mTrashImageView);
        }
        else {
            mHoveringOverTrash = false;

            if (mTrashContainerAttached) {
                mTrashContainerAttached = false;
                mWindowManager.removeView(mTrashContainer);
            }
        }
    }

    /**
     * @return true if the last MotionEvent showed that the user was hovering over the delete hotspot
     */
    public boolean isDeleting() {
        return mHoveringOverTrash;
    }

    /**
     * This method is used to notify the delete manager of motion events in order to track whether the user is
     * attempting a deletion
     * 
     * @param event
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = super.onTouchEvent(event);

        if (mTrashVisible && mAllowDelete) { // User is dragging something
            int x = (int) event.getX();

            if (x < mTrashContainer.getRight() && !mHoveringOverTrash) {
                mHoveringOverTrash = true;
                mTrashImageView.setImageResource(R.drawable.ic_launcher_trashcan_active_holo);
                mVibrator.vibrate(100);
            }
            else if (x > mTrashContainer.getRight() + 10) {
                mHoveringOverTrash = false;
                mTrashImageView.setImageResource(R.drawable.ic_launcher_trashcan_normal_holo);
            }
        }

        return handled;
    }

    /* (non-Javadoc)
     * @see com.uncodin.android.ui.DragAndDropManager#startDragging(android.graphics.Bitmap, int, int)
     */
    @Override
    protected void startDragging(Bitmap bm, int x, int y) {
        super.startDragging(bm, x, y);

        setDeleteHotspotVisible(true);
    }

    /* (non-Javadoc)
     * @see com.uncodin.android.ui.DragAndDropManager#stopDragging()
     */
    @Override
    public void stopDragging() {
        super.stopDragging();

        setDeleteHotspotVisible(false);
    }

    /**
     * Sets whether or not this drag manager allows deletion
     * 
     * @param allowDelete
     */
    public void setDeleteAllowed(boolean allowDelete) {
        mAllowDelete = allowDelete;
    }
}
