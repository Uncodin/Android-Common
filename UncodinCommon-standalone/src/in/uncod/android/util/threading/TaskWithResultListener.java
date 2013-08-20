package in.uncod.android.util.threading;

import android.os.AsyncTask;

/**
 * A task that accepts an optional results callback.
 */
public abstract class TaskWithResultListener<TParams, TProgress, TResult> extends
        AsyncTask<TParams, TProgress, TResult> {
    /**
     * Interface for task result listeners.
     */
    public static interface OnTaskResultListener<TResult> {
        /**
         * This method will be activated when the task completes.
         * 
         * @param result
         *            The task results.
         */
        public void onTaskResult(TResult result);
    }

    /**
     * Creates a task.
     * 
     * @param listener
     *            If not null, the result listener will be activated when the task has completed.
     */
    public TaskWithResultListener(OnTaskResultListener<TResult> listener) {
        mListener = listener;
    }

    private OnTaskResultListener<TResult> mListener;

    @Override
    protected void onPostExecute(TResult result) {
        if (mListener != null) {
            mListener.onTaskResult(result);
        }
    }
}