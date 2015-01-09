
package com.sw.sun.common.android;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import junit.framework.Assert;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.sw.sun.common.logger.MyLog;

public abstract class CommonUtils {

    public static boolean isUriValid(Uri uri, ContentResolver resolver) {
        if (uri == null)
            return false;

        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                MyLog.e("CommonUtils isUriValid() Fail to open URI. URI=" + uri);
                return false;
            }
            pfd.close();
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    public static void closeSilently(Closeable c) {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

    public static String ensureNotNull(String value) {
        return value == null ? "" : value;
    }

    // Throws NullPointerException if the input is null.
    public static <T> T checkNotNull(T object) {
        if (object == null)
            throw new NullPointerException();
        return object;
    }

    // Throws AssertionError if the input is false.
    public static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static void debugAssert(final boolean assertStatement) {
        if (GlobalData.isDebuggable()) {
            Assert.assertTrue(assertStatement);
        }
    }

    /**
     * 判断当前package是否在前台。
     * 
     * @param context
     * @return
     */
    public static boolean isApplicationForeground(final Context context) {
        final ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (tasks.isEmpty()) {
            return false;
        }

        final ComponentName topActivity = tasks.get(0).topActivity;
        return topActivity.getPackageName().equals(context.getPackageName());
    }

    /**
     * 判断在前台的是否是当前package的className组件
     * 
     * @param context
     * @param className
     * @return
     */
    public static boolean isApplicationForeground(final Context context, final String className) {
        final ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (tasks.isEmpty()) {
            return false;
        }

        final ComponentName topActivity = tasks.get(0).topActivity;
        return topActivity.getPackageName().equals(context.getPackageName())
                && className.equals(topActivity.getClassName());
    }

    public static void DebugAssert(final boolean assertStatement) {
        if (GlobalData.isDebuggable()) {
            Assert.assertTrue(assertStatement);
        }
    }
    
    public static boolean isScreenOn(final Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    public static boolean isValidUrl(final String strUrl) {
        if (TextUtils.isEmpty(strUrl)) {
            return false;
        }
        try {
            new URL(strUrl);
            return true;
        } catch (final MalformedURLException e) {
            return false;
        }
    }

    // 是否是中国移动
    public static boolean isChinaMobile(final Context context) {
        final TelephonyManager telManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        final String operator = telManager.getSimOperator();
        return ("46000".equals(operator) || "46002".equals(operator)) || "46007".equals(operator);
    }

    // 中国联通
    public static boolean isChinaUnicom(final Context context) {
        final TelephonyManager telManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        final String operator = telManager.getSimOperator();
        return "46001".equals(operator);
    }

    // 中国电信
    public static boolean isChinaTelecom(final Context context) {
        final TelephonyManager telManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        final String operator = telManager.getSimOperator();
        return "46003".equals(operator);
    }

}
