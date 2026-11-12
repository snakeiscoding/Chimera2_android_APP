package com.jht.chimera.lib.widget.web;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.webapp.VideoEnabledWebChromeClient;
import com.jht.chimera.lib.webapp.VideoEnabledWebView;
import com.jht.chimera.ui.mainapp.PopupWebView;
import com.jht.datamatrixlib.DataMatrix;

public class WebViewContainer extends RelativeLayout {
    private static final String TAG = "WebViewContainer";
    private VideoEnabledWebView _webView;
    private VideoEnabledWebChromeClient _webChromeClient;
    private PopupWebView _popupView;
    private RelativeLayout _popupViewContainer;
    private ImageView _popupClose;
    private String _url = "";
    private String _shimURL = "";

    /**
     * Initialize the option selector item.
     *
     * @param context   Current context for the view.
     */
    public WebViewContainer(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize the option selector item.
     *
     * @param context   Current context for the view.
     * @param attrs     Attributes that are set from the view's XML.
     */
    public WebViewContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize the option selector item.
     *
     * @param context   Current context for the view.
     * @param attrs     Attributes that are set from the view's XML.
     * @param defStyleAttr  Default style for the view.
     */
    @SuppressLint({"InflateParams", "CustomViewStyleable", "SetJavaScriptEnabled"}) //NON-NLS
    public WebViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addView(LayoutInflater.from(context).inflate(R.layout.web_view_container, null, false));
        RelativeLayout nonVideoLayout = findViewById(R.id.non_video_layout);
        RelativeLayout videoLayout = findViewById(R.id.video_layout);
        _popupView = findViewById(R.id.web_view_container_popup);
        _popupViewContainer = findViewById(R.id.web_view_container_popup_container);
        _popupClose = findViewById(R.id.web_view_container_popup_close);

        _popupViewContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hidePopup();
            }
        });

        _popupClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hidePopup();
            }
        });

        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);


        _webView = new VideoEnabledWebView(context);
        _webView.setVisibility(View.GONE);
        nonVideoLayout.addView(_webView, layoutParams);

        _webChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout);


        _webView.getSettings().setBuiltInZoomControls(false);
        _webView.getSettings().setJavaScriptEnabled(true);

        CookieManager.getInstance().setAcceptThirdPartyCookies(_webView, true);


        _webView.setWebChromeClient(_webChromeClient);
        _webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {

            }


            @Override
            public void onPageFinished(final WebView view, String url)
            {
                if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                    Log.d(TAG, "on page finished, url: " + url);
                }
                super.onPageFinished(view, url);
            }
        });
    }

    public void show()
    {
        //_timestamp = System.currentTimeMillis();
       // _webChromeClient.registerReceiver();
        _webView.setVisibility(View.VISIBLE);
        _webView.onResume();
    }

    public void hide()
    {
        _webView.setVisibility(View.GONE);
        _webView.onPause();
        //_webChromeClient.unregisterReceiver();
    }

    public void loadUrl(String url)
    {
        _url = url;
        _webView.loadUrl(_url);
    }

    public void setHotpToken(String token)
    {
        _webView.setHotpToken(token);
    }

    public void setExpandContractListeners(View.OnClickListener expand, View.OnClickListener shrink) {
        //_webChromeClient.setFullscreenListeners(expand, shrink);
    }

    @SuppressLint("JavascriptInterface")
    public void setJavascript(Object javascript) {
        _webView.addJavascriptInterface(javascript, "external");
    }


    @SuppressLint("JavascriptInterface")
    public void setJavascript(Object javascript, String name) {
        _webView.addJavascriptInterface(javascript, name);
    }

    public void showPopupView(String url) {
        _popupView.loadUrl(url);
        _popupViewContainer.setVisibility(View.VISIBLE);
    }

    public void setShimURL(String shimURL) {
        _shimURL = shimURL;
    }


    public void hidePopup() {
        _popupViewContainer.setVisibility(View.GONE);
    }
}
