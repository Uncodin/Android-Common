package in.uncod.android.media.widget;

import java.io.File;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by IntelliJ IDEA. User: ddrboxman Date: 4/4/12 Time: 12:26 PM
 */
public abstract class AbstractMediaPickerFragment extends Fragment {
    protected abstract File mediaChanged(Uri mediaUri);

    protected abstract String getProgressTitle();

    public abstract void updateMediaPreview(File mediaFile);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    protected void runOnUiThread(Runnable runnable) {
        getActivity().runOnUiThread(runnable);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {

            Uri mediaLocation = null;
            if (data != null) {
                mediaLocation = data.getData();
            }

            new UpdateMedia().execute(mediaLocation);

        }
    }

    private class UpdateMedia extends AsyncTask<Uri, Object, File> {

        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(getActivity());
            dialog.setTitle(getProgressTitle());
            dialog.show();
        }

        @Override
        protected File doInBackground(Uri... uris) {

            if (uris.length > 0) {

                Uri mediaUri = uris[0];

                return mediaChanged(mediaUri);
            }

            return null;
        }

        @Override
        protected void onPostExecute(File mediaFile) {
            if (mediaFile != null) {
                updateMediaPreview(mediaFile);
            }
            if (dialog.isShowing()) {
                try {
                    dialog.dismiss();
                }
                catch (Exception e) {

                }
            }
        }
    }
}
