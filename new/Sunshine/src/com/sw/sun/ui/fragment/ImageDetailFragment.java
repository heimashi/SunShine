package com.sw.sun.ui.fragment;

import com.sw.sun.R;
import com.sw.sun.common.android.CommonUtils;
import com.sw.sun.common.image.cache.HttpImage;
import com.sw.sun.common.image.cache.ImageWorker;
import com.sw.sun.ui.activity.ImageDetailActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImageDetailFragment extends Fragment{
	
	private static final String IMAGE_DATA_EXTRA = "extra_image_data";
	private String imageUrl;
	private ImageView imageView;
	private ImageWorker imageWorker;
	Bitmap bitmapLoading ;
	
	public static ImageDetailFragment newInstance(String imageUrl){
		final ImageDetailFragment fragment=new ImageDetailFragment();
		final Bundle bundle=new Bundle();
		bundle.putString(IMAGE_DATA_EXTRA, imageUrl);
		fragment.setArguments(bundle);
		return fragment;
	}
	
	public ImageDetailFragment(){};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		imageUrl=getArguments()!=null?getArguments().getString(IMAGE_DATA_EXTRA):null;
		bitmapLoading= BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_launcher);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view=inflater.inflate(R.layout.image_detail_fragment, container,false);
		imageView=(ImageView) view.findViewById(R.id.image_detail_iv);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(ImageDetailActivity.class.isInstance(getActivity())){
			imageWorker=((ImageDetailActivity)getActivity()).getImageWorker();
			if(!TextUtils.isEmpty(imageUrl)){
				HttpImage httpImage=new HttpImage(imageUrl);
				//httpImage.loadingBitmap=bitmapLoading;
				imageWorker.loadImage(httpImage, imageView);
			}
			
		}
		if(OnClickListener.class.isInstance(getActivity())&&CommonUtils.hasHoneycomb()){
			imageView.setOnClickListener((OnClickListener)getActivity());
		}
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(imageView!=null){
			ImageWorker.cancelWork(imageView);
			imageView.setImageDrawable(null);
		}
	}
}
