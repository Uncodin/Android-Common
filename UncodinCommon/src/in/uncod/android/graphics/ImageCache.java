package in.uncod.android.graphics;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * An implementation of LruCache for storing Bitmaps with String keys
 */
public class ImageCache extends LruCache<String, Bitmap> {

    int maxSize;

    public ImageCache(int maxSize) {
        super(maxSize);

        this.maxSize = maxSize;
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    public void freeSpace() {
        trimToSize(maxSize / 2);
    }
}
