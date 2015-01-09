package com.sw.sun.common.image.cache;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.support.v4.util.LruCache;

import com.sw.sun.common.android.GlobalData;

/**
 * This class holds our bitmap caches (memory and disk).
 */
public class ImageCache {

    public static final String MY_IMAGE_CACHE_DIR = Environment.getExternalStorageDirectory() + "/sun/cache/image/";

    public static final int SOFTREFERENCE_ELEMENT_SIZE = 16;

    private static final String TAG = "ImageCache";

    // Default memory cache size
    private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 5;// SOFTREFERENCE_ELEMENT_SIZE * 60; //

    // Default disk cache size
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 50; // 100MB

    // Compression settings when writing images to disk cache
    private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 80;

    // Constants to easily toggle various caches
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
    private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
    private static final boolean DEFAULT_CLEAR_DISK_CACHE_ON_START = false;

    private DiskImageCache mDiskCache;
    private LruCache<String, Bitmap> mMemoryCache;
    private ImageCacheParams mImageCacheParams;

    public ImageCache() {
    }

    /**
     * Creating a new ImageCache object using the specified parameters.
     * 
     * @param context The context to use
     * @param cacheParams The cache parameters to use to initialize the cache
     */
    public ImageCache(final Context context, final ImageCacheParams cacheParams) {
        init(context, cacheParams);
    }

    /**
     * Creating a new ImageCache object using the default parameters.
     * 
     * @param context The context to use
     * @param uniqueName A unique name that will be appended to the cache directory
     */
    public ImageCache(final Context context, final String uniqueName) {
        init(context, new ImageCacheParams(uniqueName));
    }

    private int imageCacheSize = DEFAULT_MEM_CACHE_SIZE;

    public int getCacheSize() {
        return this.imageCacheSize;
    }

    /**
     * Initialize the cache, providing all parameters.
     * 
     * @param context The context to use
     * @param cacheParams The cache parameters to initialize the cache
     */
    private void init(final Context context, final ImageCacheParams cacheParams) {
        mImageCacheParams = cacheParams;

        // Set up memory cache
        if (cacheParams.memoryCacheEnabled) {
            this.imageCacheSize = cacheParams.memCacheSize;
            mMemoryCache = new LruCache<String, Bitmap>(cacheParams.memCacheSize) {
                /**
                 * Measure item size in bytes rather than units which is more practical for a bitmap cache
                 */
                @Override
                protected int sizeOf(final String key, final Bitmap bitmap) {
                    if (bitmap == null) {
                        return 0;
                    }
                    // return SOFTREFERENCE_ELEMENT_SIZE;
                    return ImageCacheUtils.getBitmapSize(bitmap);
                }

            };
        }
    }

    public void addBitmapToMemCache(final String key, final Bitmap bitmap) {
        // MyLog.v("addBitmapToMemCache key=" + key + ",bitmap=" + bitmap);
        if ((key == null) || (bitmap == null)) {
            return;
        }
        // Add to memory cache
        if (mMemoryCache != null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public void addFileToDiskCache(final String key, final String filePath) {
        if ((key == null) || (filePath == null)) {
            return;
        }
        // Add to disk cache
        if (getDiskImageCache() != null) {
            getDiskImageCache().put(key, filePath);
        }
    }

    public void addBitmapToDiskCache(final String key, final Bitmap bitmap) {
        if ((key == null) || (bitmap == null)) {
            return;
        }
        // Add to disk cache
        if (getDiskImageCache() != null) {
            getDiskImageCache().put(key, bitmap);
        }
    }

    public void addBitmapToDiskCache(final String key, final Bitmap bitmap, CompressFormat compressFormat, int quility) {
        if ((key == null) || (bitmap == null)) {
            return;
        }
        // Add to disk cache
        if (getDiskImageCache() != null) {
            getDiskImageCache().put(key, bitmap, compressFormat, quility);
        }
    }

    /**
     * Get from memory cache.
     * 
     * @param data Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    public Bitmap getBitmapFromMemCache(final String key) {

        // MyLog.v("getBitmapFromMemCache key=" + key);
        if (mMemoryCache != null) {
            final Bitmap memBitmap = mMemoryCache.get(key);
            if (memBitmap != null && !memBitmap.isRecycled()) {
                return memBitmap;
            } else if (memBitmap != null && memBitmap.isRecycled()) {
                mMemoryCache.remove(key);
            }
        }
        return null;
    }

    /**
     * Get from disk cache. it may throw OOM while decoding the bitmap.
     * 
     * @param data Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    public Bitmap getBitmapFromDiskCache(final String key, int width, int height, Config config) {
        if (getDiskImageCache() != null) {
            Bitmap bm = getDiskImageCache().get(key, width, height, config);
            return bm;
        }
        return null;
    }

    /**
     * Get from disk cache.
     * 
     * @param data Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    public Bitmap getBitmapFromDiskCacheWithError(final String key, int width, int height, Config config)
            throws OutOfMemoryError, IOException {
        if (getDiskImageCache() != null) {
            Bitmap bm = getDiskImageCache().getWithError(key, width, height, config);
            return bm;
        }
        return null;
    }

    public synchronized DiskImageCache getDiskImageCache() {
        if (mDiskCache != null) {
            return mDiskCache;
        }
        if (mImageCacheParams.diskCacheEnabled) {
            final File diskCacheDir = getDiskCacheDir(GlobalData.app(), mImageCacheParams.uniqueName);

            // Set up disk cache
            if (mImageCacheParams.diskCacheEnabled) {
                mDiskCache = DiskImageCache.openCache(GlobalData.app(), diskCacheDir, mImageCacheParams.diskCacheSize);
                if (mDiskCache != null) {
                    mDiskCache.setCompressParams(mImageCacheParams.compressFormat, mImageCacheParams.compressQuality);
                    if (!mImageCacheParams.clearDiskCacheOnStart) {
                        try {
                            mDiskCache.getDiskLruCache().flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // mDiskCache.clearCache();
                    }
                }
            }
            return mDiskCache;
        } else {
            return null;
        }
    }

    public DiskLruCache getDiskLruCache() {
        return null == getDiskImageCache() ? null : getDiskImageCache().getDiskLruCache();
    }

    /**
     * Clears both the memory cache and the disk cache.
     */
    public void clearCaches() {
        if (getDiskImageCache() != null) {
            // getDiskImageCache().clearCache();
        }
        mMemoryCache.evictAll();
    }

    /**
     * Clears the memory cache.
     */
    public void clearMemCache() {
        mMemoryCache.evictAll();
    }

    /**
     * Clears the memory cache.
     */
    public void trimMemCache(int trimedSize) {
        mMemoryCache.trimToSize(trimedSize);
    }

    public LruCache<String, Bitmap> getMemoryCache() {
        return mMemoryCache;
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise).
     * 
     * @param context The context to use
     * @param uniqueName A unique directory name to append to the cache dir
     * @return The cache dir
     */
    private static File getDiskCacheDir(final Context context, final String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath = (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) ||
                !ImageCacheUtils.isExternalStorageRemovable() ?
                MY_IMAGE_CACHE_DIR :
                context.getCacheDir().getPath();
        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * A holder class that contains cache parameters.
     */
    public static class ImageCacheParams {
        public String uniqueName;
        public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;
        public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
        public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
        public int compressQuality = DEFAULT_COMPRESS_QUALITY;
        public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;
        public boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;
        public boolean clearDiskCacheOnStart = DEFAULT_CLEAR_DISK_CACHE_ON_START;

        public ImageCacheParams(final String uniqueName, int memCacheSize) {
            this.uniqueName = uniqueName;
            this.memCacheSize = memCacheSize;
        }

        public ImageCacheParams(final String uniqueName) {
            this(uniqueName, DEFAULT_MEM_CACHE_SIZE);
        }
    }
}
