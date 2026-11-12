package com.jht.chimera.lib.webapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.util.HashMap;
import java.util.Map;

// javascript snippet take from 
// http://stackoverflow.com/questions/15768837/playing-html5-video-on-fullscreen-in-android-webview/16179544#16179544

public class VideoEnabledWebView extends WebView {
    public class JavascriptInterface {
        @android.webkit.JavascriptInterface
        public void notifyVideoEnd() {
            // This code is not executed in the UI thread, so we must force that
            // to happen
            new Handler(Looper.getMainLooper()).post(() -> {
                if (_videoEnabledWebChromeClient != null) {
                    _videoEnabledWebChromeClient.onHideCustomView();
                }
            });
        }
    }

    private VideoEnabledWebChromeClient _videoEnabledWebChromeClient;
    private boolean _addedJavascriptInterface;
    private String _hotpToken;
    private boolean _useHotpToken = true;

    public VideoEnabledWebView(Context context) {
        super(context);
        _addedJavascriptInterface = false;
    }

    public VideoEnabledWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        _addedJavascriptInterface = false;
    }

    public void setUseHotpToken(boolean value) {
        _useHotpToken = value;
    }

    /**
     * Pass only a VideoEnabledWebChromeClient instance.
     */
    @Override
    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    public void setWebChromeClient(WebChromeClient client) {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        getSettings().setBuiltInZoomControls(false);
        getSettings().setSavePassword(false);
        getSettings().setSaveFormData(false);
        getSettings().setAllowFileAccess(false);
        getSettings().setDomStorageEnabled(true);
        getSettings().setLoadWithOverviewMode(true); // loads the WebView completely zoomed out
        getSettings().setUseWideViewPort(true); // makes the Webview have a normal viewport (such as a normal desktop browser)

        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        if (client instanceof VideoEnabledWebChromeClient) {
            this._videoEnabledWebChromeClient = (VideoEnabledWebChromeClient) client;
        }

        super.setWebChromeClient(client);
    }

    public void setHotpToken(String token) {
        _hotpToken = token;
    }

    @Override
    public void loadData(String data, String mimeType, String encoding) {
        addJavascriptInterface();
        super.loadData(data, mimeType, encoding);
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        addJavascriptInterface();
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    @Override
    public void loadUrl(String url) {
        // add hotp header here
        if (_useHotpToken && url.contains("http") && _hotpToken != null && !_hotpToken.isEmpty()) {
            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put("x-jfit-authorization", _hotpToken);
            loadUrl(url, extraHeaders);
            return;
        }

        if (url.contains("youtube.com")){
             Log.d("VideoEnabledWebView","####CS VideoEnabledWebView loadurl: "+url);
        }
        addJavascriptInterface();
        super.loadUrl(url);
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        addJavascriptInterface();
        super.loadUrl(url, additionalHttpHeaders);
    }

    private void addJavascriptInterface() {
        if (!_addedJavascriptInterface) {
            // Add javascript interface to be called when the video ends (must
            // be done before page load)
            addJavascriptInterface(new JavascriptInterface(), "_VideoEnabledWebView");

            _addedJavascriptInterface = true;
        }
    }

    @Override
    public ActionMode startActionMode(Callback callback) {
        // don't allow text selection
        return null;
    }

    @Override
    public void onPause() {
        // close the fullscreen view
        if (_videoEnabledWebChromeClient != null) {
            _videoEnabledWebChromeClient.hideVideoContainer();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (_videoEnabledWebChromeClient != null) {
            _videoEnabledWebChromeClient.showVideoContainer();
        }
    }

}