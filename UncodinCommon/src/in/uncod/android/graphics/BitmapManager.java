package in.uncod.android.graphics;

import in.uncod.android.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class BitmapManager {
    private static BitmapManager instance;
    private ImageCache mCache;
    private ConcurrentLinkedQueue<Image> mQueue = new ConcurrentLinkedQueue<Image>();
    private BitmapLoader mBitmapLoader;

    public static BitmapManager getBitmapManager(Context context) {
        if (instance == null) {
            instance = new BitmapManager(context);
        }
        return instance;
    }

    private BitmapManager(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        mCache = new ImageCache(memoryClassBytes / 8);
    }

    /**
     * Loads and scales the specified Bitmap image into an ImageView on the given Activity.
     * 
     * @param imageFilename
     *            The location of the bitmap on the filesystem
     * @param activity
     *            The Activity that contains the destination ImageView
     * @param imageView
     *            The ImageView that will display the image
     * @param maxSize
     *            Specifies the maximum width or height of the image. Images that exceed this size in either dimension
     *            will be scaled down, with their aspect ratio preserved. If -1, the image will not be scaled at all.
     */
    public void displayBitmapScaled(String imageFilename, final Activity activity, ImageView imageView,
            int maxSize) {
        if (imageFilename == null || imageFilename.equals(""))
            throw new IllegalArgumentException("imageFilename must be specified");

        if (!new File(imageFilename).exists()) {
            throw new IllegalArgumentException("imageFilename must be a real file");
        }

        Image image = new Image(activity, imageFilename, imageView, maxSize);

        // Have the ImageView remember the latest image to display
        imageView.setTag(image.getHash());

        Bitmap cachedResult = mCache.get(image.getHash());
        if (cachedResult != null) {
            setImage(image);
        }
        else {
            mQueue.add(image);
            if (mBitmapLoader == null || !mBitmapLoader.isAlive()) {
                mBitmapLoader = new BitmapLoader();
                mBitmapLoader.start();
            }
        }
    }

    private void setImage(final Image image) {
        final Bitmap bitmap = mCache.get(image.getHash());
        if (bitmap != null) {
            image.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    image.getImageView().setImageBitmap(bitmap);
                }
            });
        }
    }

    // http://stackoverflow.com/a/3549021/136408
    public static Bitmap loadBitmapScaled(File f, int maxSize) {
        Bitmap b = null;
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            FileInputStream fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();

            int scale = 1;
            if (o.outHeight > maxSize || o.outWidth > maxSize) {
                scale = (int) Math.pow(
                        2,
                        (int) Math.round(Math.log(maxSize / (double) Math.max(o.outHeight, o.outWidth))
                                / Math.log(0.5)));
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis, null, o2);
            fis.close();
        }
        catch (IOException e) {
        }
        return b;
    }

    private class Image {
        private File imageLocation;
        private String hash;
        private ImageView imageView;
        private Activity activity;
        private int maxSize;

        public Image(Activity activity, String imageLocation, ImageView imageView, int maxSize) {
            this.imageLocation = new File(imageLocation);
            this.hash = Util.md5(imageLocation + maxSize);
            this.imageView = imageView;
            this.activity = activity;
            this.maxSize = maxSize;
        }

        public File getImageLocation() {
            return imageLocation;
        }

        public String getHash() {
            return hash;
        }

        public ImageView getImageView() {
            return imageView;
        }

        public Activity getActivity() {
            return activity;
        }

        public int getMaxSize() {
            return maxSize;
        }
    }

    private class BitmapLoader extends Thread {
        @Override
        public void run() {
            while (!mQueue.isEmpty()) {
                Image image = mQueue.poll();

                if (!image.getImageView().getTag().equals(image.getHash()))
                    continue; // Don't bother loading image since we don't want it in this view anymore

                Bitmap b;

                if (image.getMaxSize() == -1) {
                    b = BitmapFactory.decodeFile(image.getImageLocation().getAbsolutePath());
                }
                else {
                    b = loadBitmapScaled(image.getImageLocation(), image.getMaxSize());
                }
                if (image.getHash() != null && b != null) {
                    mCache.put(image.getHash(), b);
                }

                setImage(image);
            }
        }
    }
}
