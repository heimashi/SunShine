package com.sw.sun.service;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.sw.sun.common.logger.MyLog;

public class BackgroudService extends IntentService {

	private static final String TAG = "BackgroudService";
	
	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			List<RunningAppProcessInfo> runningAppProcessInfos = activityManager
					.getRunningAppProcesses();
			for (RunningAppProcessInfo info : runningAppProcessInfos) {
				Log.i(TAG, "getRunningAppProcesses+++++++++++++pid:" + info.pid + "  processName:"
						+ info.processName);
			}
			// activityManager.getAppTasks()

			List<RunningTaskInfo> runningTaskInfos = activityManager
					.getRunningTasks(20);
			for (RunningTaskInfo runningTaskInfo : runningTaskInfos) {
				Log.i(TAG,"getRunningTasks+++++++++++++++topActivity:"
						+ runningTaskInfo.topActivity + "  baseActivity:"
						+ runningTaskInfo.baseActivity + "  description:"
						+ runningTaskInfo.description);
			}

			List<RecentTaskInfo> recentTaskInfos=activityManager.getRecentTasks(20, ActivityManager.RECENT_WITH_EXCLUDED);
			for (RecentTaskInfo recentTaskInfo : recentTaskInfos) {
				Log.i(TAG,"getRecentTasks  RECENT_WITH_EXCLUDED++++++++++++++++baseIntent:"
						+ recentTaskInfo.baseIntent + "  origActivity:"
						+ recentTaskInfo.origActivity + "  description:"
						+ recentTaskInfo.description);
			}
			
			List<RecentTaskInfo> recentTaskInfos2=activityManager.getRecentTasks(20, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
			for (RecentTaskInfo recentTaskInfo : recentTaskInfos2) {
				Log.i(TAG,"getRecentTasks  RECENT_IGNORE_UNAVAILABLE+++++++++++++++baseIntent:"
						+ recentTaskInfo.baseIntent + "  origActivity:"
						+ recentTaskInfo.origActivity + "  description:"
						+ recentTaskInfo.description);
			}
			
			super.handleMessage(msg);
		}
	};
	
	public BackgroudService(){
		super(TAG);
		MyLog.info("BackgroudService++++start");
	}
	
	public BackgroudService(String name) {
		super(name);
		MyLog.info("BackgroudService++++start");
	}

	private boolean flag=true;
	final private Timer timer=new Timer();
	private TimerTask timerTask;
	
	
	@Override
	protected void onHandleIntent(Intent intent) {

		
		timerTask=new TimerTask() {
			
			@Override
			public void run() {
				handler.sendEmptyMessage(0);
			}
		};
		
		//start 
		timer.schedule(timerTask, 30000, 30000);
		
		//stop
		//timer.cancel();
		
//		Runnable runnable = new Runnable() {
//
//			@Override
//			public void run() {
//				handler.sendEmptyMessage(0);
//				handler.postDelayed(this, 30000);
//			}
//		};
//		
//		handler.postDelayed(runnable, 30000);
		
		//handler.removeCallbacks(runnable);   
	}

}
