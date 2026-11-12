package com.jht.chimera.service.tiers;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.webapp.WebAppHelper;
import com.jht.chimera.service.tiers.helpers.DownloadHelper;
import com.jht.chimera.ui.machinesettings.JHTTypes;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.oauthlib.auth.BksCredentialProvider;

import org.jetbrains.annotations.NonNls;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class Tiers extends Service {
    @NonNls private static final String TAG = "Tiers";
    DataMatrix mDataMatrix = new DataMatrix();
    private String userId = "";
    private PremiumFeatureSet set = new PremiumFeatureSet();
    private final Object setLock = new Object();
    private FeatureUpgrade currentUpgrade;
    protected CacheControl cache = new CacheControl();
    private boolean tiersNeedsValidMachineId = false;

    private String getSaltToken() {
        BksCredentialProvider credentialProvider = new BksCredentialProvider(getApplicationContext());
        return credentialProvider.getString("HOTP_SALT_TOKEN");
    }

    private DownloadHelper.IAllDownloadsComplete downloadCompleteHandler = () -> {
        Log.e(TAG, "All downloads are complete...");
        if(set.isEmpty()) {
            //The club does NOT do tiers

            for (PremiumFeature feature : set.getFeatures()) {
                Log.e(TAG, "feature: " + feature.name() + ", State: " + feature.getFeatureState().name());
            }
            DataMatrix.putBoolean(TiersKeys.DATA_USING_TIERS_BOOL, false);
            DataMatrix.putParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, set);
            Log.e(TAG, "we just pushed NO tiers");
        }
        else if(set.getUpgrades().size() == 0) {
            synchronized (this) {
                DataMatrix.putBoolean(TiersKeys.DATA_USING_TIERS_BOOL, true);
                Log.e(TAG, "we just pushed guest tiers");
                Log.e(TAG, "determining tier type: " + userId);

                if(userId == null  || userId.isEmpty() || userId.equals(DapiUser.GUEST_USER_UUID)) {

                    DataMatrix.putParcelable(TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE, set);

                    Log.e(TAG, "we have guest tiers");
                    File file = new File("sdcard/Download/guest_settings_feature_set");
                    if (file.exists())
                        FileUtils.delete(file);
                    FileOutputStream fileOut = null;
                    try {
                        fileOut = new FileOutputStream("sdcard/Download/guest_settings_feature_set");
                        ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
                        synchronized (setLock) {
                            objOut.writeObject(set);
                        }
                        objOut.close();
                    } catch (FileNotFoundException e) {
                        Log.w(TAG, e);
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }

                    Log.e(TAG, "we just saved a copy of guest tiers");
                    Log.e(TAG, "we just pushed guest tiers");
                } else {
                    DataMatrix.putParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, set);
                    Log.e(TAG, "we just pushed highest tiers");
                }
            }

        } else {
            DataMatrix.putBoolean(TiersKeys.DATA_USING_TIERS_BOOL, true);
            DataMatrix.putParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, set);
            Log.e(TAG, "we just pushed premium features");
            //Premium User Tiers
        }
//            if(featuresUpdatedNotification != null)
//            {
//                featuresUpdatedNotification.onFeaturesUpdated(user, set);
//            }

    };

    private DownloadHelper downloadHelper = new DownloadHelper(downloadCompleteHandler);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d("ServiceStarter", "Starting " + this);
        super.onCreate();
        init();
        Log.d("ServiceStarter", "Ending " + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "Tiers onDestroy ");
    }

    private void init() {

        mDataMatrix.createBoolAdapter(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL, valid -> {
            if(valid){
                if(tiersNeedsValidMachineId){
                    tiersNeedsValidMachineId = false;
                    updateTiers();
                }
            }
        });

        // TODO - why don't we just listen for DAPIKeys.DATA_USER_DAPI_UUID_STRING ?
        // Also why keep 2 feature sets, could we just have the current user set?
        Log.e(TAG, "Tiers about to set adapter ");
        mDataMatrix.createBoolAdapter(TiersKeys.DATA_UPDATE_TIERS_BOOL, isMachineIdSafe -> {
            Log.e(TAG, "DATA_UPDATE_TIERS_BOOL " + isMachineIdSafe);
            if(isMachineIdSafe) {
                updateTiers();
            } else {
                if(DataMatrix.getBoolean(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL)){
                    updateTiers();
                }else {
                    tiersNeedsValidMachineId = true;
                }
            }
        }, true);

        mDataMatrix.createParcelableAdapter(TiersKeys.DATA_UPGRADE_PARCELABLE, FeatureUpgrade.class, featureUpgrade -> {
            currentUpgrade = featureUpgrade;
            upgradeTiers();
            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                Log.e(TAG, "in Tiers upgrade: " + currentUpgrade.url());
            }
        });

    }

    private void upgradeTiers() {
        new Handler().post(() -> {
            // user should already be setup if upgrading, so keep userId the same?
            Thread updateTiersThread = new Thread(_upgradeTiers);
            updateTiersThread.start(); // TODO - what about any previous threads?
        });
    }

    private void updateTiers() {
        new Handler().post(() -> {
            userId = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING);
            Thread updateTiersThread = new Thread(_updateTiers);
            updateTiersThread.start(); // TODO - what about any previous threads?
        });
    }

    private URL getURL() {
        @NonNls String url = "";
        switch(DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT))
        {
            case DAPIKeys.Constants.DAPI_DEPLOYMENT_DEV:
                url += "https://dev-console-feature.jfit.co/console/features/?";
                break;
            case DAPIKeys.Constants.DAPI_DEPLOYMENT_PROD:
                url += "https://console-feature.jfit.co/console/features/?";
                break;
            case DAPIKeys.Constants.DAPI_DEPLOYMENT_QA:
                url += "https://qa-console-feature.jfit.co/console/features/?";
                break;
            case DAPIKeys.Constants.DAPI_DEPLOYMENT_STAGING:
                url += "https://staging-console-feature.jfit.co/console/features/?";
                break;
            default:
                return null;
        }

        try {
            int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
            LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);

            url += "machine_uuid=" + URLEncoder.encode(
                    DapiUtil.getMachineId(), "UTF-8" );
            url += "&frame_serial=" + URLEncoder.encode(
                    DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING), "UTF-8" );
            url += "&console_serial=" + URLEncoder.encode(
                    DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING), "UTF-8" );
            //url += "&mac_address=" + AddressHelper.getMACAddressStr("wlan0", false);
            url += "&club_id=" + URLEncoder.encode("" +
                    DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT), "UTF-8");
            url += "&region=" + languageProperty.locale;
            url += "&language=" + languageProperty.getLanguageAcronym();
            url += "&uuid=" + userId;

//            if(cameFromLogin) {
//                url = "https://s3.amazonaws.com/chimera-testing/Tiers-wc.xml";
//                cameFromLogin = false;
//            }


            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                Log.e(TAG, "url: " + url);
            }

            return new URL(url);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException", e);
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException", e);
        }


        return null;
    }

    private InputStream getXMLInputStream() {

        try {
            URL url = getURL();
            if (url == null) {
                return null;
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            String hotpToken = WebAppHelper.GenerateHotpToken(
                    DapiUtil.getMachineId(),
                    "",
                    getSaltToken()
            );
            if (hotpToken != null && !hotpToken.equals("")) {
                connection.addRequestProperty("x-jfit-authorization", hotpToken);
            }

            if(userId == null || userId.isEmpty() || userId.equals(DapiUser.GUEST_USER_UUID)) {
                cache.addHeaders(connection);
            }
//            if(cache != null)
//                cache.addHeaders(connection);

            Log.e(TAG, "Header Fields: " + connection.getHeaderFields() );
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                Log.e(TAG, "we will NOT return null,  " + "HTTP_OK" );
                if(userId == null || userId.isEmpty() || userId.equals(DapiUser.GUEST_USER_UUID)) {
                    cache.updateFields(connection);
                }
//                if(cache != null)
//                    cache.updateFields(connection);

                return connection.getInputStream();
            } else if(connection.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR){

                Log.e(TAG, "server error,  " + "HTTP_INTERNAL_ERROR" );

            } else if(connection.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST){
                Log.e(TAG, "no club id was provided,  " + "HTTP_BAD_REQUEST" );
            }
            else if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.e(TAG, "xml has not been modified. returning null");
                return null;
            }
            else
                {
                Log.e(TAG, "we will return null,  " + connection.getResponseCode() );
                }

        } catch (Exception ex) {
            Log.e(TAG, "catch block of getXMLInputStream, ", ex);
        }

        Log.e(TAG, "totally got here");
        FileInputStream fileIn = null;
        try {

            fileIn = new FileInputStream("/sdcard/Download/guest_settings_feature_set"); //NON-NLS
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            PremiumFeatureSet guestSet = (PremiumFeatureSet) objIn.readObject();
            DataMatrix.putParcelable(TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE, guestSet);


            objIn.close();

        } catch (FileNotFoundException e) {
            Log.w(TAG, e);
        } catch (IOException e) {
            Log.w(TAG, e);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, e);
        }

        //TODO: I think this is where we apply guest features if something went wrong
        // (internet failed)
//        Intent intent = new Intent("com.jht.controller.tiers.updateTiers");
//        intent.putExtra("userType", "guest");
//        sendBroadcast(intent);

        return null;

    }

    private URL getUpgradeURL() {
        @NonNls String url = currentUpgrade.url() + "/?";

        try {
            url += "club_id=" + URLEncoder.encode("" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT), "UTF-8");
            url += "&uuid=" + userId;

            //Removing for GDPR purposes
            //Log.d("tiers", "Get upgrade url " + url);

            return new URL(url);
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "UnsupportedEncodingException ", e);
            return null;
        }catch (MalformedURLException e) {
            Log.d(TAG, "MalformedURLException ", e);
            return null;
        }
    }

    private InputStream getXMLUpgradeInputStream() {
        try {
            @NonNls URL url = getUpgradeURL();
            if (url == null) {
                return null;
            }

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            String hotpToken = WebAppHelper.GenerateHotpToken(
                    DapiUtil.getMachineId(),
                    "",
                    getSaltToken()
            );
            if (hotpToken != null && !hotpToken.equals("")) {
                connection.addRequestProperty("x-jfit-authorization", hotpToken);
            }

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.addRequestProperty("Content-Type", "application/json");
            OutputStream postStream = connection.getOutputStream();
            String postData = "{ \"membership_id\": \"" + currentUpgrade.id() + "\" }"; // NON-NLS
            postStream.write(postData.getBytes());
            postStream.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                Log.e(TAG, "we will NOT return null,  " + "HTTP_OK" );
//                if(cache != null)
//                    cache.updateFields(connection);

                return connection.getInputStream();
            } else if(connection.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR){

                Log.e(TAG, "server error,  " + "HTTP_INTERNAL_ERROR" );

            } else if(connection.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST){
                Log.e(TAG, "no club id was provided,  " + "HTTP_BAD_REQUEST" );
            } else {
                Log.e(TAG, "we will return null,  " + connection.getResponseCode() );
            }

        } catch (Exception ex) {
            Log.e(TAG, "catch block of getXMLInputStream, ", ex);
        }

        return null;
    }

    private Runnable _upgradeTiers = () -> {
        InputStream stream = getXMLUpgradeInputStream();
        if(stream == null)
        {
            return;
        }

        FileOutputStream fout = null;
        try {
            File featuresXML = new File("/sdcard/Download/upgrade_response.xml");
            if(featuresXML.exists()){
                FileUtils.delete(featuresXML);
            }
            fout = new FileOutputStream("/sdcard/Download/upgrade_response.xml");
            byte[] buffer = new byte[4096];
            int bytes = 0;
            while((bytes = stream.read(buffer)) >= 0)
            {
                fout.write(buffer, 0, bytes);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException: ", e);
            return;
        } catch (IOException e) {
            return;
        }
        finally
        {
            if(fout != null)
            {
                try {
                    fout.close();
                } catch (IOException e) {
                }
            }
        }

        if(stream != null)
        {
            try {
                stream.close();
                stream = null;
            } catch (IOException e) {
            }
        }
        try {
            stream = new FileInputStream("/sdcard/Download/upgrade_response.xml");
        } catch (FileNotFoundException e) {
            return;
        }

        XmlPullParserFactory factory;
        try {
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput( stream, "UTF-8" );

//            userModel.getCurrentFeatureSet().features.clear(false);
//            userModel.getCurrentFeatureSet().upgrades.clear(false);
//            userModel.getCurrentFeatureSet().invalidate();

            parseUpgradeXML(parser);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XmlPullParserException: ", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException: ", e);
        }finally
        {
            try {
                if(stream != null)
                {
                    stream.close();
                }
            } catch (IOException e) {
            }
        }
    };

    private Runnable _updateTiers = () -> {
        try {
            InputStream stream = getXMLInputStream();
            if (stream == null) {

                return;
            }

            FileOutputStream fout = null;
            try {
                File featuresXML = new File("/sdcard/Download/premium_features.xml");
                if (featuresXML.exists()) {
                    FileUtils.delete(featuresXML);
                }
                fout = new FileOutputStream("/sdcard/Download/premium_features.xml");
                byte[] buffer = new byte[4096];
                int bytes = 0;
                while ((bytes = stream.read(buffer)) >= 0) {
                    fout.write(buffer, 0, bytes);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "FileNotFoundException: ", e);
                return;
            } catch (IOException e) {
                return;
            } finally {
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (IOException e) {
                    }
                }
            }

            if (stream != null) {
                try {
                    stream.close();
                    stream = null;
                } catch (IOException e) {
                }
            }
            try {
                stream = new FileInputStream("/sdcard/Download/premium_features.xml");
            } catch (FileNotFoundException e) {
                return;
            }

            XmlPullParserFactory factory;
            try {
                factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(stream, "UTF-8");

//            userModel.getCurrentFeatureSet().features.clear(false);
//            userModel.getCurrentFeatureSet().upgrades.clear(false);
//            userModel.getCurrentFeatureSet().invalidate();
                Log.e(TAG, "checking out the time HERE");
                parseXML(parser);
            } catch (XmlPullParserException e) {
                Log.e(TAG, "XmlPullParserException: ", e);
            } catch (IOException e) {
                Log.e(TAG, "IOException: ", e);
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException e) {
                }
            }
        }
        finally {

            Log.d(TAG, "Got tiers");
            DataMatrix.putBoolean(DAPIKeys.DATA_GETTING_USER_TIERS_BOOL, false);
        }
    };

    private enum XML_PARSING_STATE {
        TOP_LEVEL,
        PREMIUM_FEATURE,
        UPGRADE_ACTION,
        UPGRADE_SKIN,
        TITLE,
        TEXT,
        UPGRADE_SKIN_TITLE;
    };

    private void parseUpgradeXML(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        boolean finished = false;
        while (!finished && eventType != XmlPullParser.END_DOCUMENT){
            switch(eventType)
            {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    if(parser.getName().equals("UpgradeResult"))
                    {
                        String result = parser.getAttributeValue("", "result");
                        eventType = parser.next();
                        if(eventType == XmlPullParser.TEXT)
                        {
                            Log.e(TAG, "" + (result != null && result.toLowerCase().equals("success")));
                            if(result != null && result.toLowerCase().equals("success")) {
                                PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                                if(set != null) {
                                    synchronized (setLock) {
                                        for (PremiumFeature feature : this.set.getFeatures()) {
                                            if (feature.containsUpgrade(currentUpgrade.id())) {
                                                feature.setFeatureState(JHTTypes.FEATURE_STATE.FEATURE_ENABLED);
                                            }
                                        }
                                    }
                                    DataMatrix.putParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, this.set);
                                }
                            }

                            Bundle bundle = new Bundle(getClassLoader());
                            bundle.putBoolean("result", (result != null && result.toLowerCase().equals("success")));
                            bundle.putString("message", parser.getText());

                            DataMatrix.putParcelable(TiersKeys.ACTION_UPGRADE_RESULT_PARCELABLE, bundle);

//                            if(featureUpgraded != null)
//                            {
//                                featureUpgraded.onFeatureUpgraded(result != null && result.toLowerCase().equals("success"), parser.getText(), upgrade);
//                            }
                            finished = true;
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    break;
                default:
                    break;
            }
            eventType = parser.next();
        }
    }

    private void parseXML(XmlPullParser parser) throws XmlPullParserException, IOException {

        if(set.isEmpty()) {
            Log.e(TAG, "set was empty");
        } else {
            Log.e(TAG, "set was not empty. we should recreate it");
            synchronized (setLock) {
                set = new PremiumFeatureSet();
            }
        }

        int eventType = parser.getEventType();
        XML_PARSING_STATE state = XML_PARSING_STATE.TOP_LEVEL;
         PremiumFeature feature = null;
        //com.jht.model.user.PremiumFeature f = null;
        FeatureUpgrade upgrade = null;
//        com.jht.model.user.FeatureUpgrade u = null;
        UpgradeSkin upgradeSkin = null;
//        com.jht.model.user.UpgradeSkin theSkin = null;
        while (eventType != XmlPullParser.END_DOCUMENT)
        {
            String name = parser.getName();
            if(name != null)
            {
                name = name.toLowerCase();
            }
            switch(eventType)
            {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    switch(state)
                    {
                        case TOP_LEVEL:
                            if(name != null && name.equals("premiumfeature"))
                            {
                                String featureType = parser.getAttributeValue("", "feature_type");

                                String featureState = parser.getAttributeValue("", "state").toLowerCase();
                                if(featureState != null)
                                {
                                    featureState = featureState.toLowerCase();
                                    JHTTypes.FEATURE_STATE s = JHTTypes.FEATURE_STATE.FEATURE_DISABLED;
                                    if(featureState.equals("enabled"))
                                    {
                                        s = JHTTypes.FEATURE_STATE.FEATURE_ENABLED;
                                    }
                                    else if(featureState.equals("disabled"))
                                    {
                                        s = JHTTypes.FEATURE_STATE.FEATURE_DISABLED;
                                    }
                                    else if(featureState.equals("limited"))
                                    {
                                        s = JHTTypes.FEATURE_STATE.FEATURE_LIMITED;
                                    }
                                    else if(featureState.equals("hidden"))
                                    {
                                        s = JHTTypes.FEATURE_STATE.FEATURE_HIDDEN;
                                    }


                                    if(featureType != null && (featureType.equals("workout") || featureType.equals("app")))
                                    {
                                        state = XML_PARSING_STATE.PREMIUM_FEATURE;

                                        feature = new PremiumFeature(
                                                parser.getAttributeValue("", "name"),
                                                parser.getAttributeValue("", "id"),
                                                featureType.equals("workout") ? JHTTypes.FEATURE_TYPE.FEATURE_TYPE_WORKOUT : JHTTypes.FEATURE_TYPE.FEATURE_TYPE_APP,
                                                s
                                        );
                                        synchronized (setLock) {
                                            set.addFeature(feature);
                                        }
//                                        f = userModel.getCurrentFeatureSet().features.createField();
//
//                                        f.name.setValue(parser.getAttributeValue("", "name"), false);
//                                        f.id.setValue(parser.getAttributeValue("", "id"), false);
//                                        f.featureType.setValue(featureType.equals("workout") ? JHTTypes.FEATURE_TYPE.FEATURE_TYPE_WORKOUT : JHTTypes.FEATURE_TYPE.FEATURE_TYPE_APP, false);
//                                        f.featureState.setValue(s, false);
//                                        userModel.getCurrentFeatureSet().features.insert(f, false);

                                    }
                                }

                            }
                            else if(name != null && name.equals("upgradeaction"))
                            {
                                @NonNls String id = parser.getAttributeValue("", "id");
                                @NonNls String url = parser.getAttributeValue("", "url");
                                @NonNls String allowed = parser.getAttributeValue("", "allowed");
                                @NonNls String launchWeb = parser.getAttributeValue("", "launch_site");
                                @NonNls String webWidth = parser.getAttributeValue("", "webview_width");
                                @NonNls String webHeight = parser.getAttributeValue("", "webview_height");
                                if(id != null && url != null)
                                {
                                    if(launchWeb != null && launchWeb.equals("true"))
                                    {
                                        int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
                                        LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);

                                        url += "/?machine_uuid=" + DapiUtil.getMachineId();
                                        url += "&frame_serial=" + URLEncoder.encode(
                                                DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING), "UTF-8" );
                                        url += "&console_serial=" + URLEncoder.encode(
                                                DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING), "UTF-8" );
                                        url += "&club_id=" + URLEncoder.encode("" +
                                                DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT), "UTF-8");
                                        url += "&region=" + languageProperty.locale;
                                        url += "&language=" + languageProperty.getLanguageAcronym();
                                        url += "&uuid=" + userId;
                                        url += "&membership_id=" + id;
                                    }
                                    int width = 1024;
                                    int height = 500;
                                    if(webWidth != null)
                                    {
                                        try
                                        {
                                            width = Integer.parseInt(webWidth);
                                            if(width < 300)
                                                width = 300;
                                            if(width > 1024)
                                                width = 1024;
                                        }
                                        catch(Exception ex)
                                        {
                                            width = 600;
                                        }
                                    }
                                    if(webHeight != null)
                                    {
                                        try
                                        {
                                            height = Integer.parseInt(webHeight);
                                            if(height < 300)
                                                height = 300;
                                            if(height > 500)
                                                height = 500;
                                        }
                                        catch(Exception ex)
                                        {
                                            height = 300;
                                        }
                                    }
                                    state = XML_PARSING_STATE.UPGRADE_ACTION;
//                                    u = userModel.getCurrentFeatureSet().upgrades.createField();
//                                    u.id.setValue(id, false);
//                                    u.canUpgrade.setValue(allowed != null && allowed.equals("true"), false);
//                                    u.url.setValue(url, false);
//                                    u.launchWebView.setValue(launchWeb != null && launchWeb.equals("true"), false);
//                                    u.webViewHeight.setValue(height, false);
//                                    u.webViewWidth.setValue(width, false);
//                                    userModel.getCurrentFeatureSet().upgrades.insert(u, false);
                                    upgrade = new FeatureUpgrade(id, url, allowed != null && allowed.equals("true"), launchWeb != null && launchWeb.equals("true"), width, height);

                                    synchronized (setLock) {
                                        set.addUpgrade(upgrade);
                                    }
                                }
                            }
                            else if(name != null && name.equals("upgradeskin"))
                            {
                                state = XML_PARSING_STATE.UPGRADE_SKIN;
                                upgradeSkin = new UpgradeSkin();
                            }
                            break;
                        case PREMIUM_FEATURE:
                            if(feature != null && name != null && name.equals("enableonupgrade"))
                            {
                                String upgradeId = parser.getAttributeValue("", "id");
                                String upgradeDefault = parser.getAttributeValue("", "default");

                                if(upgradeId != null && userId != null && !userId.equals("") && !userId.equals(DapiUser.GUEST_USER_UUID))
                                {
                                    feature.addUpgrade(upgradeId, upgradeDefault != null && upgradeDefault.equals("true"));
                                    //f.addUpgrade(upgradeId, upgradeDefault != null && upgradeDefault.equals("true"));
                                }
                            }
                            break;
                        case TEXT:
                            String locale = name;
                            if(parser.next() == XmlPullParser.TEXT)
                            {
//                                StringField value = u.text.createField(locale);
//                                value.setValue(parser.getText(), false);
//                                u.text.setValue(locale, value);
//
//                                if(locale.equals("en_US") || u.defaultText.getValue().isEmpty())
//                                {
//                                    u.defaultText.setValue(parser.getText());
//                                }
                                upgrade.addText(locale, parser.getText());
                            }
                            break;
                        case TITLE:
                            String titleLocale = name;
                            if(parser.next() == XmlPullParser.TEXT)
                            {
//                                StringField value = u.title.createField(titleLocale);
//                                value.setValue(parser.getText(), false);
//                                u.title.setValue(titleLocale, value);
//
//                                if(titleLocale.equals("en_US") || u.defaultTitle.getValue().isEmpty())
//                                {
//                                    u.defaultTitle.setValue(parser.getText());
//                                }

                                upgrade.addTitle(titleLocale, parser.getText());
                            }
                            break;
                        case UPGRADE_SKIN_TITLE:
                            String upgradeTitle = name;
                            if(parser.next() == XmlPullParser.TEXT)
                            {
//                                StringField value = theSkin.title.createField(upgradeTitle);
//                                value.setValue(parser.getText(), false);
//                                theSkin.title.setValue(upgradeTitle, value);
//
//                                if(upgradeTitle.equals("en_US") || theSkin.defaultTitle.getValue().isEmpty())
//                                {
//                                    theSkin.defaultTitle.setValue(parser.getText());
//                                }
                                upgradeSkin.addTitle(upgradeTitle, parser.getText());
                            }
                            break;
                        case UPGRADE_ACTION:
                            if(upgrade != null)
                            {
                                if(name != null && name.equals("text"))
                                {
                                    state = XML_PARSING_STATE.TEXT;
                                }

                                else if(name != null && name.equals("title"))
                                {
                                    state = XML_PARSING_STATE.TITLE;
                                }
                            }
                            break;
                        case UPGRADE_SKIN:
                            if(upgradeSkin != null)
                            {

                                if(name != null && name.equals("background"))
                                {
                                    String color = parser.getAttributeValue("", "color");
                                    String url = parser.getAttributeValue("", "url");
                                    String md5 = parser.getAttributeValue("", "md5");
                                    if(color != null)
                                    {
//                                        theSkin.backgroundColor.setValue(Color.parseColor(color), false);
                                         upgradeSkin.setBackgroundColor(Color.parseColor(color));
                                    }
                                    if(url != null)
                                    {
                                        //TODO:

                                        downloadHelper.download(DownloadHelper.DOWNLOAD_TYPE.UPGRADE_BACKGROUND, upgradeSkin, url, md5);
                                    }
                                }
                                else if(
                                        name != null && name.equals("upgradebutton") ||
                                                name != null && name.equals("cancelbutton") ||
                                                name != null && name.equals("upgradebuttonpressed") ||
                                                name != null && name.equals("cancelbuttonpressed") ||
                                                name != null && name.equals("okbutton") ||
                                                name != null && name.equals("okbuttonpressed")
                                )
                                {
                                    String url = parser.getAttributeValue("", "url");
                                    String md5 = parser.getAttributeValue("", "md5");
                                    if(md5 != null)
                                    {
                                        md5 = md5.toLowerCase();
                                    }
                                    if(url != null)
                                    {
                                        if(name != null && name.equals("upgradebutton"))
                                        {
                                            downloadHelper.download(DownloadHelper.DOWNLOAD_TYPE.UPGRADE_UPGRADE, upgradeSkin, url, md5);
                                        }
                                        else if(name != null && name.equals("cancelbutton"))
                                        {
                                            downloadHelper.download(DownloadHelper.DOWNLOAD_TYPE.UPGRADE_CANCEL, upgradeSkin, url, md5);
                                        }
                                        else if(name != null && name.equals("okbutton"))
                                        {
                                            downloadHelper.download(DownloadHelper.DOWNLOAD_TYPE.UPGRADE_OK, upgradeSkin, url, md5);
                                        }
                                        else if(name != null && name.equals("upgradebuttonpressed"))
                                        {
                                            downloadHelper.download(DownloadHelper.DOWNLOAD_TYPE.UPGRADE_UPGRADE_PRESSED, upgradeSkin, url, md5);
                                        }
                                        else if(name != null && name.equals("cancelbuttonpressed"))
                                        {
                                            downloadHelper.download(DownloadHelper.DOWNLOAD_TYPE.UPGRADE_CANCEL_PRESSED, upgradeSkin, url, md5);
                                        }
                                        else if(name != null && name.equals("okbuttonpressed"))
                                        {
                                            downloadHelper.download(DownloadHelper.DOWNLOAD_TYPE.UPGRADE_OK_PRESSED, upgradeSkin, url, md5);
                                        }
                                    }
                                }

                                else if(name != null && name.equals("title"))
                                {
                                    state = XML_PARSING_STATE.UPGRADE_SKIN_TITLE;
                                }
                            }
                            break;

                        default:
                            break;

                    }
                    break;
                case XmlPullParser.END_TAG:

                    switch(state)
                    {
                        case PREMIUM_FEATURE:
                            if(name != null && name.equals("premiumfeature"))
                            {
                                state = XML_PARSING_STATE.TOP_LEVEL;
                                feature = null;
                            }
                            break;
                        case TEXT:
                            if(name != null && name.equals("text"))
                            {
                                state = XML_PARSING_STATE.UPGRADE_ACTION;
                            }
                            break;
                        case TITLE:
                            if(name != null && name.equals("title"))
                            {
                                state = XML_PARSING_STATE.UPGRADE_ACTION;
                            }
                            break;
                        case UPGRADE_SKIN_TITLE:
                            if(name != null && name.equals("title"))
                            {
                                state = XML_PARSING_STATE.UPGRADE_SKIN;
                            }
                            break;
                        case TOP_LEVEL:
                            break;
                        case UPGRADE_ACTION:
                            if(name != null && name.equals("upgradeaction"))
                            {
                                state = XML_PARSING_STATE.TOP_LEVEL;
                                upgrade = null;
                            }
                            break;
                        case UPGRADE_SKIN:
                            if(name != null && name.equals("upgradeskin"))
                            {
                                synchronized (setLock) {
                                    set.setUpgradeSkin(upgradeSkin);
                                }
                                state = XML_PARSING_STATE.TOP_LEVEL;
                                upgradeSkin = null;
                            }
                            break;
                        default:
                            break;

                    }
                    break;
            }
            eventType = parser.next();
        }

        downloadHelper.doneAddingDownloads();



        /*
        // if (THERE ARE NO TIERS) --> make sure all apps are reset to their proper states.
        //
        //if( THERE ARE NO UPGRADES ) --> we now use guest features.
        //                                We give go screen module the guest feature set
        //                                We let the apps
        //
        //
        //

         */





    }

    private void sendTiersStatesToGoScreen(PremiumFeatureSet set) {
        //DataMatrix.put
        for(PremiumFeature feature: set.getFeatures()) {
            if(feature.getFeatureType() == JHTTypes.FEATURE_TYPE.FEATURE_TYPE_WORKOUT) {
                int featureID = Integer.parseInt(feature.id());

            }
        }
    }

}
