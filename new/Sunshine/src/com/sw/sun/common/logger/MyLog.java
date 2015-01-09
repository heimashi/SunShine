
package com.sw.sun.common.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Process;

import com.sw.sun.common.string.XMStringUtils;

public abstract class MyLog {

    // 定义的log等级。越高越严重。
    /**
     * 任何可以帮助了解程序行为的日志
     */
    public static final int INFO = 0;

    /**
     * 在程序的关键地方打印的方便定位问题的日志
     */
    public static final int DEBUG = 1;

    /**
     * 程序行为或者数据出现不一致。
     */
    public static final int WARN = 2;

    /**
     * 程序中出错了。
     */
    public static final int ERROR = 4;

    /**
     * 记录程序中的致命的错误。
     */
    public static final int FATAL = 5;

    private static int LOG_LEVEL = WARN;

    private final static HashMap<Integer, Long> mStartTimes = new HashMap<Integer, Long>();

    private final static HashMap<Integer, String> mActionNames = new HashMap<Integer, String>();

    private static final Integer NEGATIVE_CODE = Integer.valueOf(-1);

    private static AtomicInteger mCodeGenerator = new AtomicInteger(1);

    private static LoggerInterface logger = new DefaultLogger();

    public static void setLogger(LoggerInterface newLogger) {
        logger = newLogger;
    }

    /**
     * 在WARN level记录日志。
     * 
     * @param msg
     */
    public static void warn(String msg) {
        log(WARN, "[Thread:" + Thread.currentThread().getId() + "] " + msg);
    }

    /**
     * 在INFO level记录日志。
     * 
     * @param msg
     */
    public static void info(String msg) {
        log(INFO, msg);
    }

    /**
     * 在DEBUG level记录日志。
     * 
     * @param msg
     */
    public static void v(String msg) {
        log(DEBUG, "[Thread:" + Thread.currentThread().getId() + "] " + msg);
    }

    /**
     * 在DEBUG level记录日志。
     * 
     * @param msg
     */
    public static void v(Object[] msgs) {
        log(DEBUG, XMStringUtils.join(msgs, ","));
    }

    /**
     * 在ERROR level记录日志。
     * 
     * @param msg
     */
    public static void e(String msg, Throwable t) {
        log(ERROR, msg, t);
    }

    /**
     * 在ERROR level记录日志。
     * 
     * @param msg
     */
    public static void e(Throwable t) {
        log(ERROR, t);
    }

    /**
     * 在ERROR level记录日志。
     * 
     * @param msg
     */
    public static void e(String msg) {
        log(ERROR, msg);
    }

    // heapinfo调用比较耗时，使用需谨慎
    public static void heapInfo(Context context, String msg) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        int myPid = Process.myPid();
        int pids[] = {
            myPid
        };
        Debug.MemoryInfo memInfo[] = activityManager.getProcessMemoryInfo(pids);
        MyLog.info("+++Heap Info+++" + msg + " total:" + memInfo[0].getTotalPss() + ", managed:"
                + memInfo[0].dalvikPss + ", native:" + memInfo[0].nativePss);

    }

    /**
     * 开始给一个动作计时 (ps means perf start)
     * 
     * @param action
     * @return 一个code用于pe
     */
    public static Integer ps(String action) {
        if (LOG_LEVEL <= DEBUG) {
            Integer code = Integer.valueOf(mCodeGenerator.incrementAndGet());
            mStartTimes.put(code, System.currentTimeMillis());
            mActionNames.put(code, action);
            logger.log(action + " starts");
            return code;
        }

        return NEGATIVE_CODE;
    }

    /**
     * 一个动作记事结束
     * 
     * @param ps 产生的code
     */
    public static void pe(Integer code) {
        if (LOG_LEVEL <= DEBUG) {
            if (!mStartTimes.containsKey(code)) {
                return;
            }
            long startTime = mStartTimes.remove(code);
            String action = mActionNames.remove(code);
            long time = System.currentTimeMillis() - startTime;
            logger.log(action + " ends in " + time + " ms");
        }
    }

    public static void log(int level, String msg) {
        if (level >= LOG_LEVEL) {
            logger.log(msg);
        }
    }

    public static void log(int level, Throwable t) {
        if (level >= LOG_LEVEL) {
            logger.log("", t);
        }
    }

    public static void log(int level, String msg, Throwable t) {
        if (level >= LOG_LEVEL) {
            logger.log(msg, t);
        }
    }

    public static void setLogLevel(int level) {
        if (level < INFO || level > FATAL) {
            log(WARN, "set log level as " + level);
        }
        LOG_LEVEL = level;
    }

    public static int getLogLevel() {
        return LOG_LEVEL;
    }

    /**
     * Print the current callstack
     * 
     * @param TAG
     * @param message
     */
    public static void printCallStack(final String message) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        printWriter.println(message);
        printWriter.println(String.format("Current thread id (%s); thread name (%s)", Thread
                .currentThread().getId(), Thread.currentThread().getName()));

        new Throwable("Call stack").printStackTrace(printWriter);
        v(result.toString());
    }
}
