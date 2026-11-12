package com.jht.chimera.ui.mainapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.lib.webapp.VideoEnabledWebView;
import com.jht.chimera.lib.webapp.WebAppHelper;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;


public class PopupWebView extends VideoEnabledWebView {
    private String _shimText = "";
    private Activity _activity = null;
    private String _originalUrl = "";
    private boolean allowExternalLinks = true;

    public PopupWebView(Context context) {
        super(context);
        init();
    }

    public PopupWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setActivity(Activity activity) {
        _activity = activity;
    }

    public void setShimText(String shimText) {
        _shimText = shimText;
    }

    private SimpleMessageDialog messageDialog = null;
    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("deprecation")
    private void init() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBuiltInZoomControls(true);
        getSettings().setSavePassword(false);
        getSettings().setSaveFormData(false);
        getSettings().setAllowFileAccess(false);


        setWebViewClient(new WebViewClient() {
                         @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Note you can get to Netflix from Fast.com.  This is to try and prevent that.
                if (allowExternalLinks || url.startsWith(_originalUrl)) {
                    loadUrl(url);
                }
                return true;
            }


            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                view.loadUrl("about:blank");
                if (_activity != null && messageDialog == null) {

                    messageDialog = ProjectDialogFactory.message(view, description, () -> messageDialog = null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.contains("http"))
                    WebAppHelper.injectScriptFile(view, _shimText);
            }
        });

        reset();
    }

    public void reset() {
        loadUrl("about:blank");
        _originalUrl = "";
    }

    public void allowExternalLinks(boolean allowExternalLinks) {
        this.allowExternalLinks = allowExternalLinks;
    }
    public void loadWebsite(String url) {
        _originalUrl = url;
        super.loadUrl(url);
    }

    @Override
    public ActionMode startActionMode(Callback callback) {
        // don't allow text selection
        return null;
    }
}