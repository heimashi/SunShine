
package com.sw.sun.common.image;

import java.io.IOException;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.text.TextUtils;

import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.file.FileUtils;
import com.sw.sun.common.logger.MyLog;

public class BaseMeta {
    public static final int PIXEL_SIZE_FULL = 854 * 480; // cost 1.63M

    public static final int PIXEL_SIZE_BIG = 200 * 200; // cost 0.16M

    public static final int PIXEL_SIZE_RAW = 6 * 1000 * 1000; // cost 24M

    public static final int PIXEL_SIZE_LARGE = GlobalData.screenWidth * GlobalData.screenHeight;

    public boolean mIsVideo;

    public boolean mIsImage;

    public boolean mIsJpeg;

    public boolean mIsGif;

    public boolean mIsSelected; // this is a tag to indicate whether this item

    // is selected in UI

    public String mTitle;

    public String mPath;

    public int mResId;

    public long mSize;

    // store lastModified time of the file. UTC time.
    // we should use this in BaseMetaComparator.getDiff(), and do NOT use
    // mDateTaken
    // because mDateModified is never changed after sorting is done, while
    // mDateTaken
    // can be changed (EXIF info is loaded in async task) after sorting is done.
    // Therefore, if use mDateTaken, the SortedList.indexOf() may return
    // not-found.
    public long mDateModified;

    private boolean mAllDataLoaded;

    // store EXIF DateTime value if available; otherwise same as mDateModified
    // always local time, which means UTC-to-local conversion when originating
    // from mDateModified.
    public long mDateTaken;

    public boolean mHasExifThumbnail;

    public int mOrientation = ExifInterface.ORIENTATION_NORMAL;

    public long mDuration;

    public double mLatitude;

    public double mLongitude;

    protected int mWidth;

    protected int mHeight;

    public BaseMeta() {
    };

    /**
     * 小心，这里会根据path中后缀类型来判断其mime type. 对于不同的mime type，处理流程会不一样。 如果已经知道mime
     * type，建议使用另外一个构造函数，直接传入mime type.
     * 
     * @param path
     */
    public BaseMeta(String path) {
        this(path, FileUtils.getMimeType(path));
    }

    public BaseMeta(String path, String mimeType) {
        mPath = path;
        setMimeType(mimeType);
        try {
            ExifInterface exif = new ExifInterface(mPath);
            mOrientation = (int) ImageUtils.exifOrientationToDegrees(exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
        } catch (IOException e) {
            MyLog.e(e);
        }
    }

    public BaseMeta(int resId, String mimeType) {
        mResId = resId;
        setMimeType(mimeType);
    }

    protected void setMimeType(String mimeType) {
        mIsVideo = mimeType.startsWith("video");
        mIsImage = mimeType.startsWith("image");
        mIsJpeg = mimeType.equals("image/jpeg");
        mIsGif = mimeType.equals("image/gif");
    }

    public void loadAllData() {
        if (!isModified()) {
            return;
        }

        loadAllDataInternal();
        clearModifiedFlags();
    }

    public boolean isModified() {
        return !mAllDataLoaded;
    }

    private void clearModifiedFlags() {
        mAllDataLoaded = true;
    }

    private void loadAllDataInternal() {
        if (mWidth <= 0) {
            if (mIsImage && !TextUtils.isEmpty(mPath)) {
                BitmapFactory.Options options = ImageLoader.getBitmapSize(new InputStreamLoader(
                        mPath));
                mWidth = Math.max(1, options.outWidth);
                mHeight = Math.max(1, options.outHeight);
            } else if (mIsImage && mResId > 0) {
                BitmapFactory.Options options = ImageLoader.getBitmapSize(new InputStreamLoader(
                        mResId));
                mWidth = Math.max(1, options.outWidth);
                mHeight = Math.max(1, options.outHeight);
            }
        }
    }

    public boolean isGif() {
        return mIsGif;
    }

    public boolean isVideo() {
        return mIsVideo;
    }

    public int getWidth() {
        loadAllData();
        return mWidth;
    }

    public int getHeight() {
        loadAllData();
        return mHeight;
    }

    public void delete() {
    };

    public Drawable getThumbnail(int pixelSize) {
        if (mIsGif && ((mWidth * mHeight) < pixelSize) && (pixelSize >= PIXEL_SIZE_FULL)) {
            MiuiGifAnimationDrawable drawable = new MiuiGifAnimationDrawable();
            drawable.setMaxDecodeSize(pixelSize * 4L);
            if (drawable.load(Resources.getSystem(), mResId)) {
                return drawable;
            }
            if (drawable.load(Resources.getSystem(), mPath)) {
                // for wrong GIF (e.g. renamed a .jpg as .gif), load() returns
                // false;
                // not return here. Try BitmapFactory, which can still correctly
                // decode it
                return drawable;
            }
        }

        Bitmap bitmap = ImageLoader.getBitmap(mPath, pixelSize);

        if (bitmap != null) {
            return new BitmapDrawable(Resources.getSystem(), bitmap);
        }
        return null;
    }

    public Drawable getRawImage() {
        return getThumbnail(PIXEL_SIZE_RAW);
    }

    public Drawable getMiddleImage() {
        return getThumbnail(PIXEL_SIZE_FULL);
    }

    public String getKey() {
        if (!TextUtils.isEmpty(mPath)) {
            return mPath;
        }
        return String.valueOf(mResId);
    }
}
