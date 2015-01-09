
package com.sw.sun.common.thread;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sw.sun.common.logger.MyLog;
import com.sw.sun.common.thread.SerializedAsyncTaskProcessor.SerializedAsyncTask;

public class ThreadPoolManager {

    public static final int ASYNC_EXECUTOR_LEVEL_URGENT = 0;

    public static final int ASYNC_EXECUTOR_LEVEL_LOCAL_IO = 1;

    public static final int ASYNC_EXECUTOR_LEVEL_NETWORK = 2;

    public static final int ASYNC_EXECUTOR_LEVEL_IMAGE = 3;

    public static final int ASYNC_EXECUTOR_LEVEL_AVATAR = 4;

    private static ThreadPoolExecutor sExecutors[] = new ThreadPoolExecutor[ASYNC_EXECUTOR_LEVEL_AVATAR + 1];

    public static void init() {
        final ThreadPoolExecutor backupExe = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        RejectedExecutionHandler rehHandler = new RejectedExecutionHandler() {

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                backupExe.execute(r);
                MyLog.v("Thread pool executor: reject work, put into backup pool");
            }
        };
        sExecutors[ASYNC_EXECUTOR_LEVEL_URGENT] = new ThreadPoolExecutor(3, 3, 60,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), rehHandler);
        sExecutors[ASYNC_EXECUTOR_LEVEL_LOCAL_IO] = new ThreadPoolExecutor(3, 10, 60,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), rehHandler);
        sExecutors[ASYNC_EXECUTOR_LEVEL_NETWORK] = new ThreadPoolExecutor(3, 10, 60,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), rehHandler);
        sExecutors[ASYNC_EXECUTOR_LEVEL_IMAGE] = new ThreadPoolExecutor(3, 10, 60,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), rehHandler);
        sExecutors[ASYNC_EXECUTOR_LEVEL_AVATAR] = new ThreadPoolExecutor(3, 5, 60,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), rehHandler);
    }

    public static Executor getExecutorByLevel(int level) {
        if (level < ASYNC_EXECUTOR_LEVEL_URGENT || level > ASYNC_EXECUTOR_LEVEL_AVATAR) {
            throw new IllegalArgumentException("wrong level");
        }
        return sExecutors[level];
    }

    /**
     * 在后台线程池中执行runnable所代表的任务。 目前只持续一些独立的，彼此间没有依赖和同步关系的任务。
     * 
     * @param runnable
     */
    public static void execute(final Runnable runnable, int level) {
        getExecutorByLevel(level).execute(runnable);
    }

    /**
     * 在后台线程池中执行task所代表的任务。 目前只支持一些独立的，彼此间没有依赖和同步关系的任务。 这些task的
     * preProcess和postProcess会被忽略。
     * 
     * @param task
     */
    public static void execute(final SerializedAsyncTask task, final int level) {
        execute(new Runnable() {
            @Override
            public void run() {
                task.process();
            }
        }, level);
    }

}
