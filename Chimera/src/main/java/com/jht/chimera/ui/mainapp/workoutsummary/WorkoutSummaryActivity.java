package com.jht.chimera.ui.mainapp.workoutsummary;

import android.annotation.SuppressLint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.BTSinkKeys;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.widget.activity.TimeoutActivity;
import com.jht.chimera.ui.mainapp.util.BackgroundManager;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import java.io.File;

/**
 * Controls the workout summary screen.
 */
public class WorkoutSummaryActivity extends TimeoutActivity {
    private static final String TAG = "WorkoutSummaryActivity";
    private View mHomeButton;

    private ViewPager mPager;
    private SummaryPagerAdapter pagerAdapter;
    private ViewGroup webSurveySection;

    private final WorkoutSummaryFullFragment fullFragment = new WorkoutSummaryFullFragment();
    private final WorkoutSummaryQuickFragment quickFragment = new WorkoutSummaryQuickFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.workout_summary_layout);
        ViewGroup workoutSummaryRootView = findViewById(R.id.workout_summary_root_view);
        try {
            File path = new File(getFilesDir(), "/settings/theme/color");
            if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_COLOR_BOOL) && path.exists()) {
                ExecuteAsRoot.chmodRecursive("777", path.getAbsolutePath());
                File[] files = path.listFiles();
                if (files != null && files.length != 0) {
                    workoutSummaryRootView.setBackground(new BitmapDrawable(getResources(), BackgroundManager.decodeBackgroundImage(files[0])));
                } else {
                    workoutSummaryRootView.setBackground(ContextCompat.getDrawable(this, R.drawable.background_color_workout_summary));
                }
            } else {
                workoutSummaryRootView.setBackground(ContextCompat.getDrawable(this, R.drawable.background_color_workout_summary));
            }
        } catch (Exception e) {
            workoutSummaryRootView.setBackground(ContextCompat.getDrawable(this, R.drawable.background_color_workout_summary));
        }
        mHomeButton = findViewById(R.id.home_button_image_view);
        webSurveySection = findViewById(R.id.web_survey_section);

        mPager = findViewById(R.id.summary_pager);
        pagerAdapter = new SummaryPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);

        setDefaultScreenTimeout(LaunchAppUtil::launchSplashScreen);

        DataMatrix.putInt(BTSinkKeys.DATA_BT_SINK_NUM_FOLDER_ITEMS_INT, -1);
        DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_PLAYBACK_IS_PLAYING_BOOL, false);
        DataMatrix.putBytes(BTSinkKeys.DATA_BT_SINK_CURRENT_FOLDER_BYTES, null);

        checkAddWebView();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        DataMatrix.putBoolean(HALKeys.DATA_HOMEKEY_ENABLE_BOOL, true);
        mHomeButton.setEnabled(true);
        mHomeButton.setOnClickListener(v -> {
            mHomeButton.setEnabled(false);

            //Stores home button press events for Dapi Analytics
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.HOME_BTN_PRESS.name);
            LaunchAppUtil.launchSplashScreen();
        });
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.WORKOUT_SUMMARY.name());
    }

    // have seen rare crash in parker where inflation throws on webview, so add it programmatically if needed
    private void checkAddWebView() {
        try {
            WebView webView = findViewById(R.id.workout_summary_survey);
            if (webView == null) {
                webView = new WebView(this);
                webView.setId(R.id.workout_summary_survey);
                webSurveySection.addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding WebView", e);
        }
    }

    /**
     * Display the post workout survey if enabled. This setting is enabled
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void displayPostWorkoutSurvey() {
        if (webSurveySection != null && webSurveySection.getVisibility() != View.VISIBLE) {
            try {
                final WebView webView = findViewById(R.id.workout_summary_survey);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setDomStorageEnabled(true);
                webView.loadUrl(DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING));
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        webView.loadUrl(url);
                        return true;
                    }
                });

                mHomeButton.setOnClickListener(v -> {
                    if (webSurveySection.getVisibility() == View.VISIBLE) {
                        webSurveySection.setVisibility(View.GONE);
                        return;
                    }

                    LaunchAppUtil.launchSplashScreen();
                });
                webSurveySection.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error showing survey", e);
            }
        }
    }

    private class SummaryPagerAdapter extends FragmentStatePagerAdapter {
        public SummaryPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return position == 0 ? quickFragment : fullFragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
