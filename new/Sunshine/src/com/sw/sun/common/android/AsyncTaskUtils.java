
package com.sw.sun.common.android;

import java.util.concurrent.RejectedExecutionException;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

import com.sw.sun.common.logger.MyLog;
import com.sw.sun.common.thread.ThreadPoolManager;

@SuppressLint("NewApi")
public abstract class AsyncTaskUtils {
    public static <Params, Progress, Result> void exe(int level,
            AsyncTask<Params, Progress, Result> asyncTask, Params... params) {
        try {
            if (Build.VERSION.SDK_INT >= 11) {
                asyncTask.executeOnExecutor(ThreadPoolManager.getExecutorByLevel(level), params);
            } else {
                asyncTask.execute(params);
            }
        } catch (RejectedExecutionException e) {
            MyLog.v("async task pool full");
        }
    }

    public static <Params, Progress, Result> void exeNetWorkTask(
            AsyncTask<Params, Progress, Result> asyncTask, Params... params) {
        try {
            if (Build.VERSION.SDK_INT >= 11) {
                asyncTask
                        .executeOnExecutor(
                                ThreadPoolManager
                                        .getExecutorByLevel(ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_NETWORK),
                                params);
            } else {
                asyncTask.execute(params);
            }
        } catch (RejectedExecutionException e) {
            MyLog.v("async task pool full");
        }
    }

    public static <Params, Progress, Result> void exeIOTask(
            AsyncTask<Params, Progress, Result> asyncTask, Params... params) {
        try {
            if (Build.VERSION.SDK_INT >= 11) {
                asyncTask.executeOnExecutor(ThreadPoolManager
                        .getExecutorByLevel(ThreadPoolManager.ASYNC_EXECUTOR_LEVEL_LOCAL_IO),
                        params);
            } else {
                asyncTask.execute(params);
            }
        } catch (RejectedExecutionException e) {
            MyLog.v("async task pool full");
        }
    }
}
