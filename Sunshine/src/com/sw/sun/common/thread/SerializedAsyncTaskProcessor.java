
package com.sw.sun.common.thread;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sw.sun.common.logger.MyLog;

public class SerializedAsyncTaskProcessor {

    private static final int MSG_BEFORE_EXECUTE = 0;

    private static final int MSG_AFTER_EXECUTE = 1;

    private ProcessPackageThread mProcessThread;

    private Handler mMainThreadHandler = null;

    private volatile boolean threadQuit = false;

    private final boolean mIsDaemon;

    private volatile SerializedAsyncTask mCurrentTask;

    // 创建异步调度器
    public SerializedAsyncTaskProcessor() {
        this(false);
    }

    // 创建异步调度器，会创建一个跑在UI线程的handler来作为控制
    public SerializedAsyncTaskProcessor(boolean isDaemon) {
        mMainThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SerializedAsyncTask task = (SerializedAsyncTask) msg.obj;
                if (msg.what == MSG_BEFORE_EXECUTE) {
                    task.preProcess();
                } else if (msg.what == MSG_AFTER_EXECUTE) {
                    task.postProcess();
                }
                super.handleMessage(msg);
            }
        };
        mIsDaemon = isDaemon;
    }

    // 添加一个异步任务，在ui线程调用
    public synchronized void addNewTask(SerializedAsyncTask task) {
        if (mProcessThread == null) {
            mProcessThread = new ProcessPackageThread();
            mProcessThread.setDaemon(mIsDaemon);
            mProcessThread.start();
        }
        mProcessThread.insertTask(task);
    }

    public void clearTask() {
        if (mProcessThread != null) {
            mProcessThread.mTasks.clear();
        }
    }

    public void addNewTaskWithDelayed(final SerializedAsyncTask task, long delay) {
        mMainThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                addNewTask(task);
            }
        }, delay);
    }

    public SerializedAsyncTask getCurrentTask() {
        return mCurrentTask;
    }

    public void destroy() {
        threadQuit = true;
    }

    public static abstract class SerializedAsyncTask {
        /**
         * 执行异步任务前的回调方法，在ui线程调用。 目前的实现并不保证preProcess在process之前调用。
         */
        public void preProcess() {
        };

        /**
         * 异步任务
         */
        public abstract void process();

        /**
         * 执行异步任务之后的回调方法，在ui线程调用
         */
        public void postProcess() {
        };
    }

    private class ProcessPackageThread extends Thread {

        private final LinkedBlockingQueue<SerializedAsyncTask> mTasks = new LinkedBlockingQueue<SerializedAsyncTask>();

        private static final String THREAD_NAME = "PackageProcessor";

        public ProcessPackageThread() {
            super(THREAD_NAME);
        }

        // 从ui线程调用
        public void insertTask(SerializedAsyncTask task) {
            mTasks.add(task);
        }

        @Override
        public void run() {
            while (!threadQuit) {
                try {
                    mCurrentTask = mTasks.poll(1, TimeUnit.SECONDS);
                    if (mCurrentTask != null) {
                        Message msg = mMainThreadHandler.obtainMessage(MSG_BEFORE_EXECUTE,
                                mCurrentTask);
                        mMainThreadHandler.sendMessage(msg);
                        mCurrentTask.process();
                        msg = mMainThreadHandler.obtainMessage(MSG_AFTER_EXECUTE, mCurrentTask);
                        mMainThreadHandler.sendMessage(msg);
                    }
                } catch (InterruptedException e) {
                    MyLog.e(e);
                }
            }
        }
    }

}
