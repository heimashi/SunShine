
package com.sw.sun;

import android.app.Application;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;

import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.common.thread.ThreadPoolManager;
import com.sw.sun.release.SunBuildSettings;

public class SunApplication extends Application {

    public static final String DISK_CACHE_DIR = Environment.getExternalStorageDirectory()
            + "/sun/cache/image/";

    public static final String IMAGE_CAMERA_PATH = Environment.getExternalStorageDirectory()
            + "/sun/camera/image";

    public static final String VIDEO_CAMERA_PATH = Environment.getExternalStorageDirectory()
            + "/sun/camera/video";

    private static final String LOG_PATH = "/sun/logs/";

    private static final String LOG_PREFIX = "sun";

    private static final int LOG_SAVE_DAYS = 3;

    @Override
    public void onCreate() {
        if (SunBuildSettings.IS_DEBUG_BUILD || SunBuildSettings.IS_NO_CHANNEL
                || SunBuildSettings.IS_RC_BUILD) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                // .detectLeakedClosableObjects()
                        .detectActivityLeaks() // 探测Activity内存泄漏
                        .detectLeakedSqlLiteObjects() // 探测SQLite数据库操作
                        .penaltyLog() // 打印logcat
                        .penaltyDeath().build());

                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectNetwork()
                        .penaltyLog().penaltyDeath().build());
            }
        }
        super.onCreate();
        ThreadPoolManager.init();
        SunLogger logger = new SunLogger(LOG_PREFIX);
        logger.init(SunApplication.this, LOG_PATH, LOG_SAVE_DAYS);
        MyLog.setLogger(logger);
        final int s = MyLog.ps("com.sw.sun.SunApplication start");
        GlobalData.initialize(this);
        // TODO

        MyLog.warn("sun application starts. ");
        MyLog.pe(s);
    }

}
