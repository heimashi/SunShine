
package com.sw.sun.common.image.cache;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.widget.ImageView;

import com.sw.sun.common.android.AsyncTaskUtils;
import com.sw.sun.common.android.CommonUtils;
import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.image.ImageUtils;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.common.thread.ThreadPoolManager;

/**
 * This class wraps up completing some arbitrary long running work when loading
 * a bitmap to an ImageView. It handles things like using a memory and disk
 * cache, running the work in a background thread and setting a placeholder
 * image.
 */
public class ImageWorker {
    private static final String TAG = "ImageWorker";

    private static final int FADE_IN_TIME = 200;

    private static final int ACTIVE_TASK_CNT = 10;

    private static final int ACTIVE_HTTP_TASK_CNT = 10;

    private static final int STATE_RESUMED = 1;

    private static final int STATE_PAUSED = 2;

    private static final int STATE_STOPPED = 3;

    public static final int MAX_SIZE = 4096;

    public static final int MAX_SIZES = MAX_SIZE * MAX_SIZE;

    private static int sTaskInstanceCnt = 0;

    private static int sHttpTaskInstanceCnt = 0;

    private static LruCache<String, SoftReference<Bitmap>> sThumbBmpCache = new LruCache<String, SoftReference<Bitmap>>(
            50);

    private static ConcurrentHashMap<String, SoftReference<LayerDrawable>> sLayerDrawableCache = new ConcurrentHashMap<String, SoftReference<LayerDrawable>>(
            5);

    private static Set<String> sLoadingSet = Collections.synchronizedSet(new HashSet<String>());

    /**
     * 同一个key的，不要同步下载。处理时同步起来。
     */
    private static Map<String, String> sLocks = new HashMap<String, String>();

    @SuppressWarnings("rawtypes")
    private List<AsyncTask> mRunningTask = new ArrayList<AsyncTask>();

    private int mState = STATE_RESUMED;

    private ImageCache mImageCache = null;

    private Bitmap mLoadingBitmap = null;

    private boolean mFadeInBitmap = true;

    private boolean mLoadingBitmapCrossFade = false;

    private Context mContext = null;

    // 滑动时的请求被放到此map中
    private LruCache<ImageView, BaseImage> pendingMap = new LruCache<ImageView, BaseImage>(50);

    private LruCache<ImageView, BaseImage> pendingHttpMap = new LruCache<ImageView, BaseImage>(20);

    // private LruCache<ImageView, BaseImage> processingMap = new
    // LruCache<ImageView, BaseImage>(30);

    /**
     * 这个lock map的作用是将同一个memory cache key的请求同步在同一个key对象上。
     * 
     * @param url
     * @return
     */
    private static synchronized String getLock(String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        String lock = sLocks.get(key);
        if (lock != null) {
            return lock;
        }
        sLocks.put(key, key);
        return key;
    }

    public ImageWorker() {
        mContext = GlobalData.app();
    }

    /**
     * Load an image specified by the data parameter into an ImageView (override
     * {@link PadImageWorker#processBitmap(Object)} to define the processing
     * logic). A memory and disk cache will be used if an {@link PadImageCache}
     * has been set using {@link PadImageWorker#setImageCache(PadImageCache)}.
     * If the image is found in the memory cache, it is set immediately,
     * otherwise an {@link AsyncTask} will be created to asynchronously load the
     * bitmap. 强烈建议客户端在使用时对ImageView进行重用，而不是每次新inflate一个。
     * 
     * @param data The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    @SuppressLint("NewApi")
    public void loadImage(final BaseImage img, final ImageView imageView) {

        if (mState == STATE_STOPPED) {
            MyLog.warn("the worker is stopped");
            return;
        }
        Bitmap bitmap = null;

        if (mImageCache != null) {
            bitmap = mImageCache.getBitmapFromMemCache(img.getMemCacheKey());
        }

        if (bitmap != null && !bitmap.isRecycled()) {
            // Bitmap found in memory cache
            img.processImageView(imageView, bitmap);
            setImage(imageView, bitmap, img);
        } else {
            if (sLayerDrawableCache.containsKey(img.getMemCacheKey())) {
                SoftReference<LayerDrawable> sld = sLayerDrawableCache.get(img.getMemCacheKey());
                if (sld != null && sld.get() != null) {
                    imageView.setImageDrawable(sld.get());
                    MyLog.v("ImageWorker hit layerDrawable memCacheKey=" + img.getMemCacheKey());
                    return;
                }
            }
            if (cancelPotentialWork(img.getMemCacheKey(), imageView)) {
                if ((img instanceof HttpImage) && (sHttpTaskInstanceCnt < ACTIVE_HTTP_TASK_CNT)) {
                    HttpImage hi = (HttpImage) img;
                    hi.getFromHttp = true;
                }
                cancel(imageView);
                boolean isNetWork = img.getAsyncLoadLevel() == ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_IMAGE;
                if (mState == STATE_PAUSED) {
                    if (img.getLoadingBitmap() != null) {
                        imageView.setImageBitmap(img.getLoadingBitmap());
                    } else if (mLoadingBitmap != null) {
                        imageView.setImageBitmap(mLoadingBitmap);
                    }
                    if (isNetWork) {
                        pendingHttpMap.put(imageView, img);
                    } else {
                        put(imageView, img);
                    }
                } else {
                    AsyncDrawable asyncDrawable = null;

                    Bitmap loadingBitmap = null;
                    if (img.getLoadingBitmap() != null) {
                        loadingBitmap = img.getLoadingBitmap();
                    } else if (mLoadingBitmap != null) {
                        loadingBitmap = mLoadingBitmap;
                    }

                    // 这里取以前默认的drawable可能有些问题。如果用户没有设置默认图，重用时会将以前的图取
                    // 出来当做默认图。效果上就是图片错位。这里应该有个默认的空白图。
                    if (null == loadingBitmap) {
                        Drawable oldDrawable = imageView.getDrawable();
                        if ((null != oldDrawable) && (oldDrawable instanceof BitmapDrawable)) {
                            loadingBitmap = ((BitmapDrawable) oldDrawable).getBitmap();
                        }
                    }

                    boolean needPutPending = (isNetWork && sHttpTaskInstanceCnt >= ACTIVE_HTTP_TASK_CNT)
                            || (!isNetWork && sTaskInstanceCnt >= ACTIVE_TASK_CNT);
                    if (!needPutPending) {
                        String memCacheKey = img.getMemCacheKey();
                        if (!TextUtils.isEmpty(memCacheKey)) {
                            if (sLoadingSet.contains(memCacheKey)) {
                                if (null != loadingBitmap) {
                                    imageView.setImageBitmap(loadingBitmap);
                                }
                                put(imageView, img);
                                return;
                            } else {
                                sLoadingSet.add(memCacheKey);
                            }
                        }
                        if (img.isGetLayerDrawable(mImageCache)) {
                            CreateDrawableTask task = new CreateDrawableTask(imageView, img);
                            CreateDrawable drawable = null;
                            if (img != null && img.getLoadingBitmap() != null
                                    && (imageView != null)) {
                                drawable = new CreateDrawable(mContext.getResources(),
                                        img.getLoadingBitmap(), task);
                            } else if (mLoadingBitmap != null && (imageView != null)) {
                                drawable = new CreateDrawable(mContext.getResources(),
                                        mLoadingBitmap, task);
                            } else {
                                drawable = new CreateDrawable(mContext.getResources(), bitmap, task);
                            }

                            imageView.setImageDrawable(drawable);
                            mRunningTask.add(task);
                            AsyncTaskUtils.exe(ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_LOCAL_IO,
                                    task);
                        } else {
                            final BitmapWorkerTask task = new BitmapWorkerTask(imageView, img);

                            asyncDrawable = new AsyncDrawable(mContext.getResources(),
                                    loadingBitmap, task);
                            imageView.setImageDrawable(asyncDrawable);
                            mRunningTask.add(task);
                            AsyncTaskUtils.exe(img.getAsyncLoadLevel(), task);
                        }
                    } else {
                        imageView.setImageBitmap(loadingBitmap);
                        if (isNetWork) {
                            pendingHttpMap.put(imageView, img);
                        } else {
                            put(imageView, img);
                            MyLog.e("image work runs too much tasks.");
                        }
                    }
                }
            }
        }
    }

    /**
     * 如果同一个列表中有的image没有使用imagework，那么在自行设置其bitmap后需要调用这个方法。
     * 
     * @param imageView
     */
    public void cancel(ImageView imageView) {
        pendingMap.remove(imageView);
        pendingHttpMap.remove(imageView);
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is
     * running.
     * 
     * @param bitmap
     */
    public void setLoadingImage(final Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is
     * running.
     * 
     * @param resId
     */
    public void setLoadingImage(final int resId) {
        try {
            mLoadingBitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
        } catch (OutOfMemoryError e) {
            MyLog.e(e);
            System.gc();
        }
    }

    /**
     * Set the {@link PadImageCache} object to use with this ImageWorker.
     * 
     * @param cacheCallback
     */
    public void setImageCache(final ImageCache cacheCallback) {
        mImageCache = cacheCallback;
    }

    public ImageCache getImageCache() {
        return mImageCache;
    }

    /**
     * If set to true, the image will fade-in once it has been loaded by the
     * background thread.
     * 
     * @param fadeIn
     */
    public void setImageFadeIn(final boolean fadeIn) {
        mFadeInBitmap = fadeIn;
    }

    /**
     * 如果设为true，loadingbitmap 会在图片加载成功后隐去
     * 
     * @param crossFade
     */
    public void setLoadingBitmapCrossFade(boolean crossFade) {
        mLoadingBitmapCrossFade = crossFade;
    }

    /**
     * Subclasses should override this to define any processing or work that
     * must happen to produce the final bitmap. This will be executed in a
     * background thread and be long running. For example, you could resize a
     * large bitmap here, or pull down an image from the network.
     * 
     * @param data The data to identify which image to process, as provided by
     *            {@link PadImageWorker#loadImage(Object, ImageView)}
     * @return The processed bitmap
     */
    protected Bitmap processBitmap(BaseImage image) {
        try {
            return image.getBitmap(mImageCache);
        } catch (OutOfMemoryError e) {
            MyLog.e(e);
            mImageCache.clearMemCache();
            return null;
        }
    }

    protected Bitmap processHttpBitmap(BaseImage image) {
        try {
            return image.getHttpBitmap(mImageCache);
        } catch (OutOfMemoryError e) {
            MyLog.e(e);
            mImageCache.clearMemCache();
            return null;
        }
    }

    public static void cancelWork(final ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            final BaseImage image = bitmapWorkerTask.img;
            MyLog.v(TAG + " cancelWork - cancelled work for "
                    + (image == null ? null : image.getMemCacheKey()));
        }
    }

    /**
     * Returns true if the current work has been canceled or if there was no
     * work in progress on this image view. Returns false if the work in
     * progress deals with the same data. The work is not stopped in that case.
     */
    public boolean cancelPotentialWork(final String memCacheKey, final ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if ((bitmapWorkerTask != null) && (bitmapWorkerTask.img != null)) {
            final String _memCacheKey = bitmapWorkerTask.img.getMemCacheKey();
            if ((_memCacheKey == null) || !_memCacheKey.equals(memCacheKey)) {
                bitmapWorkerTask.cancel(true);
                // processingMap.remove(imageView);
                // MyLog.v(TAG + " cancelWork - cancelled work for " +
                // memCacheKey);
            } else {
                // The same work is already in progress.
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active work task (if any) associated with
     *         this imageView. null if there is no such task.
     */
    private static BitmapWorkerTask getBitmapWorkerTask(final ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * The actual AsyncTask that will asynchronously process the image.
     */
    private class BitmapWorkerTask extends AsyncTask<BaseImage, Void, Bitmap> {
        private BaseImage img;

        private final WeakReference<ImageView> imageViewReference;

        public boolean isGetFromHttp = false;

        public BitmapWorkerTask(final ImageView imageView, final BaseImage img) {
            imageViewReference = new WeakReference<ImageView>(imageView);
            this.img = img;
        }

        @Override
        protected void onPreExecute() {
            // ImageView iv = imageViewReference.get();
            // if (null != iv) {
            // addToProcessngMap(iv, img);
            // }
            super.onPreExecute();
            if (isGetFromHttp) {
                sHttpTaskInstanceCnt++;
            } else {
                sTaskInstanceCnt++;
            }
        }

        @Override
        protected void onCancelled(Bitmap result) {
            if (isGetFromHttp) {
                decreaseHttpTaskCnt();
            } else {
                decreaseTaskCnt();
            }
        }

        /**
         * Background processing.
         */
        @Override
        protected Bitmap doInBackground(final BaseImage... params) {
            Bitmap bitmap = null;

            String lock = getLock(img.getMemCacheKey());

            if (!TextUtils.isEmpty(lock)) {
                synchronized (lock) {
                    if (mImageCache == null) {
                        return null;
                    }
                    bitmap = mImageCache.getBitmapFromMemCache(img.getMemCacheKey());

                    // If the bitmap was not found in the cache and this task
                    // has not been cancelled by
                    // another thread and the ImageView that was originally
                    // bound to this task is still
                    // bound back to this task and our "exit early" flag is not
                    // set, then call the main
                    // process method (as implemented by a subclass)
                    if ((bitmap == null) && !isCancelled() && (getAttachedImageView() != null)
                            && mState != STATE_STOPPED) {
                        if (!isGetFromHttp) {
                            bitmap = processBitmap(img);
                        } else {
                            bitmap = processHttpBitmap(img);
                            if (bitmap != null) {
                                img.tryTimes++;
                            }
                        }
                    }

                    // If the bitmap was processed and the image cache is
                    // available, then add the processed
                    // bitmap to the cache for future use. Note we don't check
                    // if the task was cancelled
                    // here, if it was, and the thread is still running, we may
                    // as well add the processed
                    // bitmap to our cache as it might be used again in the
                    // future
                    if (bitmap != null) {
                        mImageCache.addBitmapToMemCache(img.getMemCacheKey(), bitmap);
                        try {
                            if (img instanceof HttpImage) {
                                String diskKey = img.getDiskCacheKey();
                                if (!TextUtils.isEmpty(diskKey)) {
                                    sThumbBmpCache.put(diskKey, new SoftReference<Bitmap>(bitmap));
                                }
                            }
                        } catch (Exception e) {
                            MyLog.e(e);
                        }
                    }
                }
            }
            return bitmap;
        }

        /**
         * Once the image is processed, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {

            // if cancel was called on this task or the "exit early" flag is set
            // then we're done
            final ImageView imageView = getAttachedImageView();
            if (isCancelled() || mState == STATE_STOPPED) {
                bitmap = null;
            } else {
                if ((bitmap != null) && (imageView != null)) {
                    setImageBitmap(imageView, bitmap, img);
                } else if (imageView != null && !isGetFromHttp && img.needGetFromHttp()) {
                    Bitmap loadingBitmap = img.getLoadingBitmap();
                    if (sHttpTaskInstanceCnt < ACTIVE_HTTP_TASK_CNT && img.tryTimes < 1) {
                        loadHttpImg(imageView, img);
                    } else {
                        imageView.setImageBitmap(loadingBitmap);
                        pendingHttpMap.put(imageView, img);
                        MyLog.e("image work runs too much http tasks.");
                    }
                }
            }
            if (isGetFromHttp) {
                decreaseHttpTaskCnt();
            } else {
                decreaseTaskCnt();
            }
            removeFromLoadingSet(img);
            mRunningTask.remove(this);

        }

        /**
         * Returns the ImageView associated with this task as long as the
         * ImageView's task still points to this task as well. Returns null
         * otherwise.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }
    }

    public Bitmap getThumbnailBmp(String diskKey) {
        if (!TextUtils.isEmpty(diskKey)) {
            SoftReference<Bitmap> srf = sThumbBmpCache.get(diskKey);
            if (null != srf) {
                return srf.get();
            }
        }
        return null;
    }

    /**
     * A custom Drawable that will be attached to the imageView while the work
     * is in progress. Contains a reference to the actual worker task, so that
     * it can be stopped if a new binding is required, and makes sure that only
     * the last started worker process can bind its result, independently of the
     * finish order.
     */
    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(final Resources res, final Bitmap bitmap,
                final BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);

            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    /**
     * Called when the processing is complete and the final bitmap should be set
     * on the ImageView.
     * 
     * @param imageView
     * @param bitmap
     */
    private void setImageBitmap(final ImageView imageView, final Bitmap bitmap, BaseImage img) {
        if (mFadeInBitmap) {
            if (img != null) {
                img.processImageView(imageView, bitmap);
            }
            // Transition drawable with a transparent drwabale and the final
            // bitmap
            // Drawable fromDrawable = null;
            // if (null != mLoadingBitmap) {
            // // Set background to loading bitmap
            // fromDrawable = new BitmapDrawable(mContext.getResources(),
            // mLoadingBitmap);
            // } else {
            // Drawable oldDrawable = imageView.getDrawable();
            // if ((null != oldDrawable) && (oldDrawable instanceof
            // BitmapDrawable)) {
            // Bitmap oldBitmap = ((BitmapDrawable) oldDrawable).getBitmap();
            // // Set background to old bitmap
            // fromDrawable = new BitmapDrawable(mContext.getResources(),
            // oldBitmap);
            // }
            // }
            // if (fromDrawable == null) {
            // fromDrawable = new ColorDrawable(android.R.color.transparent);
            // }
            // final TransitionDrawable td =
            // new TransitionDrawable(new Drawable[] {
            // fromDrawable,
            // new BitmapDrawable(mContext.getResources(), bitmap)
            // });
            final TransitionDrawable td = new TransitionDrawable(new Drawable[] {
                    new ColorDrawable(android.R.color.transparent),
                    new BitmapDrawable(mContext.getResources(), bitmap)
            });

            td.setCrossFadeEnabled(mLoadingBitmapCrossFade);

            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            setImage(imageView, bitmap, img);
        }

    }

    /**
     * 暂停后台load。后续请求会pending起来。通常情况下，在列表FLING时调用次方法。
     */
    public void pause() {
        mState = STATE_PAUSED;
    }

    /**
     * resume请求。在pause过程中pending的请求此时会一次bind。通常在activity的onResume时调用。
     */
    public void resume() {
        mState = STATE_RESUMED;
        bind();
    }

    /**
     * 将worker置于pause状态，并且清空pending的请求。通常在activity的onDestroy时调用。
     */
    public void stop() {
        mState = STATE_STOPPED;
        mLoadingBitmap = null;
        clear();
        for (AsyncTask task : mRunningTask) {
            if (task.getStatus() != AsyncTask.Status.FINISHED) {
                task.cancel(true);
            }
        }
        mRunningTask.clear();
    }

    /**
     * 因为LayerDrawable可能会leak，所以需要手动回收
     */
    public void recycleLayerDrawable() {
        for (SoftReference<LayerDrawable> sld : sLayerDrawableCache.values()) {
            LayerDrawable lDrawable = sld.get();
            if (null != lDrawable) {
                int num = lDrawable.getNumberOfLayers();
                for (int i = 0; i < num; i++) {
                    Bitmap bmp = ((BitmapDrawable) lDrawable.getDrawable(i)).getBitmap();
                    if (bmp != null && !bmp.isRecycled()) {
                        bmp.recycle();
                        MyLog.v("ImageWorker stop LayerDrawable bmp.recycle");
                    }
                }
                lDrawable.setCallback(null);
            }
        }
        sLayerDrawableCache.clear();
    }

    private void put(ImageView view, BaseImage image) {
        CommonUtils.debugAssert((null != view) && (null != image));
        pendingMap.put(view, image);
    }

    private void clear() {
        pendingMap.evictAll();
        pendingHttpMap.evictAll();
        sLoadingSet.clear();
    }

    private void bind() {
        if (sTaskInstanceCnt < ACTIVE_TASK_CNT && pendingMap.size() > 0) {
            Map<ImageView, BaseImage> binders = pendingMap.snapshot();
            for (ConcurrentHashMap.Entry<ImageView, BaseImage> entry : binders.entrySet()) {
                BaseImage value = entry.getValue();
                ImageView key = entry.getKey();
                if (sTaskInstanceCnt < ACTIVE_TASK_CNT) {
                    if (!isUsed(key, value)) {

                        pendingMap.remove(key);
                        loadImage(value, key);
                    }
                } else {
                    break;
                }
            }
        }
        if (sHttpTaskInstanceCnt < ACTIVE_HTTP_TASK_CNT && pendingHttpMap.size() > 0) {
            Map<ImageView, BaseImage> binders = pendingHttpMap.snapshot();
            for (ConcurrentHashMap.Entry<ImageView, BaseImage> entry : binders.entrySet()) {
                BaseImage value = entry.getValue();
                ImageView key = entry.getKey();
                if (sTaskInstanceCnt < ACTIVE_HTTP_TASK_CNT) {
                    if (!isUsed(key, value) && value.tryTimes < 1) {
                        pendingHttpMap.remove(key);
                        loadHttpImg(key, value);
                    }
                } else {
                    break;
                }
            }
        }

    }

    private void loadHttpImg(ImageView imageView, BaseImage img) {
        pendingHttpMap.remove(imageView);
        final BitmapWorkerTask task = new BitmapWorkerTask(imageView, img);
        AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(),
                img.getLoadingBitmap(), task);
        imageView.setImageDrawable(asyncDrawable);
        task.isGetFromHttp = true;
        AsyncTaskUtils.exe(ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_IMAGE, task);
    }

    private void decreaseTaskCnt() {
        sTaskInstanceCnt--;
        if (sTaskInstanceCnt < ACTIVE_TASK_CNT && (mState != STATE_STOPPED)) {
            bind();
        }
    }

    private void decreaseHttpTaskCnt() {
        sHttpTaskInstanceCnt--;
        if (sHttpTaskInstanceCnt < ACTIVE_HTTP_TASK_CNT && (mState != STATE_STOPPED)) {
            bind();
        }
    }

    private boolean isUsed(ImageView key, BaseImage bi) {
        Drawable d = key.getDrawable();
        if (d instanceof BitmapDrawable) {
            Bitmap bm = ((BitmapDrawable) d).getBitmap();
            if (bm != null && bm != mLoadingBitmap && bm != bi.getLoadingBitmap()) {
                return true;
            }
        }
        return false;
    }

    private void setImage(ImageView imageView, Bitmap bitmap, BaseImage img) {
        if (bitmap.getWidth() < MAX_SIZE && bitmap.getHeight() < MAX_SIZE) {
            if (img != null) {
                img.processImageView(imageView, bitmap);
            }
            imageView.setImageBitmap(bitmap);
        } else {
            CreateDrawableTask task = new CreateDrawableTask(imageView, bitmap, img);
            CreateDrawable drawable = null;
            if (img != null && img.getLoadingBitmap() != null && (imageView != null)) {
                drawable = new CreateDrawable(mContext.getResources(), img.getLoadingBitmap(), task);
            } else if (mLoadingBitmap != null && (imageView != null)) {
                drawable = new CreateDrawable(mContext.getResources(), mLoadingBitmap, task);
            } else {
                drawable = new CreateDrawable(mContext.getResources(), bitmap, task);
            }

            imageView.setImageDrawable(drawable);
            AsyncTaskUtils.exe(ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_LOCAL_IO, task);
        }
    }

    /**
     * A custom Drawable that will be attached to the imageView while the work
     * is in progress. Contains a reference to the actual worker task, so that
     * it can be stopped if a new binding is required, and makes sure that only
     * the last started worker process can bind its result, independently of the
     * finish order.
     */
    private static class CreateDrawable extends BitmapDrawable {
        private final WeakReference<CreateDrawableTask> createDrawableTaskReference;

        public CreateDrawable(final Resources res, final Bitmap bitmap,
                final CreateDrawableTask createDrawableTask) {
            super(res, bitmap);

            createDrawableTaskReference = new WeakReference<CreateDrawableTask>(createDrawableTask);
        }

        public CreateDrawableTask getCreateDrawableTask() {
            return createDrawableTaskReference.get();
        }
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active work task (if any) associated with
     *         this imageView. null if there is no such task.
     */
    private static CreateDrawableTask getCreateDrawableTask(final ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof CreateDrawable) {
                final CreateDrawable asyncDrawable = (CreateDrawable) drawable;
                return asyncDrawable.getCreateDrawableTask();
            }
        }
        return null;
    }

    private void removeFromLoadingSet(BaseImage img) {
        if (null != img) {
            String memCacheKey = img.getMemCacheKey();
            if (!TextUtils.isEmpty(memCacheKey) && sLoadingSet.contains(memCacheKey)) {
                sLoadingSet.remove(memCacheKey);
            }
        }
    }

    private class CreateDrawableTask extends AsyncTask<Void, Void, Drawable> {
        private Bitmap bitmap;

        private BaseImage img;

        private final WeakReference<ImageView> imageViewReference;

        private String filePath;

        public CreateDrawableTask(ImageView imageView, Bitmap bitmap, BaseImage image) {
            this.bitmap = bitmap;
            img = image;
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        public CreateDrawableTask(ImageView imageView, BaseImage image) {
            img = image;
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            sTaskInstanceCnt++;
        }

        @Override
        protected void onPostExecute(Drawable result) {
            super.onPostExecute(result);
            final ImageView imageView = getAttachedImageView();
            if (img != null && null != imageView) {
                img.processImageView(imageView, bitmap);
                removeFromLoadingSet(img);
            }
            if (imageView != null && result != null) {
                imageView.setImageDrawable(result);
            }
            decreaseTaskCnt();
            mRunningTask.remove(this);
        }

        @Override
        protected Drawable doInBackground(Void... params) {
            Drawable ld = null;
            if (null != this.img) {
                String lock = getLock(img.getMemCacheKey());
                if (!TextUtils.isEmpty(lock)) {
                    synchronized (lock) {
                        String memCacheKey = this.img.getMemCacheKey();
                        if (sLayerDrawableCache.containsKey(memCacheKey)) {
                            SoftReference<LayerDrawable> sld = sLayerDrawableCache.get(memCacheKey);
                            if (null != sld && null != sld.get()) {
                                MyLog.v("ImageWorker hit layerDrawable memCacheKey=" + memCacheKey);
                                return sld.get();
                            }
                        }

                        if (this.img instanceof HttpImage) {
                            HttpImage hi = (HttpImage) this.img;
                            this.filePath = hi.getLocalFilePath(mImageCache);
                            if (!TextUtils.isEmpty(filePath)) {
                                ld = ImageUtils.getLayerDrawable(mContext.getResources(),
                                        this.filePath, MAX_SIZE);
                            }
                        } else {
                            ld = ImageUtils.getLayerDrawable(mContext.getResources(), bitmap,
                                    MAX_SIZE);
                        }
                        if (ld instanceof LayerDrawable) {
                            sLayerDrawableCache.put(this.img.getMemCacheKey(),
                                    new SoftReference<LayerDrawable>((LayerDrawable) ld));
                        }
                    }
                }
            }
            return ld;
        }

        private ImageView getAttachedImageView() {
            final ImageView imageView = imageViewReference.get();
            final CreateDrawableTask createDrawableTask = getCreateDrawableTask(imageView);

            if (this == createDrawableTask) {
                return imageView;
            }

            return null;
        }

    }
}
