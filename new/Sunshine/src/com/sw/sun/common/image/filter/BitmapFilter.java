
package com.sw.sun.common.image.filter;

import android.content.Context;
import android.graphics.Bitmap;

public interface BitmapFilter {
    public Bitmap filter(Bitmap input, Context context);

    public String getId();
}
