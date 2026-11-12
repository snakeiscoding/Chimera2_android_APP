package com.jht.chimera.service.tiers.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys;
import com.jht.chimera.lib.widget.web.WebViewContainer;
import com.jht.chimera.service.tiers.PremiumFeature;
import com.jht.chimera.service.tiers.PremiumFeatureSet;
import com.jht.chimera.ui.machinesettings.JHTTypes;
import com.jht.datamatrixlib.DataMatrix;


public class WebViewDialogBox extends DialogBase {
    private static final String TAG = "WebViewDialogBox";
    public class UpgradeJavascript {
        @JavascriptInterface
        public void upgradeComplete(boolean success, String logMessage, String upgradeId)
        {
            Log.e(TAG,"upgrade complete. Success: " + success + ", upgradeID: " + upgradeId + "message: " + logMessage);
            if(success) {
                PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                if(set != null) {
                    for(PremiumFeature feature: set.getFeatures()) {
                        if(feature.containsUpgrade(upgradeId)) {
                            feature.setFeatureState(JHTTypes.FEATURE_STATE.FEATURE_ENABLED);
                        }
                    }
                    DataMatrix.putParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, set);
                }
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean("result", success);
            bundle.putString("message", logMessage);

            DataMatrix.putParcelable(TiersKeys.ACTION_UPGRADE_RESULT_PARCELABLE, bundle);
//
//            hide();


        }

        @JavascriptInterface
        public void closeUpgradeDialog()
        {
            Log.e(TAG, "we are closing web dialog");
            hide();
        }

        @JavascriptInterface
        public void keepAlive()
        {
            Log.e(TAG, "keep alive");
        }
    }

    private WebViewContainer webView;

    public WebViewDialogBox(Context context) {
        this(context, null, 0);
    }

    public WebViewDialogBox(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WebViewDialogBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        webView = findViewById(R.id.webview_box);
        webView.setJavascript(new UpgradeJavascript());
        findViewById(R.id.close_button).setOnClickListener(v -> {
           hide();
        });
    }

    public void init(String url) {
        webView.loadUrl(url);
        webView.show();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.webview_dialog_box;
    }
}
