package com.jht.chimera.ui.machinesettings.pages;

import static com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys.Constants.DAPI_DEPLOYMENT_DEV;
import static com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys.Constants.DAPI_DEPLOYMENT_PROD;
import static com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys.Constants.DAPI_DEPLOYMENT_QA;
import static com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys.Constants.DAPI_DEPLOYMENT_STAGING;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.webapp.WebAppHelper;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.oauthlib.auth.BksCredentialProvider;

import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Implements the weather page gluing the page to the model layer.
 */
public class Weather extends SettingPageBase {

    private final static String TAG = "Weather";
    private DataMatrix dataMatrix = new DataMatrix();
    private UserTextInput defaultCity;
    private UserTextInput defaultCityLat;
    private UserTextInput defaultCityLong;
    private WebView webView;

    public class WeatherJavascript {

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void setWeatherLocation(final String city, final double latitude, final double longitude) {
            Log.d(TAG, "setLocation called with city " + city);
            page().post(() -> {
                DataMatrix.putString(SettingKeys.Weather.DATA_WEATHER_CITY_STRING, city);
                DataMatrix.putFloat(SettingKeys.Weather.DATA_WEATHER_LATITUDE_FLOAT, (float) latitude);
                DataMatrix.putFloat(SettingKeys.Weather.DATA_WEATHER_LONGITUDE_FLOAT, (float) longitude);
                defaultCity.setText(city);
                defaultCityLat.setText(String.format(Locale.ENGLISH, "%.4f", latitude));
                defaultCityLong.setText(String.format(Locale.ENGLISH, "%.4f", longitude));
            });
        }
    }

    public Weather() {
    }

    private SimpleMessageDialog messageDialog = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected View onCreate(Context context, ViewGroup container) {

        View view = CommonRoutines.createWebWidget(context, R.layout.weather, TAG, null);
        if(view != null) {
            container.addView(view);

            defaultCity = view.findViewById(R.id.eng_weather_default_city);
            defaultCityLat = view.findViewById(R.id.eng_weather_default_city_lat);
            defaultCityLong = view.findViewById(R.id.eng_weather_default_city_long);

            defaultCity.setText(DataMatrix.getString(SettingKeys.Weather.DATA_WEATHER_CITY_STRING));
            defaultCityLat.setText(String.format(Locale.ENGLISH, "%.4f", DataMatrix.getFloat(SettingKeys.Weather.DATA_WEATHER_LATITUDE_FLOAT)));
            defaultCityLong.setText(String.format(Locale.ENGLISH, "%.4f", DataMatrix.getFloat(SettingKeys.Weather.DATA_WEATHER_LONGITUDE_FLOAT)));

            webView = view.findViewById(R.id.weather_settings_web_view);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new WeatherJavascript(), "external");
            webView.setWebViewClient(new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return false;
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    view.loadUrl("about:blank");
                    if (messageDialog == null) {
                        CharSequence message = error.getDescription();
                        if (message != null) {
                            messageDialog = ProjectDialogFactory.message(view, message.toString(), () -> messageDialog = null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        } else {
                            messageDialog = ProjectDialogFactory.message(view, getContext().getResources().getString(R.string.unable_to_load_pg), () -> messageDialog = null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        }
                    }
                }

            });
        }

        return view;
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        BksCredentialProvider credentialProvider = new BksCredentialProvider(getContext().getApplicationContext());
        String saltToken = credentialProvider.getString("HOTP_SALT_TOKEN");
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("x-jfit-authorization",
                WebAppHelper.GenerateHotpToken(
                        DapiUtil.getMachineId(),
                        DapiUser.GUEST_USER_UUID,
                        saltToken

                )
        );
        webView.loadUrl(createUrl(), extraHeaders);
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();
    }

    private String createUrl() {
        @NonNls String params = "";
        @NonNls String website;

        int dapiEnvEnum = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT);

        switch (dapiEnvEnum) {
            case DAPI_DEPLOYMENT_PROD:
                params += "?";
                website = "https://location.jfit.co"; // NON-NLS
                break;
            case DAPI_DEPLOYMENT_STAGING:
                params += "?";
                website = "https://staging-location.jfit.co"; // NON-NLS
                break;
            case DAPI_DEPLOYMENT_QA:
                params += "?";
                website = "https://qa-location.jfit.co"; // NON-NLS
                break;
            case DAPI_DEPLOYMENT_DEV:
                params += "?";
                website = "https://dev-location.jfit.co"; // NON-NLS
                break;
            default:
                return "";
        }

        int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
        LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);

        params += "machine_uuid=" + DapiUtil.getMachineId();
        params += "&region=" + languageProperty.locale;
        params += "&language=" + languageProperty.getLanguageAcronym();
        params += "&club_id=" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT);

        float longitude = DataMatrix.getFloat(SettingKeys.Weather.DATA_WEATHER_LONGITUDE_FLOAT);
        float latitude = DataMatrix.getFloat(SettingKeys.Weather.DATA_WEATHER_LATITUDE_FLOAT);
        String cityString = DataMatrix.getString(SettingKeys.Weather.DATA_WEATHER_CITY_STRING);

        if (!cityString.isEmpty() && latitude != 0 && longitude != 0) {
            params += "&latitude=" + latitude;
            params += "&longitude=" + longitude;
            params += "&defaultCity=" + cityString;
        }

        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);

        params += "&units=" + (isMetric ? "metric" : "imperial");
        params += "&uuid=" + DapiUser.getWebUuid();
        params += "&mode=settings";

        return website + params;
    }

}

