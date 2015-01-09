
package com.sw.sun.common.android;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Matrix;
import android.os.Handler;
import android.util.DisplayMetrics;

public abstract class GlobalData {

    private static int REQUEST_CODE_FIRST = 1000000;

    private static Application sAppContext;

    private static boolean sIsDebuggable;

    private static Object lock = new Object();

    public static float screenRate = 0;

    public static float screenDensity = 0;

    public static Matrix screenRateMatrix;

    public static int screenWidth = 0;

    public static int screenHeight = 0;

    public static DisplayMetrics screenMatrix;

    public static Handler globalHandler;

    public static int getRequestCode() {
        synchronized (lock) {
            return REQUEST_CODE_FIRST++;
        }
    }

    public static Application app() {
        return sAppContext;
    };

    public static boolean isDebuggable() {
        return sIsDebuggable;
    }

    public static void initialize(Context context) {
        sAppContext = (Application) context.getApplicationContext();
        sIsDebuggable = (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        if (globalHandler == null) {
            globalHandler = new Handler();
        }
        calculateScreenRate(context);
    }

    private static void calculateScreenRate(Context context) {
        screenMatrix = context.getResources().getDisplayMetrics();
        screenWidth = screenMatrix.widthPixels;
        screenHeight = screenMatrix.heightPixels;
        screenRate = (float) screenMatrix.densityDpi / (float) DisplayMetrics.DENSITY_HIGH;
        screenDensity = screenMatrix.density;
        screenRateMatrix = new Matrix();
        screenRateMatrix.setScale(screenRate, screenRate);

        if (screenWidth > screenHeight) {
            int temp = screenHeight;
            screenHeight = screenWidth;
            screenWidth = temp;
        }
    }
}
