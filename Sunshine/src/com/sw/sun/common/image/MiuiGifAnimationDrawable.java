
package com.sw.sun.common.image;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.sw.sun.common.logger.MyLog;

/**
 * store decode result
 */
class GifDecodeResult {
    GifDecoder mGifDecoder;

    boolean mIsDecodeOk;
}

/**
 * decode some frames from GIF
 */
class DecodeGifFrames extends Handler {
    protected static final String TAG = "DecodeGifFrames";

    private static final int MESSAGE_WHAT_START = 1;

    private Handler mCallerHandler;

    private InputStreamLoader mGifSource;

    private long mMaxDecodeSize;

    /**
     * this data member is created and destroyed in caller thread Note: it is
     * difficult to return GifDecoder as result via sendMessage()
     */
    GifDecodeResult mDecodeResult = null;

    // create a thread for each GifAnimationDrawable
    // this is to enable multiple GifAnimationDrawables are displayed together
    HandlerThread mHandlerThread;

    // invoked in caller thread
    public static DecodeGifFrames createInstance(InputStreamLoader gifSource, long maxDecodeSize,
            Handler callerHandler) {
        HandlerThread thread = new HandlerThread("handler thread to decode GIF frames");
        thread.start();
        return new DecodeGifFrames(thread, gifSource, maxDecodeSize, callerHandler);
    }

    public DecodeGifFrames(HandlerThread handlerThread, InputStreamLoader gifSource,
            long maxDecodeSize, Handler callerHandler) {
        super(handlerThread.getLooper());

        mHandlerThread = handlerThread;

        mMaxDecodeSize = maxDecodeSize;
        mGifSource = gifSource;
        mCallerHandler = callerHandler;
    }

    @Override
    protected void finalize() throws Throwable {
        // quit the thread when destroy the object
        mHandlerThread.quit();

        super.finalize();
    }

    // caller thread: invoked to decode frame from startFrame
    public void decode(int startFrame) {
        if (mDecodeResult != null) {
            return;
        }

        mDecodeResult = new GifDecodeResult();
        // trigger handleMessage() in child thread
        Message msg = obtainMessage(MESSAGE_WHAT_START, startFrame, 0);
        this.sendMessage(msg);
    }

    // caller thread: get and clear decode result
    // so that decode() can be triggered again
    public GifDecodeResult getAndClearDecodeResult() {
        GifDecodeResult result = mDecodeResult;
        mDecodeResult = null;
        return result;
    }

    // invoked in the handler thread
    @Override
    public void handleMessage(Message msg) {
        if (msg.what == MESSAGE_WHAT_START) {
            int startFrame = msg.arg1;
            GifDecodeResult decodeResult = DecodeGifUtil.decode(mGifSource, mMaxDecodeSize,
                    startFrame);

            // Note: NOT do "mDecodeResult = decodeResult";
            // copy to data members to avoid thread access conflict
            mDecodeResult.mGifDecoder = decodeResult.mGifDecoder;
            mDecodeResult.mIsDecodeOk = decodeResult.mIsDecodeOk;

            // notify caller
            mCallerHandler.sendEmptyMessage(MiuiGifAnimationDrawable.MESSAGE_WHAT_DECODE_FRAMES);
        }
    }
}

/**
 * util to help decode GIF
 */
class DecodeGifUtil {
    // decode GIF from specified start frame.
    static public GifDecodeResult decode(InputStreamLoader gifSource, long maxDecodeSize,
            int startFrame) {
        GifDecodeResult decodeResult = new GifDecodeResult();
        decodeResult.mGifDecoder = null;
        decodeResult.mIsDecodeOk = false;

        InputStream inputStream = gifSource.get();
        if (inputStream != null) {
            decodeResult.mGifDecoder = new GifDecoder();
            GifDecoder gifDecoder = decodeResult.mGifDecoder;

            gifDecoder.setStartFrame(startFrame);
            gifDecoder.setMaxDecodeSize(maxDecodeSize);

            int status = gifDecoder.read(inputStream);

            decodeResult.mIsDecodeOk = (status == GifDecoder.STATUS_OK);
        }
        gifSource.close();

        return decodeResult;
    }
}

/**
 * for libra only
 */
public class MiuiGifAnimationDrawable extends AnimationDrawable {
    // Values to assign to the Message.what field for caller's handler
    public static final int MESSAGE_WHAT_DECODE_FRAMES = 1;

    private long mMaxDecodeSize = GifDecoder.MAX_DECODE_SIZE;

    // store one GIF frame
    private static class GifFrame {
        public Bitmap mImage;

        public int mDuration;

        public int mIndex;

        public GifFrame(Bitmap im, int duration, int index) {
            mImage = im;
            mDuration = duration;
            mIndex = index;
        }

        public void recycle() {
            if (mImage != null && !mImage.isRecycled()) {
                mImage.recycle();
            }
        }
    }

    // used for pull mode. store decoded GIF frames
    List<GifFrame> mFrames = new ArrayList<GifFrame>();

    // store max frames that can be stored in mFrames
    int mMaxFrames = 0;

    // this value is valid until gifDecoder.isDecodeToTheEnd() return true
    int mRealFrameCount = 0;

    // indicate whether all frames are decoded
    boolean mDecodedAllFrames = false;

    private Handler mDecodeFrameHandler;

    private InputStreamLoader mGifSource;

    private Resources mResources;

    private DecodeGifFrames mDecodeGifFrames;

    private final PrivateFields mPrivateFields = new PrivateFields();

    /**
     * This class store private fields of framework class
     */
    private class PrivateFields {
        // mDrawables of mDrawableContainerState
        public Drawable[] mDrawables;

        // mDurations of mAnimationState
        public int[] mDurations;

        private void init() {
            try {
                // get mDurations from mAnimationState
                Field[] fields = AnimationDrawable.class.getDeclaredFields();
                Object animationState;
                Field field = findFieldByName(fields, "mAnimationState");
                field.setAccessible(true);
                animationState = field.get(MiuiGifAnimationDrawable.this);

                fields = animationState.getClass().getDeclaredFields();
                field = findFieldByName(fields, "mDurations");
                field.setAccessible(true);
                mDurations = (int[]) field.get(animationState);

                // get mDrawables from mDrawableContainerState
                fields = DrawableContainer.class.getDeclaredFields();
                field = findFieldByName(fields, "mDrawableContainerState");
                field.setAccessible(true);
                Object drawableContainerState = field.get(MiuiGifAnimationDrawable.this);

                fields = DrawableContainerState.class.getDeclaredFields();
                field = findFieldByName(fields, "mDrawables");
                field.setAccessible(true);
                mDrawables = (Drawable[]) field.get(drawableContainerState);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        private Field findFieldByName(Field[] list, String name) {
            if (name == null) {
                return null;
            }

            for (Field field : list) {
                if (field.getName().equals(name)) {
                    return field;
                }
            }

            return null;
        }

        public void setFieldValue(int frame, Drawable drawable, int duration) {
            if (mDrawables == null || mDurations == null) {
                init();
            }
            if (mDrawables == null || mDurations == null) {
                MyLog.e("PrivateFields, Failed to init private fields");
                return;
            }
            mDrawables[frame] = drawable;
            mDurations[frame] = duration;
        }
    }

    private int getLastFrameIndex() {
        GifFrame lastFrame = mFrames.get(mFrames.size() - 1);
        return lastFrame.mIndex;
    }

    private int calcFrameIndex(int frameIndex) {
        if (mRealFrameCount == 0) {
            // real frame count is not reached
            return frameIndex;
        } else {
            // wrap the frame index
            return frameIndex % mRealFrameCount;
        }
    }

    public void decodeNextFrames() {
        boolean shouldDecode = false;
        int remainingFrames = mFrames.size();
        if (mMaxFrames <= 3) {
            // if the frame is big, do decode early
            shouldDecode = (remainingFrames <= 2);
        } else {
            // if the frame is small, do decode when 50% buffer is used
            shouldDecode = (remainingFrames <= mMaxFrames / 2);
        }

        if (shouldDecode) {
            int startFrame = calcFrameIndex(getLastFrameIndex() + 1);

            // TODO: reach the end in the pull mode.
            // if (startFrame <= getLastFrameIndex()) {
            // MyLog.v("MIUI GIF animation is reach the end.");
            // }
            if (null != mDecodeGifFrames) {
                mDecodeGifFrames.decode(startFrame);
            }
        }
    }

    @Override
    public boolean selectDrawable(int idx) {
        preSelectDrawable(idx);

        return super.selectDrawable(idx);
    }

    /**
     * invoked when AnimationDrawable tries to set a new display frame this is
     * the hack to implement pull mode for large or long GIF
     */
    public void preSelectDrawable(int frame) {
        if (mFrames.isEmpty()) {
            return;
        }

        GifFrame gifFame = mFrames.get(0);
        // if there is only 1 frame, keep it to display until more frames are
        // decoded;
        // otherwise pop and discard the first frame
        if (mFrames.size() > 1) {
            mFrames.remove(0);
        }

        // try to decode next frames
        decodeNextFrames();

        // set the frame to be displayed
        BitmapDrawable drawable = new BitmapDrawable(mResources, gifFame.mImage);

        mPrivateFields.setFieldValue(frame, drawable, gifFame.mDuration);
    }

    /**
     * pull mode: called each time after decode next frames store decode result
     * after decode next frames
     * 
     * @param decodeResult
     * @return true if successfully handle decode result
     */
    private boolean handleDecodeFramesResult(GifDecodeResult decodeResult) {
        if (!decodeResult.mIsDecodeOk || decodeResult.mGifDecoder == null) {
            return false;
        }

        GifDecoder gifDecoder = decodeResult.mGifDecoder;

        String text = String.format("Thread#%d: decoded %d frames [%s] [%d]", Thread
                .currentThread().getId(), decodeResult.mGifDecoder.getFrameCount(),
                decodeResult.mIsDecodeOk, mRealFrameCount);
        MyLog.v("dumpFrameIndex " + text);

        // store real frame count when decode to the end
        if (gifDecoder.isDecodeToTheEnd()) {
            mRealFrameCount = gifDecoder.getRealFrameCount();
        }

        // store decoded bitmap into this object
        int count = gifDecoder.getFrameCount();

        if (count > 0) {
            int currentLastFrameIndex = getLastFrameIndex();
            for (int i = 0; i < count; i++) {
                Bitmap bitmap = gifDecoder.getFrame(i);
                if (null != bitmap) {
                    if (i == 0) {
                        mWidth = bitmap.getWidth();
                        mHeight = bitmap.getHeight();
                    }
                    int duration = gifDecoder.getDelay(i);
                    int frameIndex = calcFrameIndex(currentLastFrameIndex + 1 + i);
                    mFrames.add(new GifFrame(bitmap, duration, frameIndex));
                }
            }
        }

        return true;
    }

    private int mWidth;

    private int mHeight;

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getTotalPlayTime() {
        return mTotalPlayTime;
    }

    private int mTotalPlayTime = 0;

    /**
     * This is called when decode the GIF for the first time
     */
    private boolean handleFirstDecodeResult(GifDecodeResult decodeResult) {
        if (decodeResult.mGifDecoder == null || !decodeResult.mIsDecodeOk) {
            return false;
        }

        GifDecoder gifDecoder = decodeResult.mGifDecoder;
        mDecodedAllFrames = gifDecoder.isDecodeToTheEnd();
        int count = gifDecoder.getFrameCount();
        if (count <= 0) {
            return false;
        }

        mTotalPlayTime = 0;

        for (int i = 0; i < count; i++) {
            if (mDecodedAllFrames) {
                // push mode: store all frames in AnimationDrawable
                addFrame(new BitmapDrawable(mResources, gifDecoder.getFrame(i)),
                        gifDecoder.getDelay(i));
                mTotalPlayTime += gifDecoder.getDelay(i);
            } else {
                // pull mode: store frames in this object
                Bitmap bitmap = gifDecoder.getFrame(i);
                int duration = gifDecoder.getDelay(i);
                mFrames.add(new GifFrame(bitmap, duration, i));
            }

            if (i == 0) {
                Bitmap bmp = gifDecoder.getFrame(i);
                if (null != bmp) {
                    mWidth = bmp.getWidth();
                    mHeight = bmp.getHeight();
                }
            }
        }

        if (!mDecodedAllFrames) {
            // pull mode: add 2 fake frames to start animation, see
            // selectDrawable()
            GifFrame gifFame = mFrames.get(0);
            BitmapDrawable drawable = new BitmapDrawable(mResources, gifFame.mImage);
            addFrame(drawable, gifFame.mDuration);
            addFrame(drawable, gifFame.mDuration);

            // prepare to decode next frames
            mDecodeFrameHandler = new DecodeFrameHandler(this, Looper.getMainLooper());
            mDecodeGifFrames = DecodeGifFrames.createInstance(mGifSource, mMaxDecodeSize,
                    mDecodeFrameHandler);

            mMaxFrames = mFrames.size();

            // decode next frames in background
            // in case that the GIF frame is big and mFrames can hold only 1
            // frame
            decodeNextFrames();
        }

        // loop play
        setOneShot(false);

        // call super version here because no need to do preSelectDrawable()
        super.selectDrawable(0);

        return true;
    }

    static class DecodeFrameHandler extends Handler {
        WeakReference<MiuiGifAnimationDrawable> mMiuiGifAnimationDrawable;

        DecodeFrameHandler(MiuiGifAnimationDrawable miuiGif, Looper looper) {
            super(looper);
            mMiuiGifAnimationDrawable = new WeakReference<MiuiGifAnimationDrawable>(miuiGif);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WHAT_DECODE_FRAMES:
                    // decode next frames only when successfully handle
                    // decode result
                    MiuiGifAnimationDrawable miuiGif = mMiuiGifAnimationDrawable.get();
                    if (null != miuiGif && null != miuiGif.mDecodeGifFrames) {
                        if (miuiGif.handleDecodeFramesResult(miuiGif.mDecodeGifFrames
                                .getAndClearDecodeResult())) {
                            // check whether to continue to decode next
                            // frames
                            miuiGif.decodeNextFrames();
                        }
                    }
                    break;
            }
        }
    }

    public void destroy() {
        if (null != mDecodeFrameHandler) {
            mDecodeFrameHandler.removeCallbacksAndMessages(null);
            mDecodeFrameHandler = null;
        }
        if (null != mDecodeGifFrames) {
            mDecodeGifFrames.removeCallbacksAndMessages(null);
            mDecodeGifFrames = null;
        }
    }

    private boolean internalLoad(Resources res, InputStreamLoader gifSource) {
        mResources = res;
        mGifSource = gifSource;

        GifDecodeResult decodeResult = DecodeGifUtil.decode(mGifSource, mMaxDecodeSize, 0);
        return handleFirstDecodeResult(decodeResult);
    }

    public boolean load(Resources res, Context context, Uri uri) {
        return internalLoad(res, new InputStreamLoader(context, uri));
    }

    public boolean load(Resources res, String gifPath) {
        return internalLoad(res, new InputStreamLoader(gifPath));
    }

    public boolean load(Resources res, int resId) {
        return internalLoad(res, new InputStreamLoader(resId));
    }

    public boolean load(Resources res, byte[] data) {
        return internalLoad(res, new InputStreamLoader(data));
    }

    public void setMaxDecodeSize(long maxDecodeSize) {
        mMaxDecodeSize = maxDecodeSize;
    }
}
