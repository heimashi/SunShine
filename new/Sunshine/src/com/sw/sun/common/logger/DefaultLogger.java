
package com.sw.sun.common.logger;

import android.util.Log;

public class DefaultLogger implements LoggerInterface {

    private String mTag = "com.sw.sun";

    @Override
    public void setTag(String tag) {
        mTag = tag;
    }

    @Override
    public void log(String content) {
        Log.v(mTag, content);
    }

    @Override
    public void log(String content, Throwable t) {
        Log.v(mTag, content, t);
    }

}
