package com.sw.sun.ui.activity;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.sw.sun.R;
import com.sw.sun.common.android.CommonUtils;
import com.sw.sun.common.android.GlobalData;
import com.sw.sun.ui.view.MyTitlebar;

public class WebViewActivity extends BaseActivity {
	
	private WebView webView;
	private MyTitlebar myTitlebar;
	private WebViewClient webViewClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview_activity);
		
		myTitlebar=(MyTitlebar) findViewById(R.id.webview_titlebar);
		myTitlebar.setRightImageVisibility(View.VISIBLE);
		myTitlebar.setRightImage(R.drawable.all_icon_more_btn);
		myTitlebar.setRightImageOnClickListener(mTitleRightClickListener);
		myTitlebar.setLeftImageOnClickListener(mleftClickListener);
		myTitlebar.setLeftTitleVisibility(View.VISIBLE);
		
		webView=(WebView) findViewById(R.id.webview_activity);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setSupportZoom(true);
		webView.getSettings().setBuiltInZoomControls(false);
		webView.getSettings().setUseWideViewPort(true);
		webView.getSettings().setLoadWithOverviewMode(true);
		webView.clearCache(false);
		webViewClient=new MyWebViewClient();
		webView.setWebViewClient(webViewClient);
		webView.setWebChromeClient(new WebChromeClient());
		
		webView.loadUrl("http://www.baidu.com");
		
	}
	
	private class MyWebViewClient extends WebViewClient{
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			if(view!=null){
				myTitlebar.setLeftTitle(view.getTitle());
			}
			
		}
	}
	
	@Override
	public void onBackPressed() {
		if(webView.canGoBack()){
			webView.goBack();
		}
	}
	
	private OnClickListener mleftClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };
	
	private OnClickListener mTitleRightClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            onClickTitleRight();
        }
    };
    
    
    static final int COPY_LINK = 0;
    static final int VIEW_IN_SYSTEM_BROWSER = 1;
    static final int BAIDU = 2;
    static final int SINA = 3;
    static final int PLAY_GAMES = 4;
    
    
    private AlertDialog mMoreDialog;
    
    public void onClickTitleRight() {
        if (null == mMoreDialog) {
            String[] arrays=mContext.getResources().getStringArray(R.array.ml_webview_more);;
            if (null != arrays) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setItems(arrays, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case COPY_LINK: {
                                ClipboardManager cmb = (ClipboardManager) GlobalData.app().getSystemService(
                                        Context.CLIPBOARD_SERVICE);
                                cmb.setPrimaryClip(ClipData.newPlainText("data", webView.getUrl()));
                                Toast.makeText(mContext, R.string.copied_to_clipboard, 0).show();
                                break;
                            }
                            case VIEW_IN_SYSTEM_BROWSER: {
                                String url=webView.getUrl();
                                if (!TextUtils.isEmpty(url)) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    if (CommonUtils.isIntentAvailable(mContext, intent)) {
                                    	mContext.startActivity(intent);
                                    }
                                }
                                break;
                            }
                            case BAIDU:
                            	webView.loadUrl("http://www.baidu.com");
                            	break;
                            case SINA:
                            	webView.loadUrl("http://sina.cn");
                            	break;
                            case PLAY_GAMES:
                            	webView.loadUrl("http://staging.support.miliao.xiaomi.com/fe/hxg/ee113/launcher/index.html");
                            	break;
                        }
                    }
                });
                mMoreDialog = builder.create();
            }
        }
        if (null != mMoreDialog && !mMoreDialog.isShowing()) {
            mMoreDialog.show();
        }
    }
    
	
	

}
