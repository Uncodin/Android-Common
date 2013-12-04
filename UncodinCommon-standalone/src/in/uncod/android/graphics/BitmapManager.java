package in.uncod.android.graphics;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import in.uncod.android.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.npi.blureffect.Blur;

public class BitmapManager {
    public static final double MIN_MEMORY_FACTOR = .125;
    public static final double MAX_MEMORY_FACTOR = .5;

    /**
     * Interface for informing objects of the image loading process
     */
    public interface OnBitmapLoadedListener {
        /**
         * Called when an image is finished loading
         * <p/>
         * Note: not guaranteed to execute on the UI thread
         * 
         * @param cached
         *            true if the image was available in the cache
         */
        void onImageLoaded(boolean cached);

        /**
         * Called before the loading process is started for an image
         * <p/>
         * Note: not guaranteed to execute on the UI thread
         * 
         * @param cached
         *            true if the image is available in the cache
         */
        void beforeImageLoaded(boolean cached);
    }

    Handler mHandler;
    private static BitmapManager instance;
    private static double mMemoryFactor;
    private ImageCache mCache;
    private ConcurrentLinkedQueue<Image> mQueue = new ConcurrentLinkedQueue<Image>();
    private BitmapLoader mBitmapLoader;
    private Context mApplicationContext;

    /**
     * Gets a BitmapManager with a memory factor of at least 1/8.
     * <p/>
     * If a previous BitmapManager was created with a larger memory factor, it will be retrieved instead.
     * 
     * @param context
     *            The Context to associate with the BitmapManager
     * @return A BitmapManager instance
     */
    public static BitmapManager get(Context context) {
        return get(context, MIN_MEMORY_FACTOR);
    }

    /**
     * Gets a BitmapManager with a minimum specified memory factor
     * 
     * @param context
     *            The context to associate with the BitmapManager
     * @param memoryFactor
     *            The portion of memory the cache will be allowed to allocate. Valid values are in the range [.125, .5].
     *            The BitmapManager instance with the largest requested memory factor is retained and will be returned
     *            unless a larger memory factor is specified.
     * @return A BitmapManager instance
     */
    public static BitmapManager get(Context context, double memoryFactor) {

        if (memoryFactor > MAX_MEMORY_FACTOR) {
            memoryFactor = MAX_MEMORY_FACTOR;
        }
        else if (memoryFactor < MIN_MEMORY_FACTOR) {
            memoryFactor = MIN_MEMORY_FACTOR;
        }

        if (mMemoryFactor < memoryFactor) {
            mMemoryFactor = memoryFactor;

            instance = new BitmapManager(context, memoryFactor);
            instance.mHandler = new Handler();
        }

        return instance;
    }

    private BitmapManager(Context context, double memoryFactor) {
        mApplicationContext = context.getApplicationContext();

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        mCache = new ImageCache((int) (memoryClassBytes * memoryFactor));
    }

    /**
     * Loads and scales the specified Bitmap image into an ImageView on the given Activity.
     * 
     * @param imageFilename
     *            The location of the bitmap on the filesystem
     * @param imageView
     *            The ImageView that will display the image
     * @param maxSize
     *            Specifies the maximum width or height of the image. Images that exceed this size in either dimension
     *            will be scaled down, with their aspect ratio preserved. If -1, the image will not be scaled at all.
     */
    public void displayBitmapScaled(String imageFilename, ImageView imageView, int maxSize) {
        displayBitmapScaled(imageFilename, imageView, maxSize, null);
    }

    /**
     * Loads and scales the specified Bitmap image into an ImageView on the given Activity.
     * 
     * @param imageFilename
     *            The location of the bitmap on the filesystem
     * @param imageView
     *            The ImageView that will display the image
     * @param maxSize
     *            Specifies the maximum width or height of the image. Images that exceed this size in either dimension
     *            will be scaled down, with their aspect ratio preserved. If -1, the image will not be scaled at all.
     * @param bitmapLoadedListener
     *            If not null, this listener will be notified after the image bitmap is updated
     */
    public void displayBitmapScaled(String imageFilename, ImageView imageView, int maxSize,
            OnBitmapLoadedListener bitmapLoadedListener) {
        displayBitmapScaled(imageFilename, imageView, maxSize, false, bitmapLoadedListener);
    }

    public void displayBitmapScaled(String imageFilename, ImageView imageView, int maxSize,
                                    boolean blurred, OnBitmapLoadedListener bitmapLoadedListener) {
        if (imageFilename == null || imageFilename.equals(""))
            throw new IllegalArgumentException("imageFilename must be specified");

        if (!new File(imageFilename).exists()) {
            throw new IllegalArgumentException("imageFilename must be a real file");
        }

        Image image = new Image(imageFilename, imageView, maxSize, blurred, bitmapLoadedListener);

        // Have the ImageView remember the latest image to display
        imageView.setTag(image.getHash());

        Bitmap cachedResult = mCache.get(image.getHash());
        if (cachedResult != null) {
            // Notify listener
            if (bitmapLoadedListener != null) {
                bitmapLoadedListener.beforeImageLoaded(true);
            }

            setImage(image);

            // Notify listener
            if (bitmapLoadedListener != null) {
                bitmapLoadedListener.onImageLoaded(true);
            }
        }
        else {
            // Notify listener
            if (bitmapLoadedListener != null) {
                bitmapLoadedListener.beforeImageLoaded(false);
            }

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
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    image.getImageView().setImageBitmap(bitmap);
                }
            });
        }
    }

    public static Bitmap loadBitmapScaled(File f, int maxSize) throws OutOfMemoryError {
        int orientation = 0;
        try {
            ExifInterface exif = new ExifInterface(f.toString());
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return loadBitmapScaled(f, maxSize, orientation);
    }

    // http://stackoverflow.com/a/3549021/136408
    public static Bitmap loadBitmapScaled(File f, int maxSize, int orientation) throws OutOfMemoryError {
        Log.d("BitmapLoader", "MaxSize: "+ maxSize);
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
            try {
                b = BitmapFactory.decodeStream(fis, null, o2);
            }
            catch (OutOfMemoryError e) {
                fis.close();
                throw new OutOfMemoryError();
            }
            fis.close();

            if (orientation > 0) {

                switch (orientation) {
                case 3:
                    orientation = 180;
                    break;
                case 6:
                    orientation = 90;
                    break;
                case 8:
                    orientation = 270;
                    break;
                default:
                    return b;
                }

                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                Bitmap rotatedBitmap = null;
                try {
                    rotatedBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                }
                catch (OutOfMemoryError e) {
                    b.recycle();
                    b = null;
                    throw new OutOfMemoryError();
                }
                b.recycle();

                return rotatedBitmap;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return b;
    }

    private class Image {
        private File imageLocation;
        private String hash;
        private ImageView imageView;
        private int maxSize;
        private OnBitmapLoadedListener runnable;
        private boolean blurred;
        private String unblurredHash;

        public Image(String imageLocation, ImageView imageView, int maxSize, boolean blurred,
                OnBitmapLoadedListener runAfterImageUpdated) {
            this.imageLocation = new File(imageLocation);
            this.hash = Util.md5(imageLocation + maxSize) + ((blurred) ? "blur" : "");
            this.unblurredHash = Util.md5(imageLocation + maxSize);
            this.imageView = imageView;
            this.maxSize = maxSize;
            this.runnable = runAfterImageUpdated;
            this.blurred = blurred;
        }

        public OnBitmapLoadedListener getListener() {
            return runnable;
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

        public int getMaxSize() {
            return maxSize;
        }

        public boolean getBlurred() { return blurred; }
    }

    private class BitmapLoader extends Thread {
        @Override
        public void run() {
            while (!mQueue.isEmpty()) {
                Image image = mQueue.poll();

                if (!image.getImageView().getTag().equals(image.getHash()))
                    continue; // Don't bother loading image since we don't want it in this view anymore

                Bitmap b = null;

                if (image.getMaxSize() == -1) {
                    try {
                        b = BitmapFactory.decodeFile(image.getImageLocation().getAbsolutePath());
                    }
                    catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        mCache.freeSpace();
                    }
                }
                else {

                    int orientation = 0;

                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(image.getImageLocation().toString());
                        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        b = loadBitmapScaled(image.getImageLocation(), image.getMaxSize(), orientation);
                    }
                    catch (OutOfMemoryError e) {
                        mCache.freeSpace();
                    }
                }

                if (image.getHash() != null && b != null) {
                    if (image.getBlurred()) {
                        b = Blur.fastblur(mApplicationContext, b, 12);
                    }
                    mCache.put(image.getHash(), b);
                }

                setImage(image);

                // Notify listener
                if (image.getListener() != null) {
                    image.getListener().onImageLoaded(false);
                }
            }
        }
    }
}
