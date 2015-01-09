package com.sw.sun.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sw.sun.R;

public class MyTitlebar extends RelativeLayout {

    public static final int BTN_TYPE_TEXT = 0;

    public static final int BTN_TYPE_IMAGE = 1;

    private TextView mTitle,mLeftTitle;

    private TextView mLeftTextBtn, mRightTextBtn;

    private ImageView mLeftImageBtn, mRightImageBtn;

    private ProgressBar mProgressBar;

    public MyTitlebar(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.my_title_bar, this);
        mTitle = (TextView) findViewById(R.id.title);
        mLeftTitle=(TextView) findViewById(R.id.left_title);
        mLeftTextBtn = (TextView) findViewById(R.id.left_text);
        mRightTextBtn = (TextView) findViewById(R.id.right_text);

        mLeftImageBtn = (ImageView) findViewById(R.id.left_image);
        mRightImageBtn = (ImageView) findViewById(R.id.right_image);

        mProgressBar = (ProgressBar) findViewById(R.id.loading);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public View getTitleView() {
        return mTitle;
    }

    public void setTitle(int resid) {
        mTitle.setText(resid);
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public TextView getTitle() {
        return mTitle;
    }

    public void setTitleOnClickListener(OnClickListener listener) {
        mTitle.setOnClickListener(listener);
    }
    
    public void setTitleVisibility(int visibale){
    	mTitle.setVisibility(visibale);
    }
    
    public View getLeftTitleView() {
        return mLeftTitle;
    }

    public void setLeftTitle(int resid) {
        mLeftTitle.setText(resid);
    }

    public void setLeftTitle(String title) {
        mLeftTitle.setText(title);
    }

    public TextView getLeftTitle() {
        return mLeftTitle;
    }

    public void setLeftTitleOnClickListener(OnClickListener listener) {
        mLeftTitle.setOnClickListener(listener);
    }
    
    public void setLeftTitleVisibility(int visibale){
    	mLeftTitle.setVisibility(visibale);
    }

    public void setLeftBtn(int type, int resid, OnClickListener listener) {
        switch (type) {
            case BTN_TYPE_TEXT:
                setLeftTextVisibility(VISIBLE);
                setLeftImageVisibility(GONE);
                setLeftText(resid);
                setLeftTextOnClickListener(listener);
                break;

            case BTN_TYPE_IMAGE:
                setLeftTextVisibility(GONE);
                setLeftImageVisibility(VISIBLE);
                setLeftImage(resid);
                setLeftImageOnClickListener(listener);
                break;
        }
    }

    public void setProgressBarVisibility(int visibility) {
        mProgressBar.setVisibility(visibility);
    }

    public void setRightBtn(int type, int resid, OnClickListener listener) {
        switch (type) {
            case BTN_TYPE_TEXT:
                setRightTextVisibility(VISIBLE);
                setRightImageVisibility(GONE);
                setRightText(resid);
                setRightTextOnClickListener(listener);
                break;

            case BTN_TYPE_IMAGE:
                setRightTextVisibility(GONE);
                setRightImageVisibility(VISIBLE);
                setRightImage(resid);
                setRightImageOnClickListener(listener);
                break;
        }
    }

    public void setLeftTextVisibility(final int visibility) {
        mLeftTextBtn.setVisibility(visibility);
    }

    public void setLeftText(int resid) {
        mLeftTextBtn.setText(resid);
    }

    public void setLeftText(String text) {
        mLeftTextBtn.setText(text);
    }

    public void setLeftTextOnClickListener(OnClickListener listener) {
        mLeftTextBtn.setOnClickListener(listener);
    }

    public void setLeftImageVisibility(final int visibility) {
        mLeftImageBtn.setVisibility(visibility);
    }

    public void setLeftImage(int resid) {
        mLeftImageBtn.setImageResource(resid);
    }

    public void setLeftImageOnClickListener(OnClickListener listener) {
        mLeftImageBtn.setOnClickListener(listener);
    }

    public void setRightTextVisibility(final int visibility) {
        mRightTextBtn.setVisibility(visibility);
    }

    public void setRightText(int resid) {
        mRightTextBtn.setText(resid);
    }

    public void setRightText(String text) {
        mRightTextBtn.setText(text);
    }

    public void setRightTextOnClickListener(OnClickListener listener) {
        mRightTextBtn.setOnClickListener(listener);
    }

    public void setRightTextEnabled(boolean isEnabled) {
        mRightTextBtn.setEnabled(isEnabled);
    }

    public void setRightImageVisibility(final int visibility) {
        mRightImageBtn.setVisibility(visibility);
    }

    public void setRightImageEnable(boolean enable) {
        mRightImageBtn.setEnabled(enable);
    }

    public void setRightImage(int resid) {
        mRightImageBtn.setImageResource(resid);
    }

    public void setRightImageOnClickListener(OnClickListener listener) {
        mRightImageBtn.setOnClickListener(listener);
    }

    public void setTitleBarVisibility(boolean visible) {
        if (visible) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

}
