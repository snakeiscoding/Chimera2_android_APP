package com.jht.chimera.service.tiers;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

public class FeatureUpgrade implements Parcelable, Serializable {
    // Unique ID for the upgrade.  This is set by the web service running in the cloud.
    private String id;

    // This is the url to hit to upgrade the feature.
    private String url;

    // This contains the text to display in the dialog box.  It contains a hash map with all of the translations.
    private HashMap<String, String> text = new HashMap<String, String>();

    // This contains the title to display in the dialog box.  It contains a hash map with all of the translations.
    private HashMap<String, String> title = new HashMap<String, String>();

    // This is the default text to use if the Locale passed in is not in the hash map.
    private String defaultText = null;

    // This is the default title to use if the Locale passed in is not in the hash map.
    private String defaultTitle = null;

    // True if the upgrade can be performed.  This is generally used for guest.
    private boolean canUpgrade = false;

    // This is true if we should launch a webview to handle the upgrade.
    private boolean launchWebView = false;

    // These are used to control the webview size.
    private int webViewWidth = 1024;
    private int webViewHeight = 500;


    /*
     * Create the upgrade object based on the inputs.
     */
    public FeatureUpgrade(String id, String url, boolean canUpgrade, boolean launchWebView, int webViewWidth, int webViewHeight)
    {
        this.id = id;
        this.url = url;
        this.canUpgrade = canUpgrade;
        this.launchWebView = launchWebView;
        this.webViewWidth = webViewWidth;
        this.webViewHeight = webViewHeight;
    }


    /*
     * This creates the object based on the parcel passed in.
     */
    public FeatureUpgrade(Parcel source)
    {
        id = source.readString();
        url = source.readString();
        int numValues = source.readInt();
        for(int i = 0; i < numValues; i++)
        {
            String key = source.readString();
            String value = source.readString();
            text.put(key, value);
            if(i == 0)
            {
                defaultText = value;
            }
        }
        numValues = source.readInt();
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
        canUpgrade = source.readInt() == 1;
        launchWebView = source.readInt() == 1;

        webViewWidth = source.readInt();
        webViewHeight = source.readInt();

    }

    /*
     * Common overloads used by the binder to pass this object.
     */
    public static final Parcelable.Creator<FeatureUpgrade> CREATOR = new Parcelable.Creator<FeatureUpgrade>()
    {
        public FeatureUpgrade createFromParcel(Parcel in)
        {
            return new FeatureUpgrade(in);
        }

        public FeatureUpgrade[] newArray(int size)
        {
            return new FeatureUpgrade[size];
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
        dest.writeString(id);
        dest.writeString(url);
        Set<String> keys = text.keySet();
        dest.writeInt(keys.size());
        for(String k : keys)
        {
            dest.writeString(k);
            dest.writeString(text.get(k));
        }
        keys = title.keySet();
        dest.writeInt(keys.size());
        for(String k : keys)
        {
            dest.writeString(k);
            dest.writeString(title.get(k));
        }
        dest.writeInt(canUpgrade ? 1 : 0);
        dest.writeInt(launchWebView ? 1 : 0);
        dest.writeInt(webViewWidth);
        dest.writeInt(webViewHeight);

    }

    /*
     * Add the text for the locale.  Set the default if not set.
     */
    public void addText(String locale, String text)
    {
        this.text.put(locale, text);
        if(locale.equals("en_US") || defaultText == null) //NON-NLS
        {
            defaultText = text;
        }
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



    /*
     * Getters for all of the object data.
     */
    public String id() { return id; }
    public String url() { return url; }

    public String getText(String locale)
    {
        String t = text.get(locale);
        return t == null ? defaultText : t;
    }

    public String getTitle(String locale)
    {
        String t = title.get(locale);
        return t == null ? defaultTitle : t;
    }
    public boolean canUpgrade() { return this.canUpgrade; }
    public boolean launchWebView() { return this.launchWebView; }

    public int webViewWidth() { return this.webViewWidth; }
    public int webViewHeight() { return this.webViewHeight; }
}
