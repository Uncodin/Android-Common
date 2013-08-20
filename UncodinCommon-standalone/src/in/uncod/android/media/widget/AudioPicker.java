package in.uncod.android.media.widget;

import in.uncod.android.R;

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * A simple audio picker
 */
public class AudioPicker extends AbstractMediaPickerFragment implements OnClickListener {
    private static final int REQCODE_GET_AUDIO = 0;

    private AudioPlayerView mAudioPlayerView;
    private Button mSelectAudioButton;

    OnAudioChangedListener mOnAudioChangedListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layoutRoot = (LinearLayout) inflater.inflate(R.layout.audio_picker, container, false);

        mAudioPlayerView = (AudioPlayerView) layoutRoot.findViewById(R.id.audio_player);

        mSelectAudioButton = (Button) layoutRoot.findViewById(R.id.select_audio_button);
        mSelectAudioButton.setOnClickListener(this);

        return layoutRoot;
    }

    @Override
    public void onClick(View view) {
        if (view == mSelectAudioButton) {
            launchAudioPicker();
        }
    }

    /**
     * Publishes an intent to get an audio file
     */
    public void launchAudioPicker() {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent = Intent.createChooser(intent, "Select an audio source");
        startActivityForResult(intent, REQCODE_GET_AUDIO);
    }

    public void updatePreviewAudio(String path) {
        mAudioPlayerView.setMediaLocation(path);
    }

    public void setOnAudioChangedListener(OnAudioChangedListener listener) {
        mOnAudioChangedListener = listener;
    }

    public interface OnAudioChangedListener {
        public File audioChanged(Uri audioUri);
    }

    @Override
    protected File mediaChanged(Uri mediaUri) {
        if (mOnAudioChangedListener != null) {
            return mOnAudioChangedListener.audioChanged(mediaUri);
        }
        return null;
    }

    @Override
    protected String getProgressTitle() {
        return "Loading Audio";
    }

    @Override
    public void updateMediaPreview(File mediaFile) {
        mAudioPlayerView.setMediaLocation(mediaFile.getAbsolutePath());
    }

    public void setEnabled(boolean isEnabled) {
        if (mAudioPlayerView != null) {
            mAudioPlayerView.setEnabled(isEnabled);
        }
        if (mSelectAudioButton != null) {
            mSelectAudioButton.setEnabled(isEnabled);
        }
    }
}
