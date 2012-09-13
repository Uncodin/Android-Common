package in.uncod.android.media.widget;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.widget.ImageView;
import android.widget.ImageButton;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.LayoutInflater;
import android.provider.MediaStore;
import android.os.Environment;
import android.os.Bundle;
import android.net.Uri;
import android.graphics.Bitmap;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface;
import android.app.ProgressDialog;
import android.app.Activity;
import in.uncod.android.R;
import in.uncod.android.graphics.BitmapManager;
import in.uncod.android.media.widget.ImagePicker.ImagePickerListener.EditCancelCallback;

/**
 * A simple image picker
 */
public class ImagePicker extends AbstractMediaPickerFragment implements OnClickListener {
    private static final int REQCODE_GET_IMAGE = 0;
    private static final int REQCODE_CAPTURE_IMAGE = 1;

    private ImageView mImageThumbnail;
    private ImageButton mCameraButton;
    private ImageButton mGalleryButton;
    private ImageButton mEditButton;
    private ImagePickerListener mImagePickerListener;

    private Bitmap tempBitmap = null;

    private File mTempDirectory;
    private static String mCurrentPhotoPath;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layoutRoot = (ViewGroup) inflater.inflate(R.layout.image_picker, container, false);

        mImageThumbnail = (ImageView) layoutRoot.findViewById(R.id.image_thumbnail);

        mCameraButton = (ImageButton) layoutRoot.findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(this);

        mGalleryButton = (ImageButton) layoutRoot.findViewById(R.id.gallery_button);
        mGalleryButton.setOnClickListener(this);

        mEditButton = (ImageButton) layoutRoot.findViewById(R.id.edit_button);
        mEditButton.setOnClickListener(this);
        mEditButton.setEnabled(false);

        if (tempBitmap != null) {
            mImageThumbnail.setImageBitmap(tempBitmap);
            tempBitmap = null;
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("image")) {
                mCurrentPhotoPath = savedInstanceState.getString("image");
            }
        }

        return layoutRoot;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mTempDirectory = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("image", mCurrentPhotoPath);
    }

    @Override
    public void onClick(View v) {
        if (v == mGalleryButton) {
            launchImagePicker();
        }
        else if (v == mCameraButton) {
            launchCameraPicker();
        }
        else if (v == mEditButton) {
            // Create progress dialog
            final ProgressDialog dlg = ProgressDialog.show(getActivity(), getResources()
                    .getResourceEntryName(R.string.loading), null, true);
            dlg.setCancelable(true);
            dlg.show();

            mImagePickerListener.prepareForEdit(new ImageEditCancelCallback(dlg));
        }
    }

    /**
     * Publishes an intent to get an image
     */
    public void launchImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent = Intent.createChooser(intent, "Select an image source");
        startActivityForResult(intent, REQCODE_GET_IMAGE);
    }

    public void launchCameraPicker() {

        File file = null;
        try {
            file = createImageFile();
            Intent intent = new Intent();
            intent.setAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            intent = Intent.createChooser(intent, "Select an image source");
            startActivityForResult(intent, REQCODE_CAPTURE_IMAGE);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQCODE_CAPTURE_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                new UpdateMedia().execute(Uri.parse("file://" + mCurrentPhotoPath));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + "_";
        File image = File.createTempFile(imageFileName, ".jpg", mTempDirectory);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected File mediaChanged(Uri mediaUri) {
        if (mImagePickerListener != null) {
            return mImagePickerListener.imageChanged(mediaUri);
        }
        return null;
    }

    @Override
    protected String getProgressTitle() {
        return "Loading Image";
    }

    @Override
    public void updateMediaPreview(File mediaFile) {
        final Bitmap b = BitmapManager.loadBitmapScaled(mediaFile, 240);
        if (b != null && mImageThumbnail != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mImageThumbnail.setImageBitmap(b);
                    mEditButton.setEnabled(true);
                }
            });
        }
        else {
            tempBitmap = b;
        }
    }

    public void setOnImageChangedListener(ImagePickerListener listener) {
        mImagePickerListener = listener;
    }

    private class ImageEditCancelCallback extends EditCancelCallback {
        private final ProgressDialog dlg;
        private boolean mCanceled = false;

        private ImageEditCancelCallback(ProgressDialog dlg) {
            this.dlg = dlg;
            if (dlg != null) {
                dlg.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mCanceled = true;
                    }
                });
            }
        }

        public void cancel(boolean isCanceling) {
            if (dlg != null) {
                dlg.dismiss();
            }

            if (!isCanceling && !mCanceled) {
                mImagePickerListener.launchEditor();
            }
            else {
                mImagePickerListener.editingCanceled();
            }
        }
    }

    public interface ImagePickerListener {
        public class EditCancelCallback {
            public void cancel(boolean isCancelling) {
            }
        }

        public File imageChanged(Uri imageUri);

        public void editingCanceled();

        public void launchEditor();

        public void prepareForEdit(EditCancelCallback callback);

        public File getCurrentImage();
    }
}
