
package com.sw.sun.account;

import android.text.TextUtils;

public class MyAccount {

    private static MyAccount sInstance = new MyAccount();

    private SunAccount mAccount;

    public static MyAccount getInstance() {
        return sInstance;
    }

    private MyAccount() {
        mAccount = SunAccountManager.getInstance().getAccount();
    }

    public String getNickname() {
        return mAccount.nickname;
    }

    public String getAvatarUrl() {
        return mAccount.avatarUrl;
    }

    public long getUUID() {
        return mAccount.uuid;
    }

    public String getSid() {
        return mAccount.sid;
    }

    public String getTokenV3() {
        return mAccount.tokenV3;
    }

    public boolean hasAccount() {
        return mAccount != null;
    }

    public void refershMyAccount() {
        mAccount = SunAccountManager.getInstance().getAccount();
    }

    public SunAccount getAccount() {
        return mAccount;
    }

    public void updateNickname(String newName) {
        if (!TextUtils.isEmpty(newName)) {
            SunAccountManager.getInstance().setNickname(newName);
            mAccount.nickname = newName;
        }
    }

    public void updateAvatarUrl(String newUrl) {
        if (!TextUtils.isEmpty(newUrl)) {
            SunAccountManager.getInstance().setAvatarUrl(newUrl);
            mAccount.avatarUrl = newUrl;
        }

    }

}
