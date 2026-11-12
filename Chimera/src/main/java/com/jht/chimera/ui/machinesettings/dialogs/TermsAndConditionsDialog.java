package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;

public class TermsAndConditionsDialog extends DialogBase {
    private static final String TAG = "TermsAndConditionsDi...";
    private TermsAndConditionsDialog.IOnYesNo onYesNo;
    private WebView message;

    public interface IOnYesNo {
        void onYesNo(boolean var1);
    }
    public TermsAndConditionsDialog(Context context) {
        this(context, null, 0);
    }

    public TermsAndConditionsDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TermsAndConditionsDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        message = findViewById(R.id.message_text_content);
        message.getSettings().setAllowFileAccess(true);

        //we want to send the touch event to the view behind this root view (the top bar) if
        //the touch happened within the coordinates of the top bar.
        (findViewById(R.id.mainContainer)).setOnTouchListener((v, event) -> {
            if(isViewContains(findViewById(R.id.space), (int)event.getRawX(), (int)event.getRawY())) {
                hide();
                if(onYesNo != null) {
                    onYesNo.onYesNo(false);
                }
                return false;
            } else if(isViewContains(findViewById(R.id.space0), (int)event.getRawX(), (int)event.getRawY())) {
                return false;
            } else {
                return true;
            }

        });

        findViewById(R.id.backButton).setOnClickListener(v -> {
            hide();
            if(onYesNo != null) {
                onYesNo.onYesNo(false);
            }
        });
        findViewById(R.id.acceptButton).setOnClickListener(v -> {
            hide();
            if(onYesNo != null) {
                onYesNo.onYesNo(true);
            }
        });

        message.setWebViewClient(mWebViewClient);
    }

    private boolean isViewContains(View view, int rx, int ry) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int w = view.getWidth();
        int h = view.getHeight();

        if (rx < x || rx > x + w || ry < y || ry > y + h) {
            return false;
        }
        return true;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.terms_conditions_dialog;
    }

    public void init(String message, IOnYesNo onYesNo) {
        this.onYesNo = onYesNo;

        this.message.loadUrl(message);
        //this.message.loadData(message, "text/html", "base64");
        /*
        this.message.setText(Html.fromHtml(message, 0));
         */
    }

    private WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {

        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "should override url " + url);
            message.loadUrl(url);
            return true;
        }
    };

}
