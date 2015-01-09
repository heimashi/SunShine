
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
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.Build.VERSION_CODES;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.sw.sun.common.logger.MyLog;
import com.sw.sun.common.string.MD5;

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
    
    /**
     * Indicates whether the specified action can be used as an intent. This method queries the package manager for
     * installed packages that can respond to an intent with the specified action. If no suitable package is found, this
     * method returns false.
     * 
     * @param context The application's environment.
     * @param intent The Intent to check for availability.
     * @return True if an Intent with the specified action can be sent and responded to, false otherwise.
     */
    public static boolean isIntentAvailable(final Context context,
            final Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> list = packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
    
    public static int getCurrentVersionCode(Context c) {
        PackageInfo info;
        try {
            info = c.getPackageManager().getPackageInfo(c.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);
        } catch (NameNotFoundException e) {
            MyLog.e("cannot find package" + e);
            return -1;
        }
        return info.versionCode;
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

    // 前两位是an(android)代表操作系统类型，后面是六位是MAC地址的MD5的前6位
    public static String getDeviceId() {
        return "an" + MD5.MD5_32(getMacAddress()).substring(0, 6);
    }
    
    public static String getMacAddress() {
        String macAddress = "00:00:00:00:00:00";
        try {
            WifiManager wifi = (WifiManager) GlobalData.app()
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            macAddress = info.getMacAddress();
        } catch (Exception e) {
            MyLog.e(e);
        }
        return macAddress;
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
    
    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;
    }
    
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;
    }

}
