package com.jht.chimera.ui.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageButton;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.webapp.VideoEnabledWebChromeClient;
import com.jht.chimera.lib.webapp.VideoEnabledWebView;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.lib.widget.selectron.SelectronButton;
import com.jht.datamatrixlib.DataMatrix;


public class WebBrowserActivity extends Activity {
    private static final String TAG = "WebBrowserActivity";

    private VideoEnabledWebView mWebView;
    private EditText mAddressBar;
    private AppCompatImageButton mBack;
    private AppCompatImageButton mFwd;
    private SelectronButton mReload;
    private ProgressBar mLoading;

    private boolean mStop;


    private WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            mLoading.setVisibility(View.VISIBLE);
            mFwd.setEnabled(false);
            DataMatrix.putString(MainAppKeys.DATA_WEB_BROWSER_CURRENT_PAGE_STRING, url);
            mAddressBar.setText(url);

            // Display & enable stop button, hide & disable reload button
            mStop = true;
            mReload.setIcon(R.drawable.ic_close_white);
        }

        @Override
        public void onPageFinished(WebView view, String url) {

            if ("about:blank".equals(url) && view.getTag() != null) { //NON-NLS
                url = view.getTag().toString();
                view.loadUrl(view.getTag().toString());
                mAddressBar.setText(view.getTag().toString());
                DataMatrix.putString(MainAppKeys.DATA_WEB_BROWSER_CURRENT_PAGE_STRING, view.getTag().toString());
            } else
                view.setTag(url);
            super.onPageFinished(view, url);

            mLoading.setVisibility(View.GONE);

            mBack.setEnabled(mWebView.canGoBack());
            mFwd.setEnabled(mWebView.canGoForward());

            // Display & enable reload button, hide & disable stop button
            mStop = false;
            mReload.setIcon(R.drawable.ic_refresh_white);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                Log.e(TAG, "URL failed " + failingUrl + " with error code " + errorCode + " on message " + description);
            }
            super.onReceivedError(view, errorCode, description, failingUrl);

        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return !url.startsWith("http://") && !url.startsWith("https://");
        }
    };

    @Override
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CommonRoutines.initWebActivity(this, R.layout.activity_web_browser, TAG);

        mBack = findViewById(R.id.back);
        mBack.setEnabled(false);
        mBack.setOnClickListener(mOnClickListener);

        mFwd = findViewById(R.id.forward);
        mFwd.setEnabled(false);
        mFwd.setOnClickListener(mOnClickListener);

        mReload = findViewById(R.id.reload);
        mReload.setOnClickListener(mOnClickListener);

        mLoading = findViewById(R.id.loading);

        mWebView = findViewById(R.id.webView);
        mAddressBar = findViewById(R.id.addressBar);
        mAddressBar.setOnTouchListener((view, motionEvent) -> {
            mAddressBar.setCursorVisible(true);
            return false;
        });
        mAddressBar.setOnEditorActionListener((arg0, arg1, arg2) -> {
            if (arg2 != null) {
                if (arg2.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    String url = mAddressBar.getText().toString();
                    if (!url.startsWith("http://") && !url.startsWith("https://")) //NON-NLS
                        url = "http://" + url; //NON-NLS
                    mWebView.loadUrl(url);
                    DataMatrix.putString(MainAppKeys.DATA_WEB_BROWSER_CURRENT_PAGE_STRING, url);
                    mAddressBar.setText(url);

                    InputMethodManager imm =
                            (InputMethodManager) getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mAddressBar.getWindowToken(), 0);
                    mAddressBar.setCursorVisible(false);
                    return true;
                }
            }
            return false;
        });

        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setUseHotpToken(false);

        RelativeLayout nonVideoLayout = findViewById(R.id.nonVideoLayout);
        ViewGroup videoLayout = findViewById(R.id.videoLayout);
        VideoEnabledWebChromeClient mWebChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout);
        mWebView.setWebChromeClient(mWebChromeClient);
        String homePage = DataMatrix.getString(MainAppKeys.DATA_WEB_BROWSER_CURRENT_PAGE_STRING);
        if (null != homePage) {
            if (!homePage.startsWith("http://") && !homePage.startsWith("https://")) //NON-NLS
                homePage = "http://" + homePage; //NON-NLS
            mAddressBar.setText(homePage);
            mWebView.loadUrl(homePage);
            mWebView.requestFocus();
        } else {
            mAddressBar.setText(StringLookup.get(R.string.default_web_page)); //NON-NLS
            mWebView.loadUrl(StringLookup.get(R.string.default_web_page));
            mWebView.requestFocus();
        }
        mAddressBar.setText(homePage);
        mWebView.loadUrl(homePage);
        mWebView.requestFocus();
    }


    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        mWebView.clearCache(true);
        mWebView.clearHistory();
    }


    View.OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            int id = v.getId();
            if (id == R.id.back) {
                mWebView.goBack();
            } else if (id == R.id.forward) {
                mWebView.goForward();
            } else if (id == R.id.reload) {
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();

                if (!mStop)
                    mWebView.reload();
                else
                    mWebView.stopLoading();
            }
        }
    };
}
