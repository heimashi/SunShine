
package com.sw.sun;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import com.google.code.microlog4android.appender.FileAppender;
import com.google.code.microlog4android.config.PropertyConfigurator;
import com.sw.sun.common.file.SDCardUtils;
import com.sw.sun.common.logger.LoggerInterface;
import com.sw.sun.common.thread.SerializedAsyncTaskProcessor;
import com.sw.sun.common.thread.SerializedAsyncTaskProcessor.SerializedAsyncTask;
import com.sw.sun.R;

public class SunLogger implements LoggerInterface {

    private static Logger sLogger;

    private static SimpleDateFormat sDateFormatter = new SimpleDateFormat("MM-dd HH:mm:ss aaa");

    private static SerializedAsyncTaskProcessor mAsyncProcessor;

    private static SimpleDateFormat sLogFileFormatter = new SimpleDateFormat("yyyy-MM-dd");

    private static int sEffectiveDay = 2;

    private static List<Pair<String, Throwable>> logs = Collections
            .synchronizedList(new ArrayList<Pair<String, Throwable>>());

    public static String sLogRoot;

    private Context mContext;

    private String mTag;

    public SunLogger(String tag) {
        mTag = tag;
    }

    public void init(Context context, String root, int effectiveDay) {
        mContext = context.getApplicationContext();
        sLogRoot = root;
        PropertyConfigurator config = PropertyConfigurator.getConfigurator(mContext);
        config.configure(R.raw.microlog);
        sLogger = LoggerFactory.getLogger();
        mAsyncProcessor = new SerializedAsyncTaskProcessor(true);
        sEffectiveDay = effectiveDay;
    }

    public static void destory() {
        mAsyncProcessor.destroy();
    }

    @Override
    public void log(final String text, final Throwable t) {

        if (mAsyncProcessor == null) {
            return;
        }

        logs.add(new Pair<String, Throwable>(String.format("%1$s %2$s",
                sDateFormatter.format(new Date()), text), t));
        mAsyncProcessor.addNewTask(new SerializedAsyncTask() {

            @Override
            public void process() {
                if (logs.isEmpty())
                    return;
                try {
                    try {
                        if (SDCardUtils.isSDCardBusy() || SDCardUtils.isSDCardFull()) {
                            while (!logs.isEmpty()) {
                                Pair<String, Throwable> pair = logs.remove(0);
                                Log.v(mTag, pair.first, pair.second);
                            }
                            return;
                        }
                    } catch (Exception e1) {
                        return;
                    }

                    final long now = System.currentTimeMillis();

                    File logDir = new File(Environment.getExternalStorageDirectory(), sLogRoot);
                    if (logDir.isDirectory()) {
                        File[] oldFiles = logDir.listFiles(new FilenameFilter() {

                            @Override
                            public boolean accept(File dir, String filename) {
                                if (!filename.endsWith(".txt"))
                                    return false;

                                try {
                                    // 前几天的日志文件
                                    long date = sLogFileFormatter.parse(
                                            filename.substring(0, filename.length() - 4)).getTime();
                                    return (now - date > DateUtils.DAY_IN_MILLIS * sEffectiveDay);
                                } catch (ParseException e) {
                                    return false;
                                }
                            }
                        });
                        if (oldFiles != null) {
                            for (File file : oldFiles) {
                                file.delete();
                            }
                        }
                    } else {
                        logDir.mkdirs();
                    }

                    String today = sLogFileFormatter.format(new Date(now));
                    String newFileName = sLogRoot + today + ".txt";
                    FileAppender fa = (FileAppender) sLogger.getAppender(0);
                    fa.setFileName(newFileName);
                    fa.setAppend(true);
                    fa.open();
                    while (!logs.isEmpty()) {
                        Pair<String, Throwable> pair = logs.remove(0);
                        sLogger.debug(pair.first, pair.second);
                    }
                    fa.close();
                } catch (Exception e) {
                    Log.e(mTag, null, e);
                }
            }
        });
    }

    @Override
    public void log(String msg) {
        log(msg, null);
    }

    public static String getLogFolderPath() {
        return sLogRoot;
    }

    @Override
    public void setTag(String tag) {
        mTag = tag;
    }
}
