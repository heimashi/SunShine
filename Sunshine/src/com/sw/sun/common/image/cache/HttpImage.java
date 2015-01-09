
package com.sw.sun.common.image.cache;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.widget.ImageView;

import com.sw.sun.common.android.CommonUtils;
import com.sw.sun.common.android.DisplayUtils;
import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.image.BaseMeta;
import com.sw.sun.common.image.ImageLoader;
import com.sw.sun.common.image.cache.DiskLruCache.Snapshot;
import com.sw.sun.common.image.filter.BitmapFilter;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.common.network.DownloadResponse;
import com.sw.sun.common.thread.ThreadPoolManager;

public class HttpImage extends BaseImage {

    static ConcurrentHashMap<String, Integer> sFailedHostMap = new ConcurrentHashMap<String, Integer>();

    static ConcurrentHashMap<String, Double> sHostSpeed = new ConcurrentHashMap<String, Double>();

    private static final LruCache<String, String> sUrl2keyMap = new LruCache<String, String>(8);

    protected boolean getRawBmp = false;

    protected String path = "";

    protected boolean isUseDDXC = false; // 是否使用断点续传

    public String url = "";

    public String fullSizeUrl = "";

    public BitmapFilter filter = null;

    public Bitmap loadingBitmap;

    public int width = DisplayUtils.dip2px(75);

    public int height = DisplayUtils.dip2px(75);

    public Config config;

    public boolean getFromHttp = false;

    public boolean isOutLink = false;// 是否是外链，如果是则不使用落地节点加速

    private DownloadCompletedListener listener;

    private OnDownloadDiskCacheProgress mDownloadProgress = null;

    private DISKEY_TYPE disKeyType = DISKEY_TYPE.TYPE_URL;

    public static enum DISKEY_TYPE {
        TYPE_URL, TYPE_PATH
    }

    public static Comparator<String> sUrlComparator = new Comparator<String>() {

        @Override
        public int compare(String lhs, String rhs) {
            int ltimes = 0;
            int rTimes = 0;
            if (sFailedHostMap.containsKey(lhs)) {
                ltimes = sFailedHostMap.get(lhs);
            }
            if (sFailedHostMap.containsKey(rhs)) {
                rTimes = sFailedHostMap.get(rhs);
            }
            if (ltimes < rTimes) {
                return -1;
            } else if (ltimes > rTimes) {
                return 1;
            } else {
                double lSpeed = 0f;
                if (sHostSpeed.contains(lhs)) {
                    lSpeed = sHostSpeed.get(lhs);
                }
                double rSpeed = 0f;
                if (sHostSpeed.contains(rhs)) {
                    rSpeed = sHostSpeed.get(rhs);
                }
                if ((lSpeed == 0 && rSpeed > 0) || (lSpeed > 0 && rSpeed > 0 && lSpeed > rSpeed)) {
                    return -1;
                } else if ((rSpeed == 0 && lSpeed > rSpeed)
                        || (lSpeed > 0 && rSpeed > 0 && lSpeed < rSpeed)) {
                    return 1;
                }
            }
            return 0;
        }
    };

    public HttpImage() {
    }

    public HttpImage(String url) {
        this(url, null);
    }

    public HttpImage(String url, String fullSizeUrl) {
        this(url, fullSizeUrl, null);
    }

    public HttpImage(String url, String fullSizeUrl, Config config) {
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

    String getPath() {
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

    Bitmap getBmpFromFullCacheFile(ImageCache imageCache) {
        String fullImgDiskCacheKey = getFullImgDiskCacheKey();
        if (!TextUtils.isEmpty(fullImgDiskCacheKey)) {
            return imageCache.getBitmapFromDiskCache(fullImgDiskCacheKey, width, height, config);
        }
        return null;
    }

    Bitmap getBmpFromDiskCache(ImageCache imageCache) {
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

        // If the image cache is available and this task has not been cancelled
        // by another
        // thread and the ImageView that was originally bound to this task is
        // still bound back
        // to this task and our "exit early" flag is not set then try and fetch
        // the bitmap from
        // the cache
        try {
            Bitmap result = getBmpFromDiskCache(imageCache);
            if (null == result && getFromHttp) {
                // MyLog.v(" getBitmapFromDiskCache - " + getDiskCacheKey());
                result = getHttpBitmap(imageCache);
            }
            // if (!TextUtils.isEmpty(fullSizeUrl)) {
            // result = imageCache.getBitmapFromDiskCache(diskKey(fullSizeUrl),
            // width, height, config);
            // }
            //
            // if (null == result) {
            // final DiskLruCache diskCache = imageCache.getDiskLruCache();
            // if (diskCache != null) {
            // // Download a bitmap, write it to a file
            // boolean success = downloadFile(diskCache);
            // if (success) {
            // result = imageCache.getBitmapFromDiskCache(getDiskCacheKey(),
            // width, height, config);
            // }
            // }
            // }
            // }

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

    public boolean downloadFile(final DiskLruCache diskLruCache) {
        // TODO
        // try {
        // Fallback fb = null;
        // ArrayList<String> urls = new ArrayList<String>();
        // if (!isOutLink) {
        // try {
        // fb = HostManager.getInstance().getFallbacksByURL(url);
        // if (fb != null) {
        // URL urlObj = new URL(url);
        // ArrayList<String> orderHosts = fb.getHosts();
        // Collections.sort(orderHosts, sUrlComparator);
        // for (String fallback : orderHosts) {
        // urls.add(new URL(urlObj.getProtocol(), fallback,
        // urlObj.getPort(), urlObj.getFile())
        // .toString());
        // }
        // // urls = fb.getUrls(url);
        // }
        // } catch (IllegalMonitorStateException e) {
        // MyLog.e(e);
        // } catch (IllegalArgumentException e) {
        // MyLog.e(e);
        // }
        // }
        //
        // // if (urls.isEmpty()) {
        // if (!urls.contains(url)) {
        // urls.add(urls.size(), url);
        // }
        // // }
        // for (String currentUrl : urls) {
        // long start = System.currentTimeMillis();
        // DownloadResponse result = downloadFile(diskLruCache, currentUrl);
        // URL urlObj = new URL(currentUrl);
        // String host = urlObj.getHost();
        // if (result.downloadState == DownloadResponse.DOWNLOAD_STATE_SUCCESS)
        // {
        // if (fb != null) {
        // long time = System.currentTimeMillis() - start;
        // long size = Utils.getHttpGetFileTraffic(
        // currentUrl.length(), result.downloadBytes);
        // fb.succeedUrl(currentUrl, time, size);
        // if (sFailedHostMap.containsKey(host)) {
        // sFailedHostMap.remove(host);
        // }
        // double speed = time * 1.0f / size;
        // sHostSpeed.put(host, speed);
        // }
        // return true;
        // } else {
        // if (fb != null) {
        // if (sFailedHostMap.containsKey(host)) {
        // int failedTime = sFailedHostMap.get(host);
        // if (failedTime < Integer.MAX_VALUE) {
        // sFailedHostMap.put(host, ++failedTime);
        // }
        // } else {
        // sFailedHostMap.put(host, 1);
        // }
        // if (sHostSpeed.containsKey(host)) {
        // sHostSpeed.remove(host);
        // }
        // MyLog.v("httpImage downloadFile host failed:" + host);
        // fb.failedUrl(currentUrl, System.currentTimeMillis()
        // - start, Utils.getHttpGetFileTraffic(
        // currentUrl.length(), result.downloadBytes),
        // result.e);
        // }
        // }
        // }
        // } catch (MalformedURLException e) {
        // MyLog.e(e);
        // }
        return false;
    }

    protected long getFinalSize() {
        return 0;
    }

    public DownloadResponse downloadFile(final DiskLruCache diskLruCache, final String fileURL) {
        // TODO
        // if (!isUseDDXC || getFinalSize() == 0) {
        // return Utils.diskCacheDownloadFile(fileURL, getDiskCacheKey(),
        // diskLruCache, mDownloadProgress, null);
        // }
        // MyLog.v("HttpImage downloadFile use DDXC fileURL=" + fileURL);
        // final String tmpDiskCacheKey = getTmpDiskCacheKey();
        // final String diskCacheKey = getDiskCacheKey();
        // File tmpFile;
        // tmpFile = new File(diskLruCache.getDirectory(), tmpDiskCacheKey);
        // DownloadResponse dr = Utils.downloadFile(GlobalData.app(),
        // diskCacheKey, fileURL, tmpFile, new OnDownloadProgress() {
        //
        // @Override
        // public void onFailed() {
        // if (null != mDownloadProgress) {
        // mDownloadProgress.onFailed();
        // }
        // }
        //
        // @Override
        // public void onDownloaded(long downloaded, long totalLength) {
        // if (null != mDownloadProgress) {
        // mDownloadProgress.onDownloaded(downloaded,
        // totalLength);
        // }
        // }
        //
        // @Override
        // public void onCompleted(String localPath) {
        // if (null != mDownloadProgress) {
        // try {
        // mDownloadProgress.onCompleted(diskLruCache
        // .get(diskCacheKey));
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }
        // }
        //
        // @Override
        // public void onCanceled() {
        // if (null != mDownloadProgress) {
        // mDownloadProgress.onFailed();
        // }
        // }
        // }, false, true);
        // if (dr.downloadState == DownloadResponse.DOWNLOAD_STATE_SUCCESS &&
        // tmpFile.length() > 0) {
        // Editor ed = null;
        // BufferedOutputStream out;
        // FileInputStream fis = null;
        // try {
        // ed = diskLruCache.edit(diskCacheKey);
        // if (null != ed) {
        // out = new BufferedOutputStream(ed.newOutputStream(0),
        // ImageCacheUtils.IO_BUFFER_SIZE);
        // fis = new FileInputStream(tmpFile);
        // final byte[] buffer = new byte[10 * 1024];
        // int count;
        // while ((count = fis.read(buffer)) != -1) {
        // out.write(buffer, 0, count);
        // }
        // out.close();
        // fis.close();
        // ed.commit();
        // tmpFile.delete();
        // return dr;
        // }
        // } catch (IOException e) {
        // MyLog.e(e);
        // } finally {
        // if (null != fis) {
        // try {
        // fis.close();
        // } catch (IOException e) {
        // }
        // }
        // if (null != ed) {
        // try {
        // ed.abort();
        // ed = null;
        // } catch (final IOException e) {
        // MyLog.e(" Error in downloadFile - " + url + " - ", e);
        // }
        // }
        // }
        // } else {
        // if (null != mDownloadProgress) {
        // mDownloadProgress.onFailed();
        // }
        // }
        //
        // return dr;
        return null;
    }

    @Override
    public Bitmap getLoadingBitmap() {
        return loadingBitmap;
    }

    @Override
    public void processImageView(ImageView imageView, Bitmap bm) {
        if (null != listener) {
            listener.onComplete(imageView, bm);
        }
    }

    public void setDownloadCompletedListener(DownloadCompletedListener l) {
        listener = l;
    }

    @Override
    public int getAsyncLoadLevel() {
        return getFromHttp ? ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_IMAGE
                : ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_LOCAL_IO;
    }

    public void setOnDownloadProgress(OnDownloadDiskCacheProgress progress) {
        mDownloadProgress = progress;
    }

    @Override
    public Bitmap getHttpBitmap(ImageCache imageCache) {
        if (!CommonUtils.isValidUrl(url)) {
            return null;
        }

        // If the image cache is available and this task has not been cancelled
        // by another
        // thread and the ImageView that was originally bound to this task is
        // still bound back
        // to this task and our "exit early" flag is not set then try and fetch
        // the bitmap from
        // the cache
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
                            if (!getRawBmp) {
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

    public interface OnDownloadDiskCacheProgress {
        public void onDownloaded(long downloaded, long totalLength);

        public void onCompleted(Snapshot snapShot);

        // 这里 canceled 的 语义为 用户手动点击的已知的 停止，暂停，取消
        // public void onCanceled();

        public void onFailed();

    }

    public interface DownloadCompletedListener {
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

    public static void clearFailedMap() {
        sFailedHostMap.clear();
        sHostSpeed.clear();
    }

}
