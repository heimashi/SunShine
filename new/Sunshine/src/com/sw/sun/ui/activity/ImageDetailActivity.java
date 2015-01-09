package com.sw.sun.ui.activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.WindowManager.LayoutParams;

import com.sw.sun.R;
import com.sw.sun.common.android.CommonUtils;
import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.image.cache.ImageCacheManager;
import com.sw.sun.common.image.cache.ImageWorker;
import com.sw.sun.ui.fragment.ImageDetailFragment;

public class ImageDetailActivity extends FragmentActivity{
	
	public static final String EXTRA_IMAGE="extra_image";
	public static final String EXTRS_BITMAP="extra_bitmap";
	
	private ImageWorker mImageWorker;
	private ViewPager viewPager;
	private ImageDetailAdapter imageAdapter;
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.image_detail_pager);
		mImageWorker=new ImageWorker();
		mImageWorker.setImageCache(ImageCacheManager.get(GlobalData.app(), ImageCacheManager.COMMON_IMAGE_CACHE));
		viewPager=(ViewPager) findViewById(R.id.pager);
		imageAdapter=new ImageDetailAdapter(getSupportFragmentManager());
		viewPager.setAdapter(imageAdapter);
		viewPager.setPageMargin((int) getResources().getDimension(R.dimen.horizontal_page_margin));
		viewPager.setOffscreenPageLimit(2);
		
		getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);
		

        // Enable some additional newer visibility and ActionBar features to create a more
        // immersive photo viewing experience
        if (CommonUtils.hasHoneycomb()) {
            final ActionBar actionBar = getActionBar();

            // Hide title text and set home as up
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);

            // Start low profile mode and hide ActionBar
            viewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            actionBar.hide();
        }
		
		final int extraCurrentItem = getIntent().getIntExtra(EXTRA_IMAGE, -1);
        if (extraCurrentItem != -1) {
            viewPager.setCurrentItem(extraCurrentItem);
        }
		
	}

	public ImageWorker getImageWorker(){
		return mImageWorker;
	}
	
	private class ImageDetailAdapter extends FragmentStatePagerAdapter{

		public ImageDetailAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int arg0) {
			return ImageDetailFragment.newInstance(Images.imageUrls[arg0]);
		}

		@Override
		public int getCount() {
			return Images.imageUrls.length;
		}
		
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mImageWorker.resume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mImageWorker.pause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mImageWorker.stop();
	}
	
}
