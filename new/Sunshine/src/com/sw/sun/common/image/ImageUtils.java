
package com.sw.sun.common.image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.view.Gravity;

import com.sw.sun.common.android.DisplayUtils;
import com.sw.sun.common.logger.MyLog;

public class ImageUtils {

    // 最小的浮点数之间的距离Epsilon
    private static final double EPS = 1e-7;

    private static Method sMetCreateImageThumbnail;

    static {
        // images
        try {
            Class<?> clsFileUtils = Class.forName("android.media.ThumbnailUtils");
            sMetCreateImageThumbnail = clsFileUtils.getMethod("createImageThumbnail", String.class,
                    int.class);
            sMetCreateImageThumbnail.setAccessible(true);
        } catch (Exception e) {
            MyLog.e(e);
        }
    }

    public static Bitmap createBitmap(byte[] jpeg, int orientation, boolean mirror, int inSampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        options.inPurgeable = true;
        ;
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);

        if (orientation != 0 || mirror) {
            Matrix m1 = new Matrix();
            Matrix m2 = new Matrix();
            if (orientation != 0) {
                m1.setRotate(orientation, bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f);
            }
            if (mirror) {
                m2.setScale(-1, 1, bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f);
                m1.postConcat(m2);
            }
            // We only rotate the thumbnail once even if we get OOM.
            try {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), m1, true);
                // If the rotated bitmap is the original bitmap, then it
                // should not be recycled.
                if (rotated != bitmap) {
                    bitmap.recycle();
                }
                return rotated;
            } catch (Throwable t) {
                MyLog.e("Failed to rotate thumbnail", t);
            }
        }
        return bitmap;
    }

    public static Bitmap createImageThumbnail(String filePath, int kind) {
        try {
            return (Bitmap) sMetCreateImageThumbnail.invoke(null, filePath, kind);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean floatEquals(float f1, float f2) {
        return Math.abs(f1 - f2) <= ((float) EPS);
    }

    public static boolean doubleEquals(double f1, double f2) {
        return Math.abs(f1 - f2) <= EPS;
    }

    public static int degreesToExifOrientation(float normalizedAngle) {
        if (floatEquals(normalizedAngle, 0.0f)) {
            return ExifInterface.ORIENTATION_NORMAL;
        } else if (floatEquals(normalizedAngle, 90.0f)) {
            return ExifInterface.ORIENTATION_ROTATE_90;
        } else if (floatEquals(normalizedAngle, 180.0f)) {
            return ExifInterface.ORIENTATION_ROTATE_180;
        } else if (floatEquals(normalizedAngle, 270.0f)) {
            return ExifInterface.ORIENTATION_ROTATE_270;
        }
        return ExifInterface.ORIENTATION_NORMAL;
    }

    public static int degreesToExifOrientation(int degree) {
        degree = (degree + 360) % 360;
        if (degree == 0) {
            return ExifInterface.ORIENTATION_NORMAL;
        } else if (degree == 90) {
            return ExifInterface.ORIENTATION_ROTATE_90;
        } else if (degree == 180) {
            return ExifInterface.ORIENTATION_ROTATE_180;
        } else if (degree == 270) {
            return ExifInterface.ORIENTATION_ROTATE_270;
        }
        return ExifInterface.ORIENTATION_NORMAL;
    }

    public static float exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    /**
     * 将subBmp图像合并到oriBmp中
     * 
     * @param oriBmp
     * @param subBmp
     * @param rc subFilePath的图像覆盖的区域
     * @param paint
     * @return
     */
    public static boolean mergeBitmap(final Bitmap oriBmp, final Bitmap subBmp, final Rect rc,
            final Paint paint) {
        try {
            if (subBmp == null) {
                return false;
            }
            final Canvas cvs = new Canvas(oriBmp);
            final Rect rcSub = new Rect(0, 0, subBmp.getWidth(), subBmp.getHeight());
            cvs.drawBitmap(subBmp, rcSub, rc, paint);
            return true;
        } catch (final OutOfMemoryError e) {
            MyLog.e(e);
        }
        return false;
    }

    /**
     * 将subBmp图像合并到oriBmp中
     * 
     * @param oriBmp
     * @param subBmp
     * @param rc subFilePath的图像覆盖的区域
     * @return
     */
    public static boolean mergeBitmap(final Bitmap oriBmp, final Bitmap subBmp, final Rect rc) {
        return mergeBitmap(oriBmp, subBmp, rc, new Paint());
    }

    public static boolean mergeBitmap(final Bitmap oriBmp, final Bitmap subBmp) {
        return mergeBitmap(oriBmp, subBmp, new Rect(0, 0, oriBmp.getWidth(), oriBmp.getHeight()));
    }

    // 获得圆角图片的方法
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {

        Bitmap output = Bitmap
                .createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * 获取圆形带白边的bitmap，采用默认的strokeWidth
     * 
     * @param bitmap
     * @return
     */
    public static Bitmap circleBitmap(Bitmap bitmap, final int defaultAvatrWidth) {
        return circleBitmap(bitmap, defaultAvatrWidth, DisplayUtils.dip2px(1.67f));
    }

    /**
     * 获取圆形带白边的bitmap，提供自定义strokeWidth
     * 
     * @param bitmap
     * @return
     */
    public static Bitmap circleBitmap(Bitmap bitmap, final int defaultAvatrWidth,
            final int strokeWidth) {
        if (null == bitmap) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        if (width < defaultAvatrWidth) {
            bitmap = scaleBitmapToDesire(bitmap, defaultAvatrWidth, defaultAvatrWidth, false);
            width = defaultAvatrWidth;
        }
        int padding = DisplayUtils.dip2px(1.67f);
        Bitmap output = Bitmap
                .createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint mPaint = new Paint();
        int STROKE_WIDTH = strokeWidth;
        final int color = 0xff424242;
        final Rect rect = new Rect(padding, padding, width - padding, width - padding);
        final RectF rectF = new RectF(rect);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        mPaint.setColor(color);
        int r = width / 2 - padding;
        canvas.drawRoundRect(rectF, r, r, mPaint);
        mPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, mPaint);// 将图片绘制成白色图片
        // 画白色圆圈
        mPaint.reset();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(STROKE_WIDTH);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(width / 2, width / 2, r, mPaint);

        return output;
    }

    public static Bitmap scaleBitmapToDesire(Bitmap srcBmp, int destWidth, int destHeight,
            boolean recycleSrcBmp) {
        return scaleBitmapToDesire(srcBmp, destWidth, destHeight, new CropOption(0, 0, 0, 0),
                recycleSrcBmp);
    }

    public static Bitmap scaleBitmapToDesire(Bitmap srcBmp, int destWidth, int destHeight,
            CropOption cropOption, boolean recycleSrcBmp) {
        Bitmap destBmp = null;
        try {
            int srcWidth = srcBmp.getWidth();
            int srcHeight = srcBmp.getHeight();

            if (srcWidth == destWidth && srcHeight == destHeight && cropOption.rx <= 0
                    && cropOption.ry <= 0 && cropOption.borderSize <= 0) {
                destBmp = srcBmp;
            } else {
                Config config = Config.ARGB_8888;
                if (srcBmp.getConfig() != null) {
                    config = srcBmp.getConfig();
                }
                destBmp = Bitmap.createBitmap(destWidth, destHeight, config);
                cropBitmapToAnother(srcBmp, destBmp, cropOption, recycleSrcBmp);
            }
        } catch (Exception e) {
        } catch (OutOfMemoryError e) {
        }

        return destBmp;
    }

    /**
     * 中心对齐，将源图片绘制到目的图片上，且可以根据cropOption的参数来增加圆角和描边
     */
    public static boolean cropBitmapToAnother(Bitmap srcBmp, Bitmap destBmp, boolean recycleSrcBmp) {
        return cropBitmapToAnother(srcBmp, destBmp, new CropOption(0, 0, 0, 0), recycleSrcBmp);
    }

    public static boolean cropBitmapToAnother(Bitmap srcBmp, Bitmap destBmp, CropOption cOpt,
            boolean recycleSrcBmp) {
        if (srcBmp != null && destBmp != null) {
            int srcWidth = srcBmp.getWidth();
            int srcHeight = srcBmp.getHeight();
            int destWidth = destBmp.getWidth();
            int destHeight = destBmp.getHeight();

            cOpt.borderSize = Math.max(cOpt.borderSize, 0);
            cOpt.borderSize = Math.min(cOpt.borderSize, Math.min(destWidth, destWidth) / 2);
            cOpt.rx = Math.max(cOpt.rx, 0);
            cOpt.rx = Math.min(cOpt.rx, destWidth / 2);
            cOpt.ry = Math.max(cOpt.ry, 0);
            cOpt.ry = Math.min(cOpt.ry, destHeight / 2);

            Canvas canvas = new Canvas(destBmp);
            canvas.drawARGB(0, 0, 0, 0);

            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);

            if (cOpt.rx - cOpt.borderSize > 0 && cOpt.ry - cOpt.borderSize > 0) {
                canvas.drawRoundRect(new RectF(cOpt.borderSize, cOpt.borderSize, destWidth
                        - cOpt.borderSize, destHeight - cOpt.borderSize),
                        cOpt.rx - cOpt.borderSize, cOpt.ry - cOpt.borderSize, paint);

                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            }

            int visibleWidth = destWidth - 2 * cOpt.borderSize;
            int visibleHeight = destHeight - 2 * cOpt.borderSize;
            float ratio = Math
                    .min(1.0f * srcWidth / visibleWidth, 1.0f * srcHeight / visibleHeight);
            int srcLeft = (int) ((srcWidth - visibleWidth * ratio) / 2);
            int srcTop = (int) ((srcHeight - visibleHeight * ratio) / 2);
            Rect src = new Rect(srcLeft, srcTop, srcWidth - srcLeft, srcHeight - srcTop);
            Rect dst = new Rect(cOpt.borderSize, cOpt.borderSize, destWidth - cOpt.borderSize,
                    destHeight - cOpt.borderSize);
            canvas.drawBitmap(srcBmp, src, dst, paint);

            if (cOpt.borderSize > 0 && cOpt.borderColor >>> 24 != 0) {
                paint.setColor(cOpt.borderColor);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
                canvas.drawRoundRect(new RectF(0, 0, destWidth, destHeight), cOpt.rx, cOpt.ry,
                        paint);
            }

            if (recycleSrcBmp) {
                srcBmp.recycle();
            }
            return true;
        }
        return false;
    }

    public static Drawable getLayerDrawable(Resources res, String filePath, int size) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        try {
            if (!new File(filePath).exists()) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(filePath, options);
            float oWidth = options.outWidth;
            float oHeight = options.outHeight;
            options.inSampleSize = ImageLoader.computeSampleSize(options, 1000 * 1500);

            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(new InputStreamLoader(
                    filePath).get(), false);
            // oWidth = (float) oWidth / (float) options.inSampleSize;
            // oHeight = (float) oHeight / (float) options.inSampleSize;
            int rowCount = (int) Math.ceil(oHeight / size);
            int colCount = (int) Math.ceil(oWidth / size);
            List<Drawable> dList = new ArrayList<Drawable>();
            float rowOffset = 1.0f / rowCount;
            float colOffset = 1.0f / colCount;
            float l = 0f;
            float t = 0f;
            float r = 0f;
            float b = 0f;
            int rC = 0;
            int cC = 0;
            List<Integer> leftList = new ArrayList<Integer>();
            List<Integer> topList = new ArrayList<Integer>();
            int cl = 0;
            int ct = 0;
            int w = 0;
            int h = 0;
            if (decoder != null) {

                while (t < 1.0f) {
                    b = t + rowOffset;
                    if (b > 1.0f) {
                        b = 1.0f;
                    }
                    l = 0f;
                    cC = 0;
                    cl = 0;
                    while (l < 1.0f) {
                        r = l + colOffset;
                        if (r > 1.0f) {
                            r = 1.0f;
                        }
                        Bitmap bmp = decoder.decodeRegion(new Rect((int) (l * oWidth),
                                (int) (t * oHeight), (int) (r * oWidth), (int) (b * oHeight)),
                                options);
                        BitmapDrawable bd = new BitmapDrawable(res, bmp);
                        w = bmp.getWidth();
                        h = bmp.getHeight();
                        bd.setGravity(Gravity.TOP | Gravity.LEFT);
                        dList.add(bd);
                        if (l < 1 && r > 1) {
                            r = 1.0f;
                        }
                        if (t < 1 && b > 1) {
                            b = 1.0f;
                        }

                        leftList.add(cl);
                        topList.add(ct);
                        cC++;
                        l += colOffset;
                        cl += w;
                    }
                    rC++;
                    t += rowOffset;
                    ct += h;
                }

            } else {
                return null;
            }

            Drawable[] drawables = new Drawable[dList.size()];
            dList.toArray(drawables);
            size = size / options.inSampleSize;
            LayerDrawable ld = new LayerDrawable(drawables);
            int k = 0;
            for (int i = 0; i < rC; i++) {
                for (int j = 0; j < cC; j++) {
                    ld.setLayerInset(i * colCount + j, leftList.get(k), topList.get(k), 0, 0);
                    k++;
                }
            }

            return ld;
        } catch (FileNotFoundException e) {
            MyLog.e(e);
        } catch (IOException e) {
            MyLog.e(e);
        } catch (OutOfMemoryError e) {
            MyLog.e(e);
        }

        return null;
    }

    public static Drawable getLayerDrawable(Resources res, Bitmap bitmap, int size) {
        if (bitmap == null) {
            return null;
        }

        int rowCount = (int) Math.ceil((float) bitmap.getHeight() / (float) size);
        int colCount = (int) Math.ceil((float) bitmap.getWidth() / (float) size);

        BitmapDrawable[] drawables = new BitmapDrawable[rowCount * colCount];
        try {

            for (int i = 0; i < rowCount; i++) {

                int top = size * i;
                int height = i == rowCount - 1 ? bitmap.getHeight() - top : size;

                for (int j = 0; j < colCount; j++) {

                    int left = size * j;
                    int width = j == colCount - 1 ? bitmap.getWidth() - left : size;

                    Bitmap b = Bitmap.createBitmap(bitmap, left, top, width, height);
                    BitmapDrawable bd = new BitmapDrawable(res, b);
                    bd.setGravity(Gravity.TOP | Gravity.LEFT);
                    drawables[i * colCount + j] = bd;
                }
            }
        } catch (OutOfMemoryError e) {
            MyLog.e(e);
            return null;
        }

        LayerDrawable ld = new LayerDrawable(drawables);
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                ld.setLayerInset(i * colCount + j, size * j, size * i, 0, 0);
            }
        }

        return ld;
    }

    public static class CropOption {
        public int rx;

        public int ry;

        public int borderSize;

        public int borderColor;

        public CropOption(int rx, int ry, int borderSize, int borderColor) {
            this.rx = rx;
            this.ry = ry;
            this.borderSize = borderSize;
            this.borderColor = borderColor;
        }
    }

}
