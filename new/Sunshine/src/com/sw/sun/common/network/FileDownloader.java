
package com.sw.sun.common.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.text.TextUtils;

import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.file.SDCardUtils;
import com.sw.sun.common.logger.MyLog;

public class FileDownloader {

    private static Set<String> sDownloadingFiles = Collections
            .synchronizedSet(new HashSet<String>());

    public static final int DOWNLOAD_STATE_DOWNLOADING = 0;

    public static final int DOWNLOAD_STATE_CANCEL = 1;

    public static final int DOWNLOAD_STATE_FAILED = 2;

    public static final int DOWNLOAD_STATE_SUCCESS = 3;

    private File mOutputFile;

    private String mDownloadUrl;

    private OutputStream mOutputStream;

    private OnDownloadProgress mOnDownloadProgress;

    public FileDownloader(String url, File outputFile) {
        this.mDownloadUrl = url;
        this.mOutputFile = outputFile;
        this.mOutputStream = null;
    }

    public FileDownloader(String url, OutputStream outputStream) {
        this.mDownloadUrl = url;
        this.mOutputStream = outputStream;
        this.mOutputFile = null;
    }

    public FileDownloader(String url, String outputFilePath) {
        this(url, new File(outputFilePath));
    }

    public void setOnDownloadProgress(OnDownloadProgress downloadProgress) {
        this.mOnDownloadProgress = downloadProgress;
    }

    public int downloadFile() {
        if (sDownloadingFiles.contains(mDownloadUrl)) {
            // 正在上传，不需要重新开始
            MyLog.warn("FileDownloader downloading the file, ignore this request!");
            return DOWNLOAD_STATE_DOWNLOADING;
        }

        if ((mOutputFile == null && mOutputStream == null) || TextUtils.isEmpty(mDownloadUrl)
                || SDCardUtils.isSDCardBusy() || !Network.hasNetwork(GlobalData.app())) {
            if (mOnDownloadProgress != null) {
                mOnDownloadProgress.onFailed();
            }
            return DOWNLOAD_STATE_FAILED;
        }
        sDownloadingFiles.add(mDownloadUrl);
        boolean result = false;
        if (mOutputStream != null) {
            result = Network.downloadFile(mDownloadUrl, mOutputStream, HttpUtils.buildUserAgent(),
                    GlobalData.app());
        } else {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mOutputFile);
                result = Network.downloadFile(mDownloadUrl, fos, HttpUtils.buildUserAgent(),
                        GlobalData.app());
            } catch (FileNotFoundException e) {
                MyLog.e(e);
            }
        }
        sDownloadingFiles.remove(mDownloadUrl);
        return result ? DOWNLOAD_STATE_SUCCESS : DOWNLOAD_STATE_FAILED;
    }

    public static interface OnDownloadProgress {
        public void onDownloaded(long downloaded, long totalLength);

        public void onCompleted(String localPath);

        // 这里 canceled 的 语义为 用户手动点击的已知的 停止，暂停，取消
        public void onCanceled();

        public void onFailed();
    }

}
