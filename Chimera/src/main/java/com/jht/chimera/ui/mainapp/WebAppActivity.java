package com.jht.chimera.ui.mainapp;

import static com.jht.chimera.lib.workoutapps.AppManager.WORKOUT_HISTORY_URL_BASE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import com.jht.chimera.lib.widget.activity.RestringActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.webapp.JumpTokenHost;
import com.jht.chimera.lib.webapp.VideoEnabledWebChromeClient;
import com.jht.chimera.lib.webapp.VideoEnabledWebView;
import com.jht.chimera.lib.webapp.WebAppHelper;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.oauthlib.auth.BksCredentialProvider;

import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.Map;

/**
 * https://jhtnaee.atlassian.net/wiki/spaces/CA/pages/9011202/Console+Application+Environments
 */

public class WebAppActivity extends RestringActivity {
    public static final String TAG = "WebAppActivity";

    private Map<String, WebContainerView> mWebViewMap;
    protected WebContainerView mCurrentWebView;
    private PopupWebView mPopupWebView;
    private RelativeLayout mPopupViewLayout;
    private RelativeLayout mNonVideoLayout;
    private ViewGroup mVideoLayout;
    @NonNls private String mShimUrl = "https://api.jfit.co/console-apps/shims/universal.js";
    @NonNls private String mShimPopupUrl = "https://api.jfit.co/console-apps/shims/popup.js";
    private String mHotpToken;
    private Handler mHandler = new Handler();
    private String mShimText = "";
    private String mShimPopupText = "";
    private DataMatrix mDataMatrix = new DataMatrix();
    private SimpleMessageDialog alertDialog = null;
    private String[] mDomainBanList = null;
    private String[] mDomainIncludeList = null;
    private Boolean mBackButtonFinishesActivity = false;

    private JHTTimer deleteCacheTimer = null;
    private static final int DELETE_CACHE_TIME = 300000;

    // Sets whether
    private boolean clearCachePeriodically = false;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    protected String getWebViewDataDir() {
        return TAG;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        CommonRoutines.initWebActivity(this, R.layout.web_app_activity, getWebViewDataDir());
        mNonVideoLayout = findViewById(R.id.nonVideoLayout);
        mVideoLayout = findViewById(R.id.videoLayout);
        mPopupWebView = findViewById(R.id.popupView);
        mPopupWebView.setActivity(this);

        requestShim();

        VideoEnabledWebChromeClient webChromeClient = new VideoEnabledWebChromeClient(mNonVideoLayout, mVideoLayout);
        mPopupWebView.setWebChromeClient(webChromeClient);
        mPopupViewLayout = findViewById(R.id.popupViewLayout);
        mWebViewMap = new HashMap<>();

        setIntent(getIntent());
    }

    protected void requestShim() {
        // cache the shim on startup in a different thread
        new Thread(() -> {
            mShimText = WebAppHelper.getShim(mShimUrl);
            mShimPopupText = WebAppHelper.getShim(mShimPopupUrl);
            mPopupWebView.setShimText(mShimPopupText);
        }).start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (mCurrentWebView.mWebView.canGoBack()) {
            mCurrentWebView.mWebView.goBack();
        }
        else if (mBackButtonFinishesActivity) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        processIntent(getIntent());
        mDataMatrix.createEventAdapter(UiServiceKeys.EVENT_APP_BAR_REFRESH_BTN_CLICK, keyReturnedBack -> reload());
        mDataMatrix.createBoolAdapter(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, status -> requestShim());

        if (clearCachePeriodically) {
            Log.d(TAG, "Starting cache clear timer in web app activity.");
            startClearCacheTimer();
        }
    }

    void startClearCacheTimer() {
        if (deleteCacheTimer != null) {
            deleteCacheTimer.stop();
        }

        // Every DELETE_CACHE_TIME remove the cache
        deleteCacheTimer = new JHTTimer(() -> {
            Log.d(TAG, "Deleting cache from webview");
            mHandler.post(() -> mCurrentWebView.mWebView.clearCache(true));
        });
        deleteCacheTimer.start(DELETE_CACHE_TIME, DELETE_CACHE_TIME);
    }

    void stopClearCacheTimer() {
        if (deleteCacheTimer != null) {
            deleteCacheTimer.stop();
            deleteCacheTimer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            hideAllWebView();
        } catch (Exception ignored) {
        }

        stopClearCacheTimer();
        mDataMatrix.finish();
    }

    private void processIntent(Intent intent) {
        if(intent != null && intent.getData() != null) {
            mBackButtonFinishesActivity = true;

            hideAllWebView();
            showWebApp(intent.getDataString(), "");

            return;
        }
        if (intent != null && intent.getExtras() != null) {
            // get the url
            @NonNls String url;
            String machineUuid;
            String uuid;
            Bundle extras = intent.getExtras();
            Bundle bundle1 = extras.getBundle("extras"); //NON-NLS

            mBackButtonFinishesActivity = false;

            if (bundle1 != null && bundle1.containsKey("url"))
                url = bundle1.getString("url");
            else
                return;

            mDomainBanList = null;
            mDomainIncludeList = null;
            String userAgent = bundle1.getString("useragent", ""); //NON-NLS
            String webAppDomainBan = bundle1.getString("webAppDomainBan", ""); //NON-NLS
            String webAppDomainInclude = bundle1.getString("webAppDomainInclude", ""); //NON-NLS
            clearCachePeriodically = bundle1.getBoolean("clearCachePeriodically", false);
            if (!webAppDomainBan.isEmpty()) mDomainBanList = webAppDomainBan.split(";");
            if (!webAppDomainInclude.isEmpty()) mDomainIncludeList = webAppDomainInclude.split(";");

            // make sure url is valid
            if (url == null || url.isEmpty())
                return;

            getWindow().getDecorView().setContentDescription(url);

            // If this is the survey monkey app. Get the real url from the model provider
            if (url.equals("surveymonkey")) {
                url = DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING);
            } else if (url.contains("jfit")) {
                switch (DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)) {
                    case DAPIKeys.Constants.DAPI_DEPLOYMENT_DEV:
                        url = "https://dev-" + url;
                        break;
                    case DAPIKeys.Constants.DAPI_DEPLOYMENT_QA:
                        url = "https://qa-" + url;
                        break;
                    case DAPIKeys.Constants.DAPI_DEPLOYMENT_STAGING:
                        url = "https://staging-" + url;
                        break;
                    case DAPIKeys.Constants.DAPI_DEPLOYMENT_PROD:
                    default:
                        url = "https://" + url;
                        break;
                }
                // new web url for workout tracking does not need this stuff, but does need the environment prefix
                if(!url.contains(WORKOUT_HISTORY_URL_BASE)) {
                    machineUuid = DapiUtil.getMachineId();
                    url += "?machine_uuid=" + machineUuid;

                    int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
                    LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);
                    url += "&language=" + languageProperty.getLanguageAcronym();
                    url += "&region=" + languageProperty.locale;
                    url += "&club_id=" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT);
                    url += "&latitude=" + DataMatrix.getFloat(SettingKeys.Weather.DATA_WEATHER_LATITUDE_FLOAT);
                    url += "&longitude=" + DataMatrix.getFloat(SettingKeys.Weather.DATA_WEATHER_LONGITUDE_FLOAT);
                    url += "&units=" + (DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? "metric" : "imperial");

                    uuid = DapiUser.getWebUuid();
                    url += "&uuid=" + uuid;

                    url += "&mode=";
                    if (bundle1.containsKey("mode")) { //NON-NLS
                        url += bundle1.getString("mode");
                    }

                    BksCredentialProvider credentialProvider = new BksCredentialProvider(getApplicationContext());
                    String saltToken = credentialProvider.getString("HOTP_SALT_TOKEN");
                    mHotpToken = WebAppHelper.GenerateHotpToken(
                            machineUuid,
                            "",
                            saltToken
                    );
                }
            }
            Log.d(TAG, "url = " + url);
            showWebApp(url, userAgent);
        }
    }

    // show the web app
    public void showWebApp(String url, String userAgent) {
        hideAllWebView();

        // if the web app was already launched, show it; else load it
        mCurrentWebView = mWebViewMap.get(url);
        if (mCurrentWebView != null) {
            mCurrentWebView.show();
        } else {
            boolean shrinkWidth = false;
            // hack to make facebook width smaller
            //if (url.contains("facebook")) {
            //shrinkWidth = true;
            //setDisablePopup(true);
            //}

            mCurrentWebView = new WebContainerView(this, mNonVideoLayout, mVideoLayout, shrinkWidth);
            mWebViewMap.put(url, mCurrentWebView);
            mCurrentWebView.setHotpToken(mHotpToken);
            if (!userAgent.isEmpty()) {
                mCurrentWebView.mWebView.getSettings().setUserAgentString(userAgent);
            } else {
                mCurrentWebView.mWebView.getSettings().setUserAgentString(WebSettings.getDefaultUserAgent(getApplicationContext()));
            }
            mCurrentWebView.mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
            mCurrentWebView.loadUrl(url);
            mCurrentWebView.show();

            // hack to disable facebook button for myfitnesspal
            if (url.contains("myfitnesspal")) {
                mCurrentWebView.mWebView.getSettings().setSupportMultipleWindows(true);
            }

        }

    }

    // reload the current app
    public void reload() {
        if (mCurrentWebView != null) {
            mCurrentWebView.loadUrl(mCurrentWebView.mUrl);
        }
    }

    // hide all web views
    private void hideAllWebView() {
        for (Map.Entry<String, WebContainerView> stringWebContainerViewEntry : mWebViewMap.entrySet()) {
            WebContainerView view = (WebContainerView) ((Map.Entry) stringWebContainerViewEntry).getValue();
            view.hide();
        }
        closePopupView();
    }

    private void closePopupView() {
        mHandler.post(() -> {
            if (mPopupViewLayout != null)
                mPopupViewLayout.setVisibility(View.INVISIBLE);
            if (mPopupWebView != null)
                mPopupWebView.reset();
        });
    }

    private void showPopupView(final String url) {
        mHandler.post(() -> {
            mPopupViewLayout.setVisibility(View.VISIBLE);
            mPopupWebView.loadWebsite(url);
        });
    }

    public void onHidePopup(View v) {
        closePopupView();
    }

    public class WebContainerView {
        VideoEnabledWebView mWebView;
        String mUrl = "";

        @SuppressLint("SetJavaScriptEnabled")
        WebContainerView(Context context, RelativeLayout nonVideoLayout, ViewGroup videoLayout, boolean shrinkWidth) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

            if (shrinkWidth) {
                layoutParams = new RelativeLayout.LayoutParams(500, RelativeLayout.LayoutParams.MATCH_PARENT);
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            }

            mWebView = new VideoEnabledWebView(context);
            mWebView.setBackgroundColor(Color.BLACK);
            mWebView.setVisibility(View.GONE);
            nonVideoLayout.addView(mWebView, layoutParams);

            VideoEnabledWebChromeClient mChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout);

            mWebView.addJavascriptInterface(new WebAppInterface(context), "external");
            mWebView.setWebChromeClient(mChromeClient);
            mWebView.getSettings().setGeolocationDatabasePath(context.getFilesDir().getPath());

            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onLoadResource(WebView view, String url) {
                    if (url.contains("m.youtube.com/ptracking")) {
                        Log.d("WebAppActivity", "####CS WebAppActivity onLoadResource, url: " + url);
                    }
                    super.onLoadResource(view, url);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Log.d(TAG, "shouldOverrideUrlLoading: " + url);
                    WebAppHelper.incrementUid();

                    // if we need to force app to not navigate to a domain using ban list
                    if (mDomainBanList != null) {
                        for (String domainBan : mDomainBanList) {
                            if (url.toLowerCase().contains(domainBan.toLowerCase())) {
                                return true;
                            }
                        }
                    }

                    // if we need to force app to not navigate to a domain using include list
                    if (mDomainIncludeList != null) {
                        boolean domainFound = false;
                        for (String domainInclude : mDomainIncludeList) {
                            if (url.toLowerCase().contains(domainInclude.toLowerCase())) {
                                domainFound = true;
                            }
                        }
                        if (!domainFound) return true;
                    }

                    return !url.startsWith("http://") && !url.startsWith("https://");

                    //if (url.contains("intent://")) return true;
                    //if (url.contains("market://")) return true;
                    //return false;
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Log.e(TAG, "onReceivedError: " + errorCode + " ==> " + description);
                    view.loadUrl("about:blank");
                    alertDialog = ProjectDialogFactory.message(view, description, () -> alertDialog = null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (JumpTokenHost.allowsShim(url)) WebAppHelper.injectScriptFile(view, mShimText);
                    if (JumpTokenHost.allows(url)) WebAppHelper.injectTokens(view);
                }
            });
        }

        public void show() {
            mWebView.onResume();
            mWebView.setVisibility(View.VISIBLE);
        }

        public void hide() {
            mWebView.onPause();
            mWebView.setVisibility(View.GONE);
            if (alertDialog != null) {
                alertDialog.hide();
                alertDialog = null;
            }
        }

        @SuppressWarnings("deprecation")
        public void loadUrl(String url) {
            mUrl = url;
            mWebView.clearView();
            mWebView.loadUrl(mUrl);
        }

        void setHotpToken(String token) {
            mWebView.setHotpToken(token);
        }
    }


    public class WebAppInterface {
        Context _context;

        /**
         * Instantiate the interface and set the context
         */
        WebAppInterface(Context c) {
            _context = c;
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void popup(String url) {
            showPopupView(url);
        }

    }

}
