package com.sw.sun.ui.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sw.sun.R;
import com.sw.sun.common.file.SDCardUtils;
import com.sw.sun.common.image.cache.HttpImage;
import com.sw.sun.common.image.cache.ImageCacheManager;
import com.sw.sun.common.image.cache.ImageWorker;
import com.sw.sun.common.image.filter.AvatarFilter;
import com.sw.sun.common.image.filter.BitmapFilter;
import com.sw.sun.ui.view.MyTitlebar;
import com.sw.sun.ui.view.PullToRefreshListView;
import com.sw.sun.ui.view.PullToRefreshListView.OnRefreshListener;

public class SunMainActivity extends BaseActivity implements OnScrollListener{

	PullToRefreshListView mlistView;
	ImageWorker imageWorker;
	MyAdapter myAdapter;
	private MyTitlebar myTitlebar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sun_main_activity);
		myTitlebar=(MyTitlebar) findViewById(R.id.main_title_bar);
		myTitlebar.setLeftText(R.string.next_step);
		myTitlebar.setLeftImageOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		imageWorker = new ImageWorker();
		imageWorker.setImageCache(ImageCacheManager.get(mContext,
				ImageCacheManager.COMMON_IMAGE_CACHE));
		
		mlistView = (PullToRefreshListView) findViewById(R.id.image_show);
		mlistView.setOnRefreshListener(new OnRefreshListener() {
			
			@Override
			public void onRefresh() {
				
				mlistView.postDelayed(new Runnable() {
					@Override
					public void run() {
						mlistView.onRefreshComplete();
					}
				}, 2000);
				
			}
		});
		myAdapter=new MyAdapter();
		mlistView.setAdapter(myAdapter);
		
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		imageWorker.resume();

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

	private class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return Images.imageUrls.length;
		}

		@Override
		public Object getItem(int position) {
			return Images.imageUrls[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.list_layout_item, null);
				viewHolder=new ViewHolder();
				viewHolder.textView = (TextView) convertView
						.findViewById(R.id.tv_position);
				viewHolder.imageView = (ImageView) convertView
						.findViewById(R.id.iv_position);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			resetViewHolder(viewHolder);
			
			viewHolder.textView.setText("position" + position);
			String photoUrl = Images.imageUrls[position];
			Bitmap bitmapLoading = BitmapFactory.decodeResource(getResources(),
					R.drawable.ic_launcher);
			viewHolder.imageView.setVisibility(View.VISIBLE);
			if (!TextUtils.isEmpty(photoUrl) && !SDCardUtils.isSDCardBusy()) {
				HttpImage httpImage = new HttpImage(photoUrl);
				httpImage.loadingBitmap = bitmapLoading;
				httpImage.filter = new AvatarFilter();
				imageWorker.loadImage(httpImage, viewHolder.imageView);
			} else {
				viewHolder.imageView.setImageBitmap(BitmapFactory
						.decodeResource(getResources(),
								R.drawable.login_botton_1));
			}

			return convertView;
		}

		private void resetViewHolder(ViewHolder viewHolder2) {
			viewHolder2.textView.setText("");
			viewHolder2.imageView.setVisibility(View.GONE);
			
		}

	}

	private class ViewHolder {
		TextView textView;
		ImageView imageView;

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            imageWorker.pause();
        } else {
        	imageWorker.resume();
        }
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		
	}

}
