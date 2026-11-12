package com.jht.chimera.lib.workoutapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.ui.machinesettings.JHTTypes;

import java.io.File;

public class AppIcon {

    /**
     * String id for 3rd party apps, web apps, our run screen activity apps. Sent when getStringId is called. Used to
     * determine file path when creating app icon. Used to hide web and survey when needed. Used for analytics.
     */
    public String iconId;
    /** app name displayed to the user */
    public String appName;
    /** app icon display to the user */
    public int appIcon; // we could change this to Uri if we need consistent external references
    /** the actual app info associated with this icon */
    public AppInfo appInfo;

    // interfaces we may be looking to phase out
    private IAppIcon mAppIconInterface = null;
    private IAppName mAppNameInterface = null;
    private IAppShown mAppShownInterface = null;
    private IAppDisabled mAppDisabledInterface = null;

    public interface IAppIcon {
        Drawable getAppIcon();
    }

    public interface IAppName {
        String getAppName();
    }

    public interface IAppShown {
        boolean getAppShown(AppInfo info, FrameConfig config, int workoutId, String startingAppId);
    }

    public interface IAppDisabled {
        boolean getAppDisabled();
    }

    AppIcon(String iconId, String appName, @NonNull AppInfo info) {
        this.iconId = iconId;
        this.appName = appName;
        this.appInfo = info;
    }

    AppIcon(String iconId, String appName, int iconResource, @NonNull AppInfo info) {
        this.iconId = iconId;
        this.appName = appName;
        this.appIcon = iconResource;
        this.appInfo = info;
    }

    void setAppIconLogic(IAppIcon appIconLogic) {
        mAppIconInterface = appIconLogic;
    }

    void setAppNameLogic(IAppName appNameLogic) {
        mAppNameInterface = appNameLogic;
    }

    void setAppShownLogic(IAppShown appShownLogic) {
        mAppShownInterface = appShownLogic;
    }

    void setAppDisabledLogic(IAppDisabled appDisabledLogic) {
        mAppDisabledInterface = appDisabledLogic;
    }

    /**
     * Sets whether the app icon is displayed or not. Can override to provide custom logic for whether
     * the app should display or not.
     *
     * @param frameConfig The frame config
     * @param startingAppId    The launched workout app UUID
     * @return Whether to show the app
     */
    public boolean isShown(FrameConfig frameConfig, int workoutId, String startingAppId) {
        if (appInfo.state != null && appInfo.state == JHTTypes.FEATURE_STATE.FEATURE_HIDDEN) return false;

        if (mAppShownInterface == null) {
            return true;
        } else {
            return mAppShownInterface.getAppShown(appInfo, frameConfig, workoutId, startingAppId);
        }
    }

    public String getIconId() {
        return iconId;
    }

    public int getAppAnalyticsId() {
        return appInfo.appAnalyticsId;
    }

    public String getAppName() {
        if (mAppNameInterface == null) {
            if(appName.equals("")) {
                return getAppNameFromPackage();
            }
            return appName;
        } else {
            return mAppNameInterface.getAppName();
        }
    }

    public String getAppPackage() {
        return appInfo.appPackage;
    }

    public String getAppActivity() {
        return appInfo.appActivity;
    }

    public String getUrl() {
        return appInfo.urlExtra;
    }

    public String getUuid() {
        return appInfo.uuid;
    }

    public boolean isDisabled() {
        if (mAppDisabledInterface != null) {
            return mAppDisabledInterface.getAppDisabled();
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) { // currently triggered in a contains check as part of app order logic
        if (obj instanceof AppIcon) {
            AppIcon app = (AppIcon) obj;
            return getIconId().equals(app.getIconId());
        }
        return false;
    }

    private String getAppNameFromPackage() {
        Context context = ContextUtil.getApplicationContext();
        if(context != null) {
            try {
                CharSequence charSequence = context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(appInfo.appPackage, 0));
                return charSequence == null ? "" : charSequence.toString();
            } catch (PackageManager.NameNotFoundException e) {
                return "";
            }
        }
        return "";
    }

    public Drawable getAppIcon() {
        if (mAppIconInterface == null) {
            Context context = ContextUtil.getApplicationContext();
            Drawable drawable = null;

            File filepath = new File(context.getFilesDir() + "/settings/apps/bitmaps/" + iconId.toLowerCase() + ".png"); //NON-NLS
            if (filepath.exists()) {
                drawable = Drawable.createFromPath(filepath.getAbsolutePath());
            } else if (appIcon != 0) {
                drawable = ContextCompat.getDrawable(context, appIcon);
                //drawable = DrawableUtil.getDrawableForUri(context, appIcon); // if we decide to do URI appIcon instead of resource ID
            }
            if (drawable == null) {
                try {
                    drawable = context.getPackageManager().getApplicationIcon(appInfo.appPackage);
                } catch (Exception e) {
                    drawable = new ColorDrawable(Color.WHITE);
                }
            }
            return drawable;
        } else {
            return mAppIconInterface.getAppIcon();
        }
    }

    @Override
    public String toString() {
        return iconId;
    }
}