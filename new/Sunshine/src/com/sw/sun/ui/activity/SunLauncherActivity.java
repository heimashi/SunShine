
package com.sw.sun.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.View.OnClickListener;

import com.sw.sun.R;
import com.sw.sun.common.logger.MyLog;
import com.sw.sun.ui.view.MyTitlebar;

/**
 * 入口Activity
 * 
 * @author csm
 */
@SuppressLint("HandlerLeak")
public class SunLauncherActivity extends BaseActivity implements OnClickListener {

    /**
     * 初始化动画界面标识
     */
    private static final int STATUS_INIT_ANIMATION = 0;

    /**
     * 手机号码输入界面标识
     */
    private static final int STATUS_INPUT_PHONE_NUMBER = 1;

    /**
     * 验证码输入界面标识
     */
    private static final int STATUS_INPUT_VALIDATE_CODE = 2;

    /**
     * 设置头像界面标识
     */
    private static final int STATUS_SET_HEAD_PHOTO = 3;

    /**
     * 头像选取完成界面标识
     */
    private static final int STATUS_FINISH = 4;

    /**
     * 跳转下一页消息标识
     */
    private static final int MSG_TURN_NEXT_VIEW = 0;

    /**
     * 跳转上一页消息标识
     */
    private static final int MSG_TURN_PRE_VIEW = 1;

    /**
     * 当前界面标识
     */
    private int status;
    
    private MyTitlebar myTitlebar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sun_launcher_activity);
        myTitlebar=(MyTitlebar) findViewById(R.id.launcher_title_bar);
        myTitlebar.setTitle(R.string.app_name);
        myTitlebar.setLeftImageVisibility(View.GONE);
        findViewById(R.id.btn_next).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent=new Intent(SunLauncherActivity.this,WebViewActivity.class);
				MyLog.info("++++sun  info test!");
				MyLog.v("++++sun  v test");
				MyLog.warn("++++sun   warn test");
				startActivity(intent);
				
//				ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//				List<RunningAppProcessInfo> runningAppProcessInfos = activityManager
//						.getRunningAppProcesses();
//				StringBuffer sBuffer=new StringBuffer();
//				for (RunningAppProcessInfo info : runningAppProcessInfos) {
////				Log.i("SunLauncherActivity", "+++++pid:" + info.pid + "  processName:"
////							+ info.processName);
//					sBuffer.append(info.processName+" \n");
//				}
//				Toast.makeText(mContext, sBuffer.toString(), 0).show();
//				Intent intent=new Intent(SunLauncherActivity.this,BackgroudService.class);
//				startService(intent);
				//finish();
			}
		});;
        
    }

    /**
     * 处理界面更新的消息句柄
     */
    Handler statusHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TURN_NEXT_VIEW:
                    status++;
                    initViewByStatus();
                    break;
                case MSG_TURN_PRE_VIEW:
                    status--;
                    initViewByStatus();
                    break;
                default:
                    break;
            }
        }

    };

    /**
     * 可相应点击的Span
     * 
     * @author csm
     */
    class MyURLSpan extends ClickableSpan {
        String url;

        MyURLSpan(String url) {
            this.url = url;
        }

        @Override
        public void onClick(View widget) {
            if ("pre_page".equals(url)) {
                statusHandler.sendEmptyMessage(MSG_TURN_PRE_VIEW);
            }
        }
    }

    /**
     * 根据标识调整界面, 未细致调整，等待UI最后确定。
     */
    void initViewByStatus() {
        switch (status) {
            case STATUS_INIT_ANIMATION:
                
                break;
            case STATUS_INPUT_PHONE_NUMBER:
                
                break;
            case STATUS_INPUT_VALIDATE_CODE:
                
                break;
            case STATUS_SET_HEAD_PHOTO:
                
                break;
            case STATUS_FINISH:
                
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // 按下键盘上返回按钮
        if (status == STATUS_INPUT_VALIDATE_CODE || status == STATUS_FINISH) {
            statusHandler.sendEmptyMessage(MSG_TURN_PRE_VIEW);
            return;
        }
        super.onBackPressed();
    }

}
