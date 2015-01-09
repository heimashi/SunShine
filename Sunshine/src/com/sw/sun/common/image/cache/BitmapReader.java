
package com.sw.sun.common.image.cache;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.sw.sun.common.image.cache.DiskLruCache.Snapshot;
import com.sw.sun.common.logger.MyLog;

public class BitmapReader {

    public Bitmap read(final DiskLruCache diskLruCache, final String diskKey, int width,
            int height, Config config) {
        synchronized (diskLruCache) {
            Bitmap result = null;
            try {
                result = this.readWithError(diskLruCache, diskKey, width, height, config);
            } catch (IOException e) {
                MyLog.e("BitmapReader.read ", e);
            }
            return result;
        }
    }

    public Bitmap readWithError(final DiskLruCache diskLruCache, final String diskKey, int width,
            int height, Config config) throws OutOfMemoryError, IOException {
        Snapshot s = null;
        InputStream is = null;
        Bitmap result = null;
        try {
            s = diskLruCache.get(diskKey);
            is = null != s ? s.getInputStream(0) : null;
            if (null == is) {
                result = null;
            } else {
                if ((0 == width) || (0 == height)) {
                    result = BitmapFactory.decodeStream(is);
                } else {
                    // First decode with inJustDecodeBounds=true to check
                    // dimensions
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(is, null, options);
                    ImageCacheUtils.closeQuietly(s);
                    ImageCacheUtils.closeQuietly(is);
                    s = diskLruCache.get(diskKey);
                    is = null != s ? s.getInputStream(0) : null;
                    if (null == is) {
                        result = null;
                    } else {
                        // Calculate inSampleSize
                        options.inSampleSize = calculateInSampleSize(options, width, height);
                        // Decode bitmap with inSampleSize set
                        options.inJustDecodeBounds = false;
                        options.inPreferredConfig = config;
                        result = BitmapFactory.decodeStream(is, null, options);
                    }
                }
            }
            return result;
        } finally {
            ImageCacheUtils.closeQuietly(s);
            ImageCacheUtils.closeQuietly(is);
        }
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options}
     * object when decoding bitmaps using the decode* methods from
     * {@link BitmapFactory}. This implementation calculates the closest
     * inSampleSize that will result in the final decoded bitmap having a width
     * and height equal to or larger than the requested width and height. This
     * implementation does not ensure a power of 2 is returned for inSampleSize
     * which can be faster when decoding but results in a larger bitmap which
     * isn't as useful for caching purposes.
     * 
     * @param options An options object with out* params already populated (run
     *            through a decode* method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        int inSampleSize = 1;
        if ((height > reqHeight) || (width > reqWidth)) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).
            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while ((totalPixels / (inSampleSize * inSampleSize)) > totalReqPixelsCap) {
                inSampleSize++;
            }

        }
        return inSampleSize;
    }
}
