package com.jht.chimera.service.tiers;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

public class UpgradeSkin implements Parcelable, Serializable {
    private static final String TAG = "UpgradeSkin";
    // This is the background color to use for the pop up dialog.  Note if this is transparent, ignore this value.
    private int backgroundColor;

    // This is the custom bitmap to use for the pop up dialog.  Ignore if this is null.
    private Bitmap background = null;

    // This is the custom bitmap for the upgrade button.  Ignore if this is null.
    private Bitmap upgradeButton = null;
    private Bitmap upgradeButtonPressed = null;

    // This is the custom bitmap for the cancel button.  Ignore if this is null.
    private Bitmap cancelButton = null;
    private Bitmap cancelButtonPressed = null;

    // This is the custom bitmap for the ok button.  This is used if the user cannot perform the upgrade on console.  Ignore if this is null.
    private Bitmap okButton = null;
    private Bitmap okButtonPressed = null;

    // This contains the title to display in the dialog box.  It contains a hash map with all of the translations.
    private HashMap<String, String> title = new HashMap<String, String>();

    // This is the default title to use if the Locale passed in is not in the hash map.
    private String defaultTitle = null;

    private String backgroundPath = null;
    private String cancelButtonPath = null;
    private String okButtonPath = null;
    private String upgradeButtonPath = null;
    private String cancelButtonPressedPath = null;
    private String okButtonPressedPath = null;
    private String upgradeButtonPressedPath = null;

    /*
     * Create the upgrade object based on the inputs.
     */
    public UpgradeSkin()
    {
        backgroundColor = Color.BLACK;
    }


    /*
     * This creates the object based on the parcel passed in.
     */
    public UpgradeSkin(Parcel source)
    {
        backgroundColor = source.readInt();
        background = source.readParcelable(Bitmap.class.getClassLoader());
        upgradeButton = source.readParcelable(Bitmap.class.getClassLoader());
        upgradeButtonPressed = source.readParcelable(Bitmap.class.getClassLoader());
        cancelButton = source.readParcelable(Bitmap.class.getClassLoader());
        cancelButtonPressed = source.readParcelable(Bitmap.class.getClassLoader());
        okButton = source.readParcelable(Bitmap.class.getClassLoader());
        okButtonPressed = source.readParcelable(Bitmap.class.getClassLoader());

        int numValues = source.readInt();
        for(int i = 0; i < numValues; i++)
        {
            String key = source.readString();
            String value = source.readString();
            title.put(key, value);
            if(i == 0)
            {
                defaultTitle = value;
            }
        }

        backgroundPath = source.readString();
        cancelButtonPath = source.readString();
        okButtonPath = source.readString();
        upgradeButtonPath = source.readString();
        cancelButtonPressedPath = source.readString();
        okButtonPressedPath = source.readString();
        upgradeButtonPressedPath = source.readString();

    }

    /*
     * Common overloads used by the binder to pass this object.
     */
    public static final Parcelable.Creator<UpgradeSkin> CREATOR = new Parcelable.Creator<UpgradeSkin>()
    {
        public UpgradeSkin createFromParcel(Parcel in)
        {
            return new UpgradeSkin(in);
        }

        public UpgradeSkin[] newArray(int size)
        {
            return new UpgradeSkin[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    /*
     * Write this object to the parcel.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(backgroundColor);
        dest.writeParcelable(background, 0);
        dest.writeParcelable(upgradeButton, 0);
        dest.writeParcelable(upgradeButtonPressed, 0);
        dest.writeParcelable(cancelButton, 0);
        dest.writeParcelable(cancelButtonPressed, 0);
        dest.writeParcelable(okButton, 0);
        dest.writeParcelable(okButtonPressed, 0);

        Set<String> keys = title.keySet();
        dest.writeInt(keys.size());
        for(String k : keys)
        {
            dest.writeString(k);
            dest.writeString(title.get(k));
        }

        dest.writeString(backgroundPath);
        dest.writeString(cancelButtonPath);
        dest.writeString(okButtonPath);
        dest.writeString(upgradeButtonPath);
        dest.writeString(cancelButtonPressedPath);
        dest.writeString(okButtonPressedPath);
        dest.writeString(upgradeButtonPressedPath);
    }

    /*
     * Add the title for the locale.  Set the default if not set.
     */
    public void addTitle(String locale, String title)
    {
        this.title.put(locale, title);
        if(locale.equals("en_US") || defaultTitle == null) //NON-NLS
        {
            defaultTitle = title;
        }
    }

    public void setBackgroundColor(int color)
    {
        Log.e(TAG, "setting the background color: " + color);
        backgroundColor = color;
    }
    public void setBackground(Bitmap bitmap)
    {
        background = bitmap;
    }

    public void setBackgroundPath(String backgroundPath) {
        this.backgroundPath = backgroundPath;
    }

    public String getBackgroundPath() {
        return backgroundPath;
    }

    public void setCancelButtonPath(String cancelButtonPath) {
        this.cancelButtonPath = cancelButtonPath;
    }

    public String getCancelButtonPath() {
        return cancelButtonPath;
    }

    public void setOkButtonPath(String okButtonPath) {
        this.okButtonPath = okButtonPath;
    }

    public String getOkButtonPath() {
        return okButtonPath;
    }

    public void setUpgradeButtonPath(String upgradeButtonPath) {
        this.upgradeButtonPath = upgradeButtonPath;
    }

    public String getUpgradeButtonPath() {
        return upgradeButtonPath;
    }

    public void setCancelButtonPressedPath(String cancelButtonPressedPath) {
        this.cancelButtonPressedPath = cancelButtonPressedPath;
    }

    public String getCancelButtonPressedPath() {
        return cancelButtonPressedPath;
    }

    public void setOkButtonPressedPath(String okButtonPressedPath) {
        this.okButtonPressedPath = okButtonPressedPath;
    }

    public String getOkButtonPressedPath() {
        return okButtonPressedPath;
    }

    public void setUpgradeButtonPressedPath(String upgradeButtonPressedPath) {
        this.upgradeButtonPressedPath = upgradeButtonPressedPath;
    }

    public String getUpgradeButtonPressedPath() {
        return upgradeButtonPressedPath;
    }

    public void setUpgradeButton(Bitmap bitmap)
    {
        upgradeButton = bitmap;
    }
    public void setCancelButton(Bitmap bitmap)
    {
        cancelButton = bitmap;
    }
    public void setOkButton(Bitmap bitmap)
    {
        okButton = bitmap;
    }
    public void setUpgradeButtonPressed(Bitmap bitmap)
    {
        upgradeButtonPressed = bitmap;
    }
    public void setCancelButtonPressed(Bitmap bitmap)
    {
        cancelButtonPressed = bitmap;
    }
    public void setOkButtonPressed(Bitmap bitmap)
    {
        okButtonPressed = bitmap;
    }


    /*
     * Getters for all of the object data.
     */
    public int backgroundColor() { return backgroundColor; }
    public Bitmap background() { return background; }
    public Bitmap upgradeButton() { return upgradeButton; }
    public Bitmap cancelButton() { return cancelButton; }
    public Bitmap okButton() { return okButton; }
    public Bitmap upgradeButtonPressed() { return upgradeButtonPressed; }
    public Bitmap cancelButtonPressed() { return cancelButtonPressed; }
    public Bitmap okButtonPressed() { return okButtonPressed; }

    public String getTitle(String locale)
    {
        String t = title.get(locale);
        return t == null ? defaultTitle : t;
    }

}
