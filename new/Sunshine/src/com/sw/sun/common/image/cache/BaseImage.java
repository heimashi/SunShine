
package com.sw.sun.common.image.cache;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.sw.sun.common.image.filter.BitmapFilter;

public abstract class BaseImage {

    public int tryTimes = 0;

    public BitmapFilter filter = null;

    public Bitmap loadingBitmap = null;

    public abstract String getMemCacheKey();

    public abstract String getDiskCacheKey();

    /**
     * 获取对应的bitmap对象。 从disk读取，或者从网络下载，然后放到diskCache中。
     * 
     * @param imageCache
     * @return
     */
    public abstract Bitmap getBitmap(ImageCache imageCache);

    public abstract Bitmap getHttpBitmap(ImageCache imageCache);

    /**
     * 是否支持loading的默认图。不支持的话，会使用ImageWorker中的默认图。
     * 
     * @return
     */
    public abstract Bitmap getLoadingBitmap();

    /**
     * 我们可能希望根据bitmap的情况调整imageview的layout参数。
     * 
     * @param imageView
     * @param bm
     */
    public abstract void processImageView(ImageView imageView, Bitmap bm);

    /**
     * 不同的bitmap，采用后台加载的分级不同，用以决定放入那个thread pool来执行
     * 
     * @return
     */
    public abstract int getAsyncLoadLevel();

    public boolean needGetFromHttp() {
        return false;
    }

    public boolean isGetLayerDrawable(ImageCache imageCache) {
        return false;
    }

    public interface LoadImageSuccessListener {
        public void successLoadImage(int width, int height);
    }

}
