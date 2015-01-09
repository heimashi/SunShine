
package com.sw.sun.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.string.RC4Cryption;

/**
 * 管理account, 进行本地持久化，即把account的每个字段存入一个preference文件中
 * 
 * @author kevin
 */
public class SunAccountManager {

    private static final int MODE = Context.MODE_PRIVATE;

    private static final String PREFERENCE_NAME = "account";

    private static final String S_KEY = "SNTrewWuKnk/g2G+WUdsfQyeZewRLQ=="; // 加密私钥，随意定义的，请不要改变

    private static final String S_ID = "14150783420726873"; // 加密ID，随意定义的，请不要改变

    private static final String USERNAME = "username";

    private static final String UUID = "uuid";

    private static final String NICKNAME = "nickname";

    private static final String AVATAR_URL = "avatar_url";

    public static final String EN_OLD_PASSWORD = "en_old_pwd";

    private static final String EN_PASSWORD = "en_password";

    private static final String EN_TOKEN_V3 = "en_token_v3";

    private static final String EN_SID = "en_sid";

    private static final String EN_PASSTOKEN = "en_passtoken";

    private static final String EN_SERVICETOKEN = "en_servicetoken";

    private static final String EN_PSECURITY = "en_psecurity";

    private static final String EN_SSECURITY = "en_ssecurity";

    private static final byte[] KEY_FOR_RC4 = RC4Cryption.generateKeyForRC4(S_KEY, S_ID);

    private static SunAccountManager sInstance = new SunAccountManager();

    private final Context mContext;

    public static SunAccountManager getInstance() {
        return sInstance;
    }

    private SunAccountManager() {
        mContext = GlobalData.app();
    }

    public boolean hasAccount() {
        return !mContext.getSharedPreferences(PREFERENCE_NAME, MODE).getAll().isEmpty();
    }

    public void setNickname(String name) {
        Editor edit = mContext.getSharedPreferences(PREFERENCE_NAME, MODE).edit();
        edit.putString(NICKNAME, name);
        edit.commit();
    }

    public void setAvatarUrl(String url) {
        Editor edit = mContext.getSharedPreferences(PREFERENCE_NAME, MODE).edit();
        edit.putString(AVATAR_URL, url);
        edit.commit();
    }

    public void setAccount(SunAccount account) {
        Editor edit = mContext.getSharedPreferences(PREFERENCE_NAME, MODE).edit();
        edit.putString(USERNAME, account.username);
        edit.putLong(UUID, account.uuid);
        edit.putString(NICKNAME, account.nickname);
        edit.putString(AVATAR_URL, account.avatarUrl);

        if (!TextUtils.isEmpty(account.oldPwd)) {
            edit.putString(EN_OLD_PASSWORD, RC4Cryption.encrypt(KEY_FOR_RC4, account.oldPwd));
        }
        if (!TextUtils.isEmpty(account.password)) {
            edit.putString(EN_PASSWORD, RC4Cryption.encrypt(KEY_FOR_RC4, account.password));
        }
        if (!TextUtils.isEmpty(account.tokenV3)) {
            edit.putString(EN_TOKEN_V3, RC4Cryption.encrypt(KEY_FOR_RC4, account.tokenV3));
        }
        if (!TextUtils.isEmpty(account.sid)) {
            edit.putString(EN_SID, RC4Cryption.encrypt(KEY_FOR_RC4, account.sid));
        }
        if (!TextUtils.isEmpty(account.passToken)) {
            edit.putString(EN_PASSTOKEN, RC4Cryption.encrypt(KEY_FOR_RC4, account.passToken));
        }
        if (!TextUtils.isEmpty(account.serviceToken)) {
            edit.putString(EN_SERVICETOKEN, RC4Cryption.encrypt(KEY_FOR_RC4, account.serviceToken));
        }
        if (!TextUtils.isEmpty(account.pSecurity)) {
            edit.putString(EN_PSECURITY, RC4Cryption.encrypt(KEY_FOR_RC4, account.pSecurity));
        }
        if (!TextUtils.isEmpty(account.sSecurity)) {
            edit.putString(EN_SSECURITY, RC4Cryption.encrypt(KEY_FOR_RC4, account.sSecurity));
        }
        edit.commit();
    }

    public SunAccount getAccount() {
        if (!hasAccount()) {
            return null;
        }
        SharedPreferences sp = mContext.getSharedPreferences(PREFERENCE_NAME, MODE);
        SunAccount account = new SunAccount();
        account.uuid = sp.getLong(UUID, 0);
        if (account.uuid == 0) {
            return null;
        }
        account.username = sp.getString(USERNAME, null);
        account.nickname = sp.getString(NICKNAME, null);
        account.avatarUrl = sp.getString(AVATAR_URL, null);

        String oldPwd = sp.getString(EN_OLD_PASSWORD, null);
        if (!TextUtils.isEmpty(oldPwd)) {
            account.oldPwd = new String(RC4Cryption.decrypt(KEY_FOR_RC4, oldPwd));
        }

        String password = sp.getString(EN_PASSWORD, null);
        if (!TextUtils.isEmpty(password)) {
            account.password = new String(RC4Cryption.decrypt(KEY_FOR_RC4, password));
        }

        String tokenV3 = sp.getString(EN_TOKEN_V3, null);
        if (!TextUtils.isEmpty(tokenV3)) {
            account.tokenV3 = new String(RC4Cryption.decrypt(KEY_FOR_RC4, tokenV3));
        }

        String sid = sp.getString(EN_SID, null);
        if (!TextUtils.isEmpty(sid)) {
            account.sid = new String(RC4Cryption.decrypt(KEY_FOR_RC4, sid));
        }

        String passToken = sp.getString(EN_PASSTOKEN, null);
        if (!TextUtils.isEmpty(passToken)) {
            account.passToken = new String(RC4Cryption.decrypt(KEY_FOR_RC4, passToken));
        }

        String serviceToken = sp.getString(EN_SERVICETOKEN, null);
        if (!TextUtils.isEmpty(serviceToken)) {
            account.serviceToken = new String(RC4Cryption.decrypt(KEY_FOR_RC4, serviceToken));
        }

        String pSecurity = sp.getString(EN_PSECURITY, null);
        if (!TextUtils.isEmpty(pSecurity)) {
            account.pSecurity = new String(RC4Cryption.decrypt(KEY_FOR_RC4, pSecurity));
        }

        String sSecurity = sp.getString(EN_SSECURITY, null);
        if (!TextUtils.isEmpty(sSecurity)) {
            account.sSecurity = new String(RC4Cryption.decrypt(KEY_FOR_RC4, sSecurity));
        }

        return account;
    }

    public void removeAccount() {
        Editor edit = mContext.getSharedPreferences(PREFERENCE_NAME, MODE).edit();
        edit.clear();
        edit.commit();
    }
}
