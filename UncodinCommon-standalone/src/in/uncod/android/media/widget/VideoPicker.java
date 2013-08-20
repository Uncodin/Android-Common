package in.uncod.android.media.widget;

import in.uncod.android.R;

import java.io.File;
import java.io.IOException;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

/**
 * A simple video picker
 * <p/>
 * TODO Add support for cloud videos (i.e. Picasa videos via Gallery app)
 */
public class VideoPicker extends AbstractMediaPickerFragment implements OnClickListener {
    private static final int REQCODE_GET_VIDEO = 0;
    private static final int REQCODE_CAPTURE_VIDEO = 0;

    private SurfaceView mVideoPreview;
    private ImageButton mSelectVideoButton;
    private ImageButton mCaptureVideoButton;

    private MediaPlayer mMediaPlayer;
    private Uri videoSource;

    OnVideoChangedListener mOnVideoChangedListener;

    private File mTempDirectory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layoutRoot = inflater.inflate(R.layout.video_picker, container, false);

        mVideoPreview = (SurfaceView) layoutRoot.findViewById(R.id.video_preview);
        mVideoPreview.setOnClickListener(this);

        mVideoPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // Deprecated, but must be called in order to work on 2.2 (e.g. Dell Streak)

        // Add a surface holder callback as an alternative route for preparing the media player
        // The surface is destroyed if a full-blown activity (such as Gallery) is launched in order to get a video, so
        // there must also be logic here to prepare the player when the surface is recreated
        mVideoPreview.getHolder().addCallback(new Callback() {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                shutdownMediaPlayer();
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                prepareMediaPlayer();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
        });

        mSelectVideoButton = (ImageButton) layoutRoot.findViewById(R.id.select_video_button);
        mSelectVideoButton.setOnClickListener(this);
        mCaptureVideoButton = (ImageButton) layoutRoot.findViewById(R.id.capture_video_button);
        mCaptureVideoButton.setOnClickListener(this);

        return layoutRoot;
    }

    @Override
    public void onPause() {
        super.onPause();

        shutdownMediaPlayer();
    }

    @Override
    public void onClick(View view) {
        if (view == mSelectVideoButton) {
            launchVideoPicker();
        }
        else if (view == mCaptureVideoButton) {
            launchVideoCapture();
        }
        else if (view == mVideoPreview) {
            toggleVideoPlaying();
        }
    }

    /**
     * Play or stop playing the selected video
     */
    private void toggleVideoPlaying() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mMediaPlayer.seekTo(0);
            }
            else if (videoSource != null) {
                mVideoPreview.setBackgroundDrawable(null);
                mMediaPlayer.start();
            }
        }
    }

    /**
     * Publishes an intent to get a video file
     */
    public void launchVideoPicker() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent = Intent.createChooser(intent, "Select a video source");
        startActivityForResult(intent, REQCODE_GET_VIDEO);
    }

    public void launchVideoCapture() {
        File file = new File(mTempDirectory, "vid.temp");

        Intent intent = new Intent();
        intent.setAction(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        intent = Intent.createChooser(intent, "Select an video source");
        startActivityForResult(intent, REQCODE_CAPTURE_VIDEO);
    }

    /**
     * Initializes the media player and prepares it for video playback
     */
    private void prepareMediaPlayer() {
        if (videoSource != null && mVideoPreview.getHolder().getSurface().isValid()) {
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setOnVideoSizeChangedListener(mVideoSizeListener);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING); // Use ringer volume
                mMediaPlayer.setDataSource(getActivity(), videoSource);
                mMediaPlayer.setDisplay(mVideoPreview.getHolder());
                mMediaPlayer.prepare();

            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    MediaPlayer.OnVideoSizeChangedListener mVideoSizeListener = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
            int surfaceHeight = mVideoPreview.getHeight();
            int surfaceWidth = mVideoPreview.getWidth();

            android.view.ViewGroup.LayoutParams lp = mVideoPreview.getLayoutParams();

            int newHeight = (int) ((double) surfaceWidth * ((double) height / (double) width));
            if (newHeight <= surfaceHeight) {
                lp.height = newHeight;
            }
            else {
                lp.width = (int) ((double) surfaceHeight * ((double) width / (double) height));
            }
        }
    };

    /**
     * Releases resources used by the media player
     */
    private void shutdownMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }

    public void setOnVideoChangedListener(OnVideoChangedListener listener) {
        mOnVideoChangedListener = listener;
    }

    public interface OnVideoChangedListener {
        public File videoChanged(Uri videoUri);
    }

    @Override
    protected File mediaChanged(Uri mediaUri) {
        if (mOnVideoChangedListener != null) {
            return mOnVideoChangedListener.videoChanged(mediaUri);
        }
        return null;
    }

    @Override
    protected String getProgressTitle() {
        return "Loading Video";
    }

    @Override
    public void updateMediaPreview(File mediaFile) {
        videoSource = Uri.fromFile(mediaFile);

        shutdownMediaPlayer();
        prepareMediaPlayer();
    }
}
