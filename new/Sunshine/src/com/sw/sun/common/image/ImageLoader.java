
package com.sw.sun.common.image;

import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.TextUtils;

import com.sw.sun.common.logger.MyLog;

/**
 * Define a helper class to load images from both internal and external storage.
 */
public final class ImageLoader {

    private static final String TAG = "ImageLoader";

    public static BitmapDrawable getDrawable(Bitmap bitmap) {
        return bitmap == null ? null : new BitmapDrawable(Resources.getSystem(), bitmap);
    }

    public static BitmapFactory.Options getDefaultOptions() {
        return getDefaultOptions(Bitmap.Config.RGB_565);
    }

    public static BitmapFactory.Options getDefaultOptions(Config config) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inDither = false;
        opt.inJustDecodeBounds = false;
        opt.inPreferredConfig = config;
        // opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opt.inSampleSize = 1;
        opt.inScaled = false;
        return opt;
    }

    public static int computeSampleSize(BitmapFactory.Options options, int pixelSize) {
        double size = Math.sqrt((double) options.outWidth * options.outHeight / pixelSize);
        int roundedSize = 1;
        while (roundedSize * 2 <= size) {
            roundedSize <<= 1;
        }
        return roundedSize;
    }

    public static int computeSampleSize(InputStreamLoader streamLoader, int pixelSize) {
        BitmapFactory.Options options = getBitmapSize(streamLoader);
        double size = Math.sqrt((double) options.outWidth * options.outHeight / pixelSize);
        int roundedSize = 1;
        while (roundedSize * 2 <= size) {
            roundedSize <<= 1;
        }
        return roundedSize;
    }

    public static int computeSampleSize(int resId, int pixelSize, Context context) {
        BitmapFactory.Options options = getBitmapSize(resId, context);
        double size = Math.sqrt((double) options.outWidth * options.outHeight / pixelSize);
        int roundedSize = 1;
        while (roundedSize * 2 <= size) {
            roundedSize <<= 1;
        }
        return roundedSize;
    }

    public static Bitmap getBitmap(String path) {
        return getBitmap(path, BaseMeta.PIXEL_SIZE_RAW);
    }

    public static final Bitmap getBitmap(Context context, Uri uri, int pixelSize) {
        if (uri == null) {
            return null;
        }
        return getBitmap(new InputStreamLoader(context, uri), pixelSize);
    }

    public static Bitmap getBitmap(String path, int pixelSize) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        return getBitmap(new InputStreamLoader(path), pixelSize);
    }

    public static Bitmap getBitmap(String path, int pixelSize, Config config) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        return getBitmap(new InputStreamLoader(path), pixelSize, config);
    }

    public static Bitmap getBitmap(String path, int pixelSize, BitmapFactory.Options options) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        return getBitmap(new InputStreamLoader(path), pixelSize, options);
    }

    public static final Bitmap getBitmap(InputStreamLoader streamLoader, int pixelSize) {
        return getBitmap(streamLoader, pixelSize, ImageLoader.getDefaultOptions());
    }

    public static final Bitmap getBitmap(InputStreamLoader streamLoader, int pixelSize,
            Config config) {
        return getBitmap(streamLoader, pixelSize, ImageLoader.getDefaultOptions(config));
    }

    public static final Bitmap getBitmap(InputStreamLoader streamLoader, int pixelSize,
            BitmapFactory.Options options) {
        if (options.inSampleSize <= 1) {
            options.inSampleSize = computeSampleSize(streamLoader, pixelSize);
        }
        // Decode bufferedInput to a bitmap.
        Bitmap bitmap = null;
        int retry = 0;
        while (retry++ < 3) { // 如果内存不够，则加大sample size重新读取，超过3次后退出，返回null
            try {
                // Get the input stream again for decoding it to a bitmap.
                bitmap = BitmapFactory.decodeStream(streamLoader.get(), null, options);
                break;
            } catch (OutOfMemoryError ex) {
                MyLog.v(TAG + " out of memory, try to GC");
                System.gc();
                options.inSampleSize *= 2;
                MyLog.v(TAG + " try to increase sample size to " + options.inSampleSize);
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            } finally {
                streamLoader.close();
            }
        }
        return bitmap;
    }

    public static final Bitmap getBitmap(int resId, int pixelSize, Context context,
            BitmapFactory.Options options) {
        if (options.inSampleSize <= 1) {
            options.inSampleSize = computeSampleSize(resId, pixelSize, context);
        }
        // Decode bufferedInput to a bitmap.
        Bitmap bitmap = null;
        int retry = 0;
        while (retry++ < 3) { // 如果内存不够，则加大sample size重新读取，超过3次后退出，返回null
            try {
                // Get the input stream again for decoding it to a bitmap.
                InputStream is = context.getResources().openRawResource(resId);
                bitmap = BitmapFactory.decodeStream(is, null, options);
                break;
            } catch (OutOfMemoryError ex) {
                MyLog.v(TAG + " out of memory, try to GC");
                System.gc();
                options.inSampleSize *= 2;
                MyLog.v(TAG + " try to increase sample size to " + options.inSampleSize);
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            }
        }
        return bitmap;
    }

    public static final BitmapFactory.Options getBitmapSize(InputStreamLoader streamLoader) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(streamLoader.get(), null, options);
        } catch (Exception e) {
        } finally {
            streamLoader.close();
        }
        return options;
    }

    /**
     * @param resId
     * @param pixelSize 这个参数传期望输出的图片面积
     * @param context
     * @return
     */
    public static final Bitmap getBitmap(int resId, int pixelSize, Context context) {
        return getBitmap(resId, pixelSize, context, ImageLoader.getDefaultOptions());
    }

    public static final Bitmap getBitmap(int resId, int pixelSize, Context context, Config config) {
        return getBitmap(resId, pixelSize, context, ImageLoader.getDefaultOptions(config));
    }

    public static final BitmapFactory.Options getBitmapSize(int resId, Context context) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(context.getResources(), resId, options);
        } catch (Exception e) {
        }
        return options;
    }

}
