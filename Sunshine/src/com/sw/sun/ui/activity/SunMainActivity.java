package com.sw.sun.ui.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;

import com.sw.sun.R;
import com.sw.sun.common.file.SDCardUtils;
import com.sw.sun.common.image.cache.HttpImage;
import com.sw.sun.common.image.cache.ImageCacheManager;
import com.sw.sun.common.image.cache.ImageWorker;
import com.sw.sun.common.image.filter.AvatarFilter;
import com.sw.sun.common.image.filter.BitmapFilter;

public class SunMainActivity extends BaseActivity {

	ImageView mImageView;
	ImageWorker imageWorker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sun_main_activity);
		mImageView = (ImageView) findViewById(R.id.image_show);
		imageWorker = new ImageWorker();
		imageWorker.setImageCache(ImageCacheManager.get(mContext,
				ImageCacheManager.COMMON_IMAGE_CACHE));
		
		
		
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		imageWorker.resume();
		Bitmap bitmapLoading = BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_launcher);
		//http://lxcdn.dl.files.xiaomi.net/mfsv2/download/s010/p01CwrLjlTFp/6GyM5HYYg5uY9S.png
		//http://lxcdn.dl.files.xiaomi.net/mfsv2/download/s010/p01YR6wnatLJ/H8R273OBgByfnw.png
		String photoUrl ="http://lxcdn.dl.files.xiaomi.net/mfsv2/download/s010/p01CwrLjlTFp/6GyM5HYYg5uY9S.png";
		if(!TextUtils.isEmpty(photoUrl)&&!SDCardUtils.isSDCardBusy()){
			HttpImage httpImage = new HttpImage(photoUrl);
			httpImage.loadingBitmap = bitmapLoading;
			httpImage.filter=new AvatarFilter();
			imageWorker.loadImage(httpImage, mImageView);
		}else {
			mImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
				R.drawable.login_botton_1));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		imageWorker.pause();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		imageWorker.stop();
	}
	
	
	
	

}
