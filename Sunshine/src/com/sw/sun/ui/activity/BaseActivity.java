
package com.sw.sun.ui.activity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.android.SystemBarTintManager;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.R;

public abstract class BaseActivity extends Activity {

    public static final int NOT_ACTIVE = -1;

    private static String sForgroundActivityClassName = "";

    public static boolean isMIUIV6 = true;

    protected Activity mContext;

    public static boolean isForGround(String name) {
        return sForgroundActivityClassName.equalsIgnoreCase(name);
    }

    @TargetApi(19)
    public static void setTranslucentStatus(Activity activity, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private static void setFullScreen(Activity activity) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        winParams.flags |= bits;
        win.setAttributes(winParams);
    }

    public static void setStatusBarOfProfile(final Activity act) {
        setStatusBarOfProfile(act, false);
    }

    public static void cancelStatusBar(final Activity act) {
        if (!isMIUIV6) {
            return;
        }
        Window window = act.getWindow();
        // Class clazz = window.getClass();
        // try {
        // int tranceFlag = 0;
        // int darkModeFlag = 0;
        // Class layoutParams = Class
        // .forName("android.view.MiuiWindowManager$LayoutParams");
        // // 透明
        // Field field = layoutParams
        // .getField("EXTRA_FLAG_STATUS_BAR_TRANSPARENT");
        // tranceFlag = field.getInt(layoutParams);
        // // 黑色样式
        // Method extraFlagField = clazz.getMethod("setExtraFlags", int.class,
        // int.class);
        // extraFlagField.invoke(window, tranceFlag | darkModeFlag, tranceFlag
        // | darkModeFlag);
        // window.getDecorView().setBackgroundColor(color);
        // isMIUIV6 = true;

        setTranslucentStatus(act, false);
        SystemBarTintManager tintManager = new SystemBarTintManager(act);
        tintManager.setStatusBarTintEnabled(false);
        tintManager.setNavigationBarTintEnabled(false);
        // return;
        // } catch (NoSuchMethodException e) {
        // e.printStackTrace();
        // } catch (ClassNotFoundException e) {
        // e.printStackTrace();
        // } catch (NoSuchFieldException e) {
        // e.printStackTrace();
        // } catch (IllegalAccessException e) {
        // e.printStackTrace();
        // } catch (IllegalArgumentException e) {
        // e.printStackTrace();
        // } catch (InvocationTargetException e) {
        // e.printStackTrace();
        // }
        // isMIUIV6 = false;
    }

    public static void setStatusBarOfProfile(final Activity act, boolean isDark) {
        if (!isMIUIV6) {
            return;
        }
        Window window = act.getWindow();
        Class clazz = window.getClass();
        try {
            int tranceFlag = 0;
            int darkModeFlag = 0;
            Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            // 透明
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_TRANSPARENT");
            tranceFlag = field.getInt(layoutParams);
            // 黑色样式
            if (isDark) {
                field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
            }
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(window, tranceFlag | darkModeFlag, tranceFlag | darkModeFlag);
            isMIUIV6 = true;
            setFullScreen(act);
            return;
        } catch (NoSuchMethodException e) {
            MyLog.e(e);
        } catch (ClassNotFoundException e) {
            MyLog.e(e);
        } catch (NoSuchFieldException e) {
            MyLog.e(e);
        } catch (IllegalAccessException e) {
            MyLog.e(e);
        } catch (IllegalArgumentException e) {
            MyLog.e(e);
        } catch (InvocationTargetException e) {
            MyLog.e(e);
        }
        isMIUIV6 = false;
    }
    
    
    
    @Override
	public void finish() {
		super.finish();
		if (useAnimation()) {
            overridePendingTransition(0, R.anim.compose_out);
        }
	}

	protected boolean useAnimation() {
        return true;
    }

    public static void setStatusBar(final Activity act) {
        // int color = act.getResources().getColor(R.color.color_title_back);
        // setStatusBar(act, color, true);
    }

    public static void setStatusBar(final Activity act, int color, boolean isDark) {
        if (!isMIUIV6) {
            return;
        }
        Window window = act.getWindow();
        Class clazz = window.getClass();
        try {
            int tranceFlag = 0;
            int darkModeFlag = 0;
            Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            // 透明
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_TRANSPARENT");
            tranceFlag = field.getInt(layoutParams);
            // 黑色样式
            if (isDark) {
                field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
            }
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(window, tranceFlag | darkModeFlag, tranceFlag | darkModeFlag);
            // window.getDecorView().setBackgroundColor(color);
            isMIUIV6 = true;

            setTranslucentStatus(act, true);
            SystemBarTintManager tintManager = new SystemBarTintManager(act);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setNavigationBarTintEnabled(true);
            tintManager.setTintColor(color);
            return;
        } catch (NoSuchMethodException e) {
            MyLog.e(e);
        } catch (ClassNotFoundException e) {
            MyLog.e(e);
        } catch (NoSuchFieldException e) {
            MyLog.e(e);
        } catch (IllegalAccessException e) {
            MyLog.e(e);
        } catch (IllegalArgumentException e) {
            MyLog.e(e);
        } catch (InvocationTargetException e) {
            MyLog.e(e);
        }
        isMIUIV6 = false;
    }

    @Override
    public void setContentView(int layoutResID) {
        View view = LayoutInflater.from(this).inflate(layoutResID, null);
        if (isDoSetStausBar()) {
            setContentView(view);
        } else {
            super.setContentView(layoutResID);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void setContentView(View view) {
        if (isDoSetStausBar() && isMIUIV6) {
            view.setFitsSystemWindows(true);
        }
        super.setContentView(view);

    }

    protected boolean isDoSetStausBar() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	if (useAnimation()) {
            overridePendingTransition(R.anim.compose_in, 0);
        }
        super.onCreate(savedInstanceState);
        MyLog.v("BaseActivity " + this.getClass().getName());
        mContext = this;

        if (isDoSetStausBar()) {
            setStatusBar(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sForgroundActivityClassName = this.getClass().getName();
        // TODO 统计前台时长
        // if (GlobalData.sMiliaoStartTime == GlobalData.MILIAO_NOT_ACTIVE) {
        // GlobalData.sMiliaoStartTime = System.currentTimeMillis();
        // }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO 统计前台时长
        // if (!CommonUtils.isApplicationForeground(this)
        // || !CommonUtils.isScreenOn(this)
        // && GlobalData.sMiliaoStartTime != GlobalData.MILIAO_NOT_ACTIVE) {
        // MyLog.v("open miliao at " + GlobalData.sMiliaoStartTime
        // + ", and close it at " + System.currentTimeMillis());
        // GlobalData.sMiliaoStartTime = GlobalData.MILIAO_NOT_ACTIVE;
        // }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sForgroundActivityClassName = "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Runtime.getRuntime().gc();
        GlobalData.globalHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                Runtime.getRuntime().gc();
            }
        }, 1000);
    }
}
