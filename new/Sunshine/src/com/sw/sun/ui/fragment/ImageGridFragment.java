package com.sw.sun.ui.fragment;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.sw.sun.R;
import com.sw.sun.common.android.CommonUtils;
import com.sw.sun.common.android.GlobalData;
import com.sw.sun.common.image.cache.HttpImage;
import com.sw.sun.common.image.cache.ImageCacheManager;
import com.sw.sun.common.image.cache.ImageWorker;
import com.sw.sun.ui.activity.ImageDetailActivity;
import com.sw.sun.ui.activity.Images;

public class ImageGridFragment extends Fragment implements OnItemClickListener {

	private int mImageThumbSize;
	private int mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ImageWorker mImageWorker;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mImageThumbSize = getResources().getDimensionPixelSize(
				R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(
				R.dimen.image_thumbnail_spacing);
		mAdapter = new ImageAdapter();
		mImageWorker = new ImageWorker();
		mImageWorker.setImageCache(ImageCacheManager.get(GlobalData.app(),
				ImageCacheManager.COMMON_IMAGE_CACHE));

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.image_grid_fragment,
				container, false);
		final GridView mGridView = (GridView) view.findViewById(R.id.gridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (!CommonUtils.hasHoneycomb()) {
					if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
						mImageWorker.pause();
					} else {
						mImageWorker.resume();
					}
				}
				
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

			}
		});

		// This listener is used to get the final width of the GridView and then
		// calculate the
		// number of columns and the width of each column. The width of each
		// column is variable
		// as the GridView has stretchMode=columnWidth. The column width is used
		// to set the height
		// of each view so we get nice square thumbnails.
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@TargetApi(VERSION_CODES.JELLY_BEAN)
					@Override
					public void onGlobalLayout() {
						if (mAdapter.getNumColumns() == 0) {
							final int numColumns = (int) Math.floor(mGridView
									.getWidth()
									/ (mImageThumbSize + mImageThumbSpacing));
							if (numColumns > 0) {
								final int columnWidth = (mGridView.getWidth() / numColumns)
										- mImageThumbSpacing;
								mAdapter.setNumColumns(numColumns);
								mAdapter.setItemHeight(columnWidth);
								if (CommonUtils.hasJellyBean()) {
									mGridView.getViewTreeObserver()
											.removeOnGlobalLayoutListener(this);
								} else {
									mGridView.getViewTreeObserver()
											.removeGlobalOnLayoutListener(this);
								}
							}
						}
					}
				});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		mImageWorker.resume();
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();
		mImageWorker.pause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageWorker.stop();
	}

	private class ViewHolder {
		public TextView textView;
		public ImageView imageView;

	}

	
	
	private class ImageAdapter extends BaseAdapter {

		//private final Context mContext;
		private int mItemHeight = 0;
		private int mNumColumns = 0;

		// private int mActionBarHeight = 0;
		// private LayoutParams mImageViewLayoutParams;

		public ImageAdapter() {
			super();
			//mContext = context;
			// mImageViewLayoutParams = new LayoutParams(
			// LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		@Override
		public int getCount() {
			// If columns have yet to be determined, return no items
			if (getNumColumns() == 0) {
				return 0;
			}

			// Size + number of columns for top empty row
			return Images.imageThumbUrls.length;
		}

		@Override
		public Object getItem(int position) {
			return Images.imageThumbUrls[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			// Now handle the main ImageView thumbnails
			ViewHolder viewHolder;
			if (convertView == null) { // if it's not recycled, instantiate and
										// initialize
				convertView = LayoutInflater.from(GlobalData.app()).inflate(
						R.layout.grid_image_item, null);
				viewHolder = new ViewHolder();
				viewHolder.imageView = (ImageView) convertView
						.findViewById(R.id.grid_iv);
				viewHolder.textView = (TextView) convertView
						.findViewById(R.id.grid_tv);
				// viewHolder.imageView.setLayoutParams(mImageViewLayoutParams);
				convertView.setTag(viewHolder);
			} else { // Otherwise re-use the converted view
				viewHolder = (ViewHolder) convertView.getTag();
			}

			viewHolder.textView.setText("position:" + position);

			// Check the height matches our calculated column width
			if (viewHolder.imageView.getLayoutParams().height != mItemHeight) {
				viewHolder.imageView.setLayoutParams(new LayoutParams(
						LayoutParams.MATCH_PARENT, mItemHeight));
			}

			// Finally load the image asynchronously into the ImageView, this
			// also takes care of
			// setting a placeholder image while the background thread runs
			String photoUrl = Images.imageThumbUrls[position];
			Bitmap bitmapLoading = BitmapFactory.decodeResource(getResources(),
					R.drawable.ic_launcher);
			if (!TextUtils.isEmpty(photoUrl)) {
				HttpImage httpImage = new HttpImage(photoUrl);
				httpImage.loadingBitmap = bitmapLoading;
				mImageWorker.loadImage(httpImage, viewHolder.imageView);
			}

			return convertView;

		}

		/**
		 * Sets the item height. Useful for when we know the column width so the
		 * height can be set to match.
		 * 
		 * @param height
		 */
		public void setItemHeight(int height) {
			if (height == mItemHeight) {
				return;
			}
			mItemHeight = height;
			// mImageViewLayoutParams =
			// new LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
			// mImageWorker.setImageSize(height);
			notifyDataSetChanged();
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}

	}

	@TargetApi(VERSION_CODES.JELLY_BEAN)
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		final Intent i = new Intent(getActivity(), ImageDetailActivity.class);
		i.putExtra(ImageDetailActivity.EXTRA_IMAGE, (int) id);
		if (CommonUtils.hasJellyBean()) {
			// makeThumbnailScaleUpAnimation() looks kind of ugly here as the
			// loading spinner may
			// show plus the thumbnail image in GridView is cropped. so using
			// makeScaleUpAnimation() instead.
			ActivityOptions options = ActivityOptions.makeScaleUpAnimation(v,
					0, 0, v.getWidth(), v.getHeight());
			getActivity().startActivity(i, options.toBundle());
		} else {
			startActivity(i);
		}

	}

}
