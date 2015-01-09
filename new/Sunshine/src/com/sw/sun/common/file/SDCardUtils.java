
package com.sw.sun.common.file;

import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;

public class SDCardUtils {

    public static final String ROOT_PATH = "/";

    public static final String SD_CARD_PATH = Environment.getExternalStorageDirectory().getPath();

    /**
     * 没有检测到SD卡
     * 
     * @return
     */
    public static boolean isSDCardUnavailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED);
    }

    /**
     * @return true 如果SD卡处于不可读写的状态
     */
    public static boolean isSDCardBusy() {
        return !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 检查ＳＤ卡是否已满。如果ＳＤ卡的剩余空间小于１００ｋ，则认为ＳＤ卡已满。
     * 
     * @param context
     * @return
     */
    public static boolean isSDCardFull() {
        return getSDCardAvailableBytes() <= (100 * 1024);
    }

    public static boolean isSDCardUseful() {
        return (!isSDCardBusy()) && (!isSDCardFull()) && (!isSDCardUnavailable());
    }

    /**
     * 获取ＳＤ卡的剩余字节数。
     * 
     * @return
     */
    public static long getSDCardAvailableBytes() {
        if (isSDCardBusy()) {
            return 0;
        }

        final File path = Environment.getExternalStorageDirectory();
        if (path == null || TextUtils.isEmpty(path.getPath())) {
            return 0;
        }
        final StatFs stat = new StatFs(path.getPath());
        final long blockSize = stat.getBlockSize();
        final long availableBlocks = stat.getAvailableBlocks();
        return blockSize * (availableBlocks - 4);
    }

    private static boolean checkFsWritable() {
        // Create a temporary file to see whether a volume is really writeable.
        // It's important not to put it in the root directory which may have a
        // limit on the number of files.
        String directoryName = Environment.getExternalStorageDirectory().toString() + "/DCIM";
        File directory = new File(directoryName);
        if (!directory.isDirectory()) {
            if (!directory.mkdirs()) {
                return false;
            }
        }
        return directory.canWrite();
    }

    public static boolean isSDCardWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            boolean writable = checkFsWritable();
            return writable;
        }
        return false;
    }
}
