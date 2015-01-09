
package com.sw.sun.common.network;

import java.util.Map;

public class HttpHeaderInfo {
    public int responseCode;

    public String contentType;

    public String userAgent;

    public String realUrl;

    public Map<String, String> allHeaders;

    @Override
    public String toString() {
        return String.format("resCode = %1$d, headers = %2$s", responseCode, allHeaders.toString());
    }
}
