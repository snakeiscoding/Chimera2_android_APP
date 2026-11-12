package com.jht.chimera.service.tiers;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.chimera.ui.machinesettings.JHTTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PremiumFeature implements Parcelable, Serializable {
    // This is the name of the feature.
    private String name;

    // This is the identifier for the feature.  For workouts this is the workout id.  For apps this is the app UUID.
    private String id;

    // This contains a list of upgrade actions.  Each entry is the UUID for the upgrade action and is used to map to
    // a FeatureUpgrade object.
    private List<String> upgradeActions = new ArrayList<String>();

    // The is the default upgrade action as there may be several upgrades you can make.
    private String defaultUpgradeAction = "";

    // This is the feature type (workout or app).
    private JHTTypes.FEATURE_TYPE featureType;

    // This is the state of the feature (on or off).
    private JHTTypes.FEATURE_STATE featureState;


    /*
     * The default constructor used to create the object.
     */
    public PremiumFeature(String name, String id, JHTTypes.FEATURE_TYPE featureType, JHTTypes.FEATURE_STATE featureState)
    {
        this.name = name;
        this.id = id;
        this.featureType = featureType;
        this.featureState = featureState;
    }


    /*
     * Create the object from a parcel.  This is used by the system binder.
     */
    @SuppressWarnings("unchecked")
    public PremiumFeature(Parcel source)
    {
        name = source.readString();
        id = source.readString();
        upgradeActions = source.readArrayList(String.class.getClassLoader());
        defaultUpgradeAction = source.readString();
        featureType = (JHTTypes.FEATURE_TYPE)source.readSerializable();
        featureState = (JHTTypes.FEATURE_STATE)source.readSerializable();
    }


    /*
     * Standard binder helper functions.
     */
    public static final Parcelable.Creator<PremiumFeature> CREATOR = new Parcelable.Creator<PremiumFeature>()
    {
        public PremiumFeature createFromParcel(Parcel in)
        {
            return new PremiumFeature(in);
        }

        public PremiumFeature[] newArray(int size)
        {
            return new PremiumFeature[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }


    /*
     * Write the object to a parcel.  This is used by the system binder.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(name);
        dest.writeString(id);
        dest.writeList(upgradeActions);
        dest.writeString(defaultUpgradeAction);
        dest.writeSerializable(featureType);
        dest.writeSerializable(featureState);
    }

    /*
     * Used to add an upgrade link to the feature.
     */
    public void addUpgrade(String upgrade, boolean isDefault)
    {
        upgradeActions.add(upgrade);
        if(isDefault || defaultUpgradeAction == null)
        {
            defaultUpgradeAction = upgrade;
        }
    }

    /*
     * Getters for all of the object data.
     */
    public String name() { return this.name; }
    public String id() { return this.id; }
    public String getDefaultUpgrade() {	return defaultUpgradeAction; }
    public boolean containsUpgrade(String upgrade) { return upgradeActions.contains(upgrade); }
    public JHTTypes.FEATURE_TYPE getFeatureType() { return featureType; }
    public JHTTypes.FEATURE_STATE getFeatureState() { return featureState; }

    /*
     * Allow the client to update the feature state.
     */
    public void setFeatureState(JHTTypes.FEATURE_STATE state) { featureState = state; }
}
