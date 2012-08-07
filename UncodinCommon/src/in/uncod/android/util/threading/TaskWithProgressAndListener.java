package in.uncod.android.util.threading;

import android.app.ProgressDialog;
import android.util.Log;

/**
 * A task that accepts an optional results callback and progress dialog.
 */
public abstract class TaskWithProgressAndListener<TParams, TProgress, TResult> extends
        TaskWithResultListener<TParams, TProgress, TResult> {
    private static final String TAG = "TaskWithProgressAndListener";

    protected ProgressDialog mDialog;

    /**
     * Creates a task.
     * 
     * @param listener
     *            If not null, the result listener will be activated when the task has completed.
     * @param dialog
     *            If not null, the progress dialog will be shown and dismissed automatically. Any special settings such
     *            as cancellation, style, etc. should be set on the dialog before the task is executed, and progress
     *            updates must be handled manually.
     */
    public TaskWithProgressAndListener(OnTaskResultListener<TResult> listener, ProgressDialog dialog) {
        super(listener);

        mDialog = dialog;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (mDialog != null) {
            mDialog.setProgress(0);

            try {
                mDialog.show();
            }
            catch (Exception e) {
                e.printStackTrace();

                Log.e(TAG, "Unable to show dialog; did you forget to start the task on a UI thread?");
            }
        }
    }

    @Override
    protected void onPostExecute(final TResult result) {
        super.onPostExecute(result);

        if (mDialog != null) {
            mDialog.dismiss();
        }
    }
}