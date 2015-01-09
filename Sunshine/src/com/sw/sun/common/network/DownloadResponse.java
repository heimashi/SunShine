
package com.sw.sun.common.network;

public class DownloadResponse {

    public static final int DOWNLOAD_STATE_CANCEL = 1;

    public static final int DOWNLOAD_STATE_FAILED = 2;

    public static final int DOWNLOAD_STATE_SUCCESS = 3;

    public int responseCode;

    public int downloadState;

    public int downloadBytes;

    public Exception e;

    public DownloadResponse(int responseCode, int downloadState, int downloadBytes, Exception e) {
        this.responseCode = responseCode;
        this.downloadState = downloadState;
        this.downloadBytes = downloadBytes;
        this.e = e;
    }
}
