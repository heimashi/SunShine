package com.sw.sun.ui.activity;

import com.sw.sun.ui.fragment.ImageGridFragment;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class ImageGridActivity extends FragmentActivity{
	private static final String TAG = "ImageGridActivity";
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		if(getSupportFragmentManager().findFragmentByTag(TAG)==null){
			final FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
			fragmentTransaction.add(android.R.id.content, new ImageGridFragment(), TAG);
			fragmentTransaction.commit();
		}
	}
	
}
