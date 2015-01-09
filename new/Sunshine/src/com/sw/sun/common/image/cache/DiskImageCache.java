/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sw.sun.common.image.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import com.sw.sun.common.image.cache.DiskLruCache.Editor;
import com.sw.sun.common.image.cache.DiskLruCache.Snapshot;
import com.sw.sun.common.logger.MyLog;

/**
 * A simple disk LRU bitmap cache to illustrate how a disk cache would be used
 * for bitmap caching. A much more robust and efficient disk LRU cache solution
 * can be found in the ICS source code
 * (libcore/luni/src/main/java/libcore/io/DiskLruCache.java) and is preferable
 * to this simple implementation.
 */
public class DiskImageCache {
    private static final String TAG = "DiskImageCache";

    private final File mCacheDir;

    private CompressFormat mCompressFormat = CompressFormat.JPEG;

    private int mCompressQuality = 80;

    private DiskLruCache mDiskLruCache = null;

    private BitmapReader mBitmapReader;

    /**
     * Used to get the instance of DiskLruCache.
     * 
     * @return
     */
    public DiskLruCache getDiskLruCache() {
        return mDiskLruCache;
    }

    public File getDiskCacheDir() {
        return mCacheDir;
    }

    public CompressFormat getCompressFormat() {
        return mCompressFormat;
    }

    public int getCompressQuality() {
        return mCompressQuality;
    }

    /**
     * Used to fetch an instance of DiskImageCache.
     * 
     * @param context
     * @param cacheDir
     * @param maxByteSize
     * @return
     */
    public static DiskImageCache openCache(final Context context, final File cacheDir,
            long maxByteSize) {
        try {
            if (!cacheDir.exists() || !cacheDir.isDirectory()) {
                cacheDir.mkdirs();
            }
            if (cacheDir.isDirectory() && cacheDir.canWrite()) {
                long useableDirSpace = ImageCacheUtils.getUsableSpace(cacheDir);
                maxByteSize = Math.min(useableDirSpace / 3, maxByteSize);
                return new DiskImageCache(cacheDir, Math.max(maxByteSize, 1));
            }
        } catch (final IOException e) {
            MyLog.e(TAG + " Error in openCache: ", e);
        }
        return null;
    }

    /**
     * Constructor that should not be called directly, instead use
     * {@link DiskImageCache#openCache(Context, File, long)} which runs some
     * extra checks before creating a DiskLruCache instance.
     * 
     * @param cacheDir
     * @param maxByteSize
     * @throws IOException
     */
    private DiskImageCache(final File cacheDir, final long maxByteSize) throws IOException {
        mCacheDir = cacheDir;
        mDiskLruCache = DiskLruCache.open(mCacheDir, 1, 1, maxByteSize);
        mBitmapReader = new BitmapReader();
    }

    public void put(final String key, final String filePath) {
        Editor ed = null;
        OutputStream os = null;

        try {
            ed = mDiskLruCache.edit(key);
            if (null != ed) {
                os = ed.newOutputStream(0);
                if (writeFile(filePath, os)) {
                    ed.commit();
                    ed = null;
                }
            }
        } catch (final IOException e) {
            MyLog.e(TAG + " Error in put: ", e);
        } finally {
            ImageCacheUtils.closeQuietly(os);
            if (null != ed) {
                try {
                    ed.abort();
                } catch (final IOException e) {
                    MyLog.e(TAG + " Error in put (abort): ", e);
                }
            }
        }
    }

    /**
     * Add a bitmap to the disk cache.
     * 
     * @param key A unique identifier for the bitmap.
     * @param data The bitmap to store.
     */
    public void put(final String key, final Bitmap data) {
        put(key, data, mCompressFormat, mCompressQuality);
    }

    /**
     * Add a bitmap to the disk cache.
     * 
     * @param key A unique identifier for the bitmap.
     * @param data The bitmap to store.
     */
    public void put(final String key, final Bitmap data, CompressFormat compressFormat, int quility) {
        Editor ed = null;
        OutputStream os = null;
        try {
            ed = mDiskLruCache.edit(key);
            if (null != ed) {
                os = ed.newOutputStream(0);
                boolean successful = writeBitmap(data, os, compressFormat, quility);
                if (successful) {
                    ed.commit();
                    ed = null;
                }
            }
        } catch (final IOException e) {
            MyLog.e(TAG + " Error in put: ", e);
        } finally {
            ImageCacheUtils.closeQuietly(os);
            if (null != ed) {
                try {
                    ed.abort();
                } catch (final IOException e) {
                    MyLog.e(TAG + " Error in put (abort): ", e);
                }
            }
        }
    }

    /**
     * Get an image from the disk cache.
     * 
     * @param key The unique identifier for the bitmap
     * @return The bitmap or null if not found the method may throws OOM while
     *         decoding the image.
     */
    public Bitmap get(final String key, int width, int height, Config config) {
        Snapshot s = null;
        Bitmap result = null;
        try {
            result = mBitmapReader.read(this.mDiskLruCache, key, width, height, config);
        } finally {
            if (null != s) {
                s.close();
            }
        }
        return result;
    }

    /**
     * Get an image from the disk cache.
     * 
     * @param key The unique identifier for the bitmap
     * @return The bitmap or null if not found
     */
    public Bitmap getWithError(final String key, int width, int height, Config config)
            throws OutOfMemoryError, IOException {
        return mBitmapReader.readWithError(this.mDiskLruCache, key, width, height, config);
    }

    public static Bitmap getBitmap(Snapshot snapshot) {
        if (snapshot == null || snapshot.getInputStream(0) == null) {
            return null;
        }

        InputStream is = snapshot.getInputStream(0);
        return BitmapFactory.decodeStream(is);
    }

    /**
     * Removes all disk cache entries from this instance cache dir
     */
    public void clearCache() {
        try {
            mDiskLruCache.delete();
        } catch (final IOException e) {
            MyLog.e(TAG + " Error in clearCache: ", e);
        }
    }

    /**
     * Sets the target compression format and quality for images written to the
     * disk cache.
     * 
     * @param compressFormat
     * @param quality
     */
    public void setCompressParams(final CompressFormat compressFormat, final int quality) {
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
    }

    /**
     * Writes a bitmap to a file. Call
     * {@link DiskImageCache#setCompressParams(CompressFormat, int)} first to
     * set the target bitmap compression and format.
     * 
     * @param bitmap
     * @param out
     * @return
     * @throws IOException
     */
    private boolean writeBitmap(final Bitmap bitmap, final OutputStream out) throws IOException {
        try {
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Writes a bitmap to a file. Call
     * {@link DiskImageCache#setCompressParams(CompressFormat, int)} first to
     * set the target bitmap compression and format.
     * 
     * @param bitmap
     * @param out
     * @return
     * @throws IOException
     */
    private boolean writeBitmap(final Bitmap bitmap, final OutputStream out,
            CompressFormat compressFormat, int quility) throws IOException {
        try {
            return bitmap.compress(compressFormat, quility, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private boolean writeFile(final String filePath, final OutputStream out) {

        File f = new File(filePath);
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = in.read(buffer)) != -1)
                out.write(buffer, 0, bytes_read);
        } catch (FileNotFoundException e) {
            MyLog.e(TAG + " file not found: ", e);
            return false;
        } catch (IOException e) {
            MyLog.e(TAG + " IOException: ", e);
            return false;
        } finally {
            if (in != null) {
                ImageCacheUtils.closeQuietly(in);
            }
            if (out != null) {
                ImageCacheUtils.closeQuietly(out);
            }
        }

        return true;
    }
}
