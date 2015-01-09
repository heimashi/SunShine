
package com.sw.sun.common.network;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.os.Build;
import android.text.TextUtils;

import com.sw.sun.account.MyAccount;
import com.sw.sun.common.android.CommonUtils;
import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.common.string.Base64Coder;
import com.sw.sun.common.string.XMStringUtils;

public class HttpUtils {

    // User-Agent: <app版本> (<设备>;<OS版本>;<语言>) 其他扩展
    private static String USER_AGENT = null;

    public synchronized static String buildUserAgent() {
        if (USER_AGENT == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Shank ");
            sb.append(CommonUtils.getCurrentVersionCode(GlobalData.app()));
            sb.append(" (");
            sb.append(XMStringUtils.join(new String[] {
                    Build.MODEL, String.valueOf(Build.VERSION.SDK_INT),
                    Locale.getDefault().getLanguage()
            }, ";"));
            sb.append(") ");
            sb.append(Build.VERSION.RELEASE);
            USER_AGENT = sb.toString();
        }
        return USER_AGENT;
    }

    public static String doHttpPost(final String url, final List<NameValuePair> nameValuePairs)
            throws IOException {
        if (!Network.hasNetwork(GlobalData.app())) {
            MyLog.e("没有网络，不进行http连接");
            throw new IOException("doHttpPost");
        }
        String result = null;
        try {
            long start = System.currentTimeMillis();
            result = Network.doHttpPost(GlobalData.app(), url, nameValuePairs, null,
                    buildUserAgent(), null);
            // 注意，此处只是检查并且重新设置sid, token; 不要自动重试，防止出现死循环。
            needRefreshToken(result);

            MyLog.v("http post to " + url + ", params=" + nameValuePairs + ", cost time="
                    + (System.currentTimeMillis() - start) + ", result=" + result);
        } catch (final IOException e) {
            MyLog.e("error to call url:" + url + " error:" + e.getMessage(), e);
            throw e;
        }
        return result;
    }

    public static String doHttpPostV3(final String url, final List<NameValuePair> nameValuePairs)
            throws IOException {
        prepareParametersV3(nameValuePairs);
        return doHttpPost(url, nameValuePairs);
    }

    public static void needRefreshToken(final String result) {
        // TODO
    }

    public static boolean refreshSidToken() {
        // TODO
        return false;
    }

    public static boolean prepareParametersV3(final List<NameValuePair> nameValuePairs) {

        String sid = MyAccount.getInstance().getSid();
        String tokenV3 = MyAccount.getInstance().getTokenV3();

        if (TextUtils.isEmpty(sid) || TextUtils.isEmpty(tokenV3)) {
            // 没有sid和token,
            if (!refreshSidToken()) {
                return false;
            }
            sid = MyAccount.getInstance().getSid();
            tokenV3 = MyAccount.getInstance().getTokenV3();
        }

        return prepareParametersV3(sid, tokenV3, nameValuePairs);
    }

    private static boolean prepareParametersV3(final String sid, final String tokenV3,
            final List<NameValuePair> nameValuePairs) {
        if (TextUtils.isEmpty(sid) || TextUtils.isEmpty(tokenV3)) {
            // 没有sid和token,
            return false;
        }

        final String md5 = getKeyFromParamsV3(nameValuePairs, sid);

        nameValuePairs.add(new BasicNameValuePair("s", URLEncoder.encode(md5)));
        nameValuePairs.add(new BasicNameValuePair("token", tokenV3));
        return true;
    }

    private static String getKeyFromParamsV3(final List<NameValuePair> nameValuePairs,
            final String salt) {
        nameValuePairs.add(new BasicNameValuePair("time",
                String.valueOf(System.currentTimeMillis())));

        Collections.sort(nameValuePairs, new Comparator<NameValuePair>() {

            @Override
            public int compare(final NameValuePair p1, final NameValuePair p2) {
                return p1.getName().compareTo(p2.getName());
            }
        });

        final StringBuilder keyBuilder = new StringBuilder();
        boolean isFirst = true;
        for (final NameValuePair nvp : nameValuePairs) {

            if (!isFirst) {
                keyBuilder.append("&");
            }

            keyBuilder.append(nvp.getName()).append("=").append(nvp.getValue());
            isFirst = false;
        }

        keyBuilder.append("&").append(salt);

        final String key = keyBuilder.toString();
        final byte[] keyBytes = key.getBytes();

        return XMStringUtils.getMd5Digest(new String(Base64Coder.encode(keyBytes)));
    }

}
