
package com.sw.sun.common.image.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;

import com.sw.sun.common.image.cache.ImageCache.ImageCacheParams;

public class ImageCacheManager {

    /**
     * 头像在rom中的路径
     */
    @Deprecated
    public static final String AVATAR_CACHE = "avatar";

    /**
     * 表情在rom中缓存路径。
     */
    @Deprecated
    public static final String ANIMEMOJI_CACHE = "animemoji_cache";

    /**
     * 原先广播在rom中缓存路径。
     */
    @Deprecated
    public static final String WALL_IMAGE_CACHE = "simple_image_cache";

    /**
     * 最早的时候，广播在rom中缓存路径。
     */
    @Deprecated
    public static final String OLD_WALL_IMAGE_CACHE = "wall_image_cache";

    public static final String COMMON_IMAGE_CACHE = "common_image_cache_2";

    public static final int COMMON_TRIMED_IMAGE_CACHE = 2 * 1024 * 1024;

    protected static Map<String, ImageCache> sImageCacheMap = new ConcurrentHashMap<String, ImageCache>();

    public static ImageCache get(final Context context, final String uniqueName) {
        synchronized (sImageCacheMap) {
            ImageCache result = sImageCacheMap.get(uniqueName);
            if (null == result) {
                result = new ImageCache(context, uniqueName);//change
                sImageCacheMap.put(uniqueName, result);
            }
            return result;
        }
    }

    public static ImageCache get(final Context context, final ImageCacheParams params) {
        synchronized (sImageCacheMap) {
            ImageCache result = sImageCacheMap.get(params.uniqueName);
            if (null == result) {
                result = new ImageCache(context, params);//change
                sImageCacheMap.put(params.uniqueName, result);
            }
            return result;
        }
    }

    public static void clearMemoryCache() {
        synchronized (sImageCacheMap) {
            sImageCacheMap.values();
            for (ImageCache ic : sImageCacheMap.values()) {
                ic.clearMemCache();
            }
            sImageCacheMap.clear();
        }
    }

    public static void clearCaches() {
        synchronized (sImageCacheMap) {
            sImageCacheMap.values();
            for (ImageCache ic : sImageCacheMap.values()) {
                ic.clearCaches();
            }
            sImageCacheMap.clear();
        }
    }

    public static void clearMemoryCache(Context context, String uniqueName) {
        get(context, uniqueName).clearMemCache();
    }

    public static void trimMemoryCache(Context context, String uniqueName, int trimedSize) {
        get(context, uniqueName).trimMemCache(trimedSize);
    }
}
