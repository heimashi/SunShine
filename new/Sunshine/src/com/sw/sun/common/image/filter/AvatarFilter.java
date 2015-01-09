
package com.sw.sun.common.image.filter;

import android.content.Context;
import android.graphics.Bitmap;
import com.sw.sun.common.image.ImageUtils;

import com.sw.sun.common.android.CommonUtils;
import com.sw.sun.common.android.DisplayUtils;

public class AvatarFilter implements BitmapFilter {
    //
    // private static Bitmap mMask = null;
    // private static Bitmap mBorder = null;
    //
    // private static Bitmap getMask(Context context) {
    // if (null == mMask) {
    // mMask = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_mask);
    // CommonUtils.DebugAssert(null != mMask && !mMask.isRecycled());
    // }
    // return mMask;
    // }

    // private static Bitmap getBorder(Context context) {
    // if (null == mBorder) {
    // mBorder = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_border);
    // CommonUtils.DebugAssert(null != mBorder && !mBorder.isRecycled());
    // }
    // return mBorder;
    // }
    static final float radius = DisplayUtils.dip2px(2.33f);

    @Override
    public Bitmap filter(Bitmap input, Context context) {
        CommonUtils.DebugAssert(null != input && !input.isRecycled());

        if(null == input){
            return null;
        }
        return ImageUtils.getRoundedCornerBitmap(input, input.getWidth()/2);
        //
        // final int targetWidth = input.getWidth();
        // final int targetHeight = input.getHeight();
        //
        // if (!input.isMutable()) {
        // Bitmap bm = Bitmap.createBitmap(targetWidth, targetHeight, Config.ARGB_8888);
        // CommonUtils.mergeBitmap(bm, input);
        // input = bm;
        // }
        //
        // input = ImageUtil.getRoundedCornerBitmap(input, radius);
        // return input;
        // if (input.isMutable()) {
        // doFilter(input, context);
        // return input;
        // } else {
        // Bitmap result = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Config.ARGB_8888);
        // CommonUtils.mergeBitmap(result, input);
        // doFilter(result, context);
        // return result;
        // }
    }

    // private void doFilter(Bitmap input, Context context) {
    // CommonUtils.DebugAssert(null != input && !input.isRecycled());
    // Bitmap mask = getMask(context);
    // Bitmap border = getBorder(context);
    // CommonUtils.mergeBitmap(input, border);
    // Paint paint = new Paint();
    // paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
    // CommonUtils.mergeBitmap(input, mask, paint);
    // }

    @Override
    public String getId() {
        return "RoundAvatarFilter";
    }
}
