
package com.sw.sun.common.image.cache;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.widget.ImageView;

import com.sw.sun.common.android.CommonUtils;
import com.sw.sun.common.android.DisplayUtils;
import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.image.BaseMeta;
import com.sw.sun.common.image.ImageLoader;
import com.sw.sun.common.image.cache.DiskLruCache.Editor;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.common.network.FileDownloader;
import com.sw.sun.common.thread.ThreadPoolManager;

public class HttpImage extends BaseImage {

    private static final LruCache<String, String> sUrl2keyMap = new LruCache<String, String>(10);

    protected boolean useRawImage = false;

    protected String path = "";

    protected String url = "";

    protected String fullSizeUrl = "";

    protected int width = DisplayUtils.dip2px(75);

    protected int height = DisplayUtils.dip2px(75);

    protected Bitmap.Config config;

    protected boolean getFromHttp = false;

    protected OnImageLoadCompletedListener imageLoadCompletedListener;

    protected DISKEY_TYPE disKeyType = DISKEY_TYPE.TYPE_URL;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setWidth(int w) {
        this.width = w;
    }

    public void setHeight(int h) {
        this.height = h;
    }

    public void setUseRawImage(boolean useRawImage) {
        this.useRawImage = useRawImage;
    }

    public static enum DISKEY_TYPE {
        TYPE_URL, TYPE_PATH
    }

    public HttpImage() {
    }

    public HttpImage(String url) {
        this(url, null);
    }

    public HttpImage(String url, String fullSizeUrl) {
        this(url, fullSizeUrl, null);
    }

    public HttpImage(String url, String fullSizeUrl, Bitmap.Config config) {
        this.url = url;
        this.config = config;
        this.fullSizeUrl = fullSizeUrl;
    }

    public void init(ImageCache imageCache) {
        if (null != imageCache) {
            String localPath = imageCache.getDiskLruCache().getCacheFilePath(getDiskCacheKey());
            if (TextUtils.isEmpty(localPath)) {
                getFromHttp = true;
            }
        }
    }

    public void setDiskeyType(DISKEY_TYPE type) {
        this.disKeyType = type;
    }

    @Override
    public String getMemCacheKey() {
        StringBuilder key = new StringBuilder();
        switch (this.disKeyType) {
            case TYPE_PATH:
                key.append(getPath());
                break;
            case TYPE_URL:/* fall through */
            default:
                key.append(url);
                break;

        }
        key.append("#").append((null == filter ? "" : filter.getId())).append("width")
                .append(width).append("height").append(height);
        return key.toString();
    }

    protected String getPath() {
        if (TextUtils.isEmpty(path)) {
            try {
                URL urlObj = new URL(url);
                path = urlObj.getPath();
                MyLog.v("url=" + url + ",path=" + path);
            } catch (MalformedURLException e) {
                MyLog.e(e);
            }
        }
        return path;
    }

    @Override
    public String getDiskCacheKey() {
        switch (this.disKeyType) {
            case TYPE_PATH: {
                return diskKey(getPath());
            }
            case TYPE_URL:
            default: {
                return diskKey(url);
            }
        }
    }

    /**
     * 返回大图url在本地的diskCacheKey
     * 
     * @return
     */
    public String getFullImgDiskCacheKey() {
        if (!TextUtils.isEmpty(fullSizeUrl)) {
            return diskKey(fullSizeUrl);
        }
        return "";
    }

    public String getTmpDiskCacheKey() {
        return getDiskCacheKey() + ".tmp";
    }

    protected Bitmap getBmpFromFullCacheFile(ImageCache imageCache) {
        String fullImgDiskCacheKey = getFullImgDiskCacheKey();
        if (!TextUtils.isEmpty(fullImgDiskCacheKey)) {
            return imageCache.getBitmapFromDiskCache(fullImgDiskCacheKey, width, height, config);
        }
        return null;
    }

    protected Bitmap getBmpFromDiskCache(ImageCache imageCache) {
        Bitmap result = null;
        result = imageCache.getBitmapFromDiskCache(getDiskCacheKey(), width, height, config);
        if (null == result && !TextUtils.isEmpty(fullSizeUrl)) {
            result = getBmpFromFullCacheFile(imageCache);
        }
        return result;
    }

    @Override
    public Bitmap getBitmap(ImageCache imageCache) {
        if (!CommonUtils.isValidUrl(url)) {
            return null;
        }
        try {
            Bitmap result = getBmpFromDiskCache(imageCache);
            if (null == result && getFromHttp) {
                result = getHttpBitmap(imageCache);
            }

            if ((null != result) && (null != filter)) {
                return filter.filter(result, GlobalData.app());
            } else {
                return result;
            }
        } catch (OutOfMemoryError e) {
            MyLog.e(e);
        }
        return null;
    }

    private boolean downloadFile(final DiskLruCache diskLruCache) {
        return downloadFile(diskLruCache, url);
    }

    private boolean downloadFile(final DiskLruCache diskLruCache, final String fileURL) {
        Editor ed = null;
        BufferedOutputStream out;
        try {
            ed = diskLruCache.edit(getDiskCacheKey());
            if (null != ed) {
                out = new BufferedOutputStream(ed.newOutputStream(0),
                        ImageCacheUtils.IO_BUFFER_SIZE);
                FileDownloader downloader = new FileDownloader(fileURL, out);
                boolean result = (downloader.downloadFile() == FileDownloader.DOWNLOAD_STATE_SUCCESS);
                if (result) {
                    ed.commit();
                    ed = null;
                    return result;
                }
            }
        } catch (final Exception e) {
            MyLog.e(" Error in downloadFile - " + fileURL + " - ", e);
        } finally {
            if (null != ed) {
                try {
                    ed.abort();
                    ed = null;
                } catch (final IOException e) {
                    MyLog.e(" Error in downloadFile - " + fileURL + " - ", e);
                }
            }
        }
        return false;
    }

    @Override
    public Bitmap getLoadingBitmap() {
        return loadingBitmap;
    }

    @Override
    public void processImageView(ImageView imageView, Bitmap bm) {
        if (null != imageLoadCompletedListener) {
            imageLoadCompletedListener.onComplete(imageView, bm);
        }
    }

    public void setOnImageLoadCompletedListener(OnImageLoadCompletedListener l) {
        imageLoadCompletedListener = l;
    }

    @Override
    public int getAsyncLoadLevel() {
        return getFromHttp ? ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_IMAGE
                : ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_LOCAL_IO;
    }

    @Override
    public Bitmap getHttpBitmap(ImageCache imageCache) {
        if (!CommonUtils.isValidUrl(url)) {
            return null;
        }
        try {
            Bitmap result = null;
            if (null == result) {
                MyLog.v(" processBitmap - " + url);

                result = getBmpFromDiskCache(imageCache);
                if (null == result) {
                    final DiskLruCache diskCache = imageCache.getDiskLruCache();
                    if (diskCache != null) {
                        // Download a bitmap, write it to a file
                        boolean success = downloadFile(diskCache);
                        if (success) {
                            if (!useRawImage) {
                                result = imageCache.getBitmapFromDiskCache(getDiskCacheKey(),
                                        width, height, config);
                            } else {
                                String filePath = imageCache.getDiskLruCache().getCacheFilePath(
                                        getDiskCacheKey());
                                result = ImageLoader.getBitmap(filePath, BaseMeta.PIXEL_SIZE_LARGE);
                            }
                        }
                    }
                }
            }

            if ((null != result) && (null != filter)) {
                return filter.filter(result, GlobalData.app());
            } else {
                return result;
            }
        } catch (OutOfMemoryError e) {
            MyLog.e(e);
            return null;
        }
    }

    @Override
    public boolean needGetFromHttp() {
        return !getFromHttp;
    }

    public String getLocalFilePath(ImageCache imageCache) {
        try {
            if (null != imageCache && !TextUtils.isEmpty(url)
                    && null != imageCache.getDiskImageCache()) {
                return imageCache.getDiskLruCache().getCacheFilePath(getDiskCacheKey());
            }
        } catch (Exception e) {
            MyLog.e(e);
        }
        return "";
    }

    public String getFullSizeLocalFilePath(ImageCache imageCache) {
        if (null != imageCache && !TextUtils.isEmpty(fullSizeUrl)
                && null != imageCache.getDiskImageCache()) {
            return imageCache.getDiskLruCache().getCacheFilePath(getFullImgDiskCacheKey());
        }
        return "";
    }

    public interface OnImageLoadCompletedListener {
        void onComplete(ImageView imageView, Bitmap bitmap);
    }

    public static String diskKey(final String url) {
        synchronized (sUrl2keyMap) {
            String cacheKey = null;
            if (TextUtils.isEmpty(url)) {
                MyLog.warn("Null url passed in");
                return "";
            } else {
                cacheKey = sUrl2keyMap.get(url);
                if (null == cacheKey) {
                    cacheKey = url.replaceAll("[.:/,%?&= ]", "+").replaceAll("[+]+", "+");
                    sUrl2keyMap.put(url, cacheKey);
                }
                if (cacheKey.length() > DiskLruCache.MAX_DISKEY_SIZE) {
                    cacheKey = cacheKey.substring(0, DiskLruCache.MAX_DISKEY_SIZE);
                    sUrl2keyMap.put(url, cacheKey);
                }
            }
            return cacheKey;
        }
    }

}
