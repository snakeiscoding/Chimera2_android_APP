package com.jht.chimera.service.tiers;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.chimera.ui.machinesettings.JHTTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PremiumFeatureSet implements Parcelable, Serializable {
    // List of features.
    private List<PremiumFeature> features = new ArrayList<PremiumFeature>();

    // List of upgrades.
    private List<FeatureUpgrade> upgrades = new ArrayList<FeatureUpgrade>();

    // Used to customize the upgrade dialog.
    private UpgradeSkin skin = null;


    public PremiumFeatureSet()
    {

    }

    /*
     * Create from parcel.
     */
    @SuppressWarnings("unchecked")
    public PremiumFeatureSet(Parcel source)
    {
        features = source.readArrayList(PremiumFeature.class.getClassLoader());
        upgrades = source.readArrayList(FeatureUpgrade.class.getClassLoader());
        skin = source.readParcelable(UpgradeSkin.class.getClassLoader());
    }

    /*
     * Standard overrids needed by system binder.
     */
    public static final Parcelable.Creator<PremiumFeatureSet> CREATOR = new Parcelable.Creator<PremiumFeatureSet>()
    {
        public PremiumFeatureSet createFromParcel(Parcel in)
        {
            return new PremiumFeatureSet(in);
        }

        public PremiumFeatureSet[] newArray(int size)
        {
            return new PremiumFeatureSet[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    /*
     * Write object to parcel.  Called by the system binder.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeList(features);
        dest.writeList(upgrades);
        dest.writeParcelable(skin, 0);
    }

    /*
     * Used to add features and upgrades to the set.
     */
    public void addFeature(PremiumFeature feature)
    {
        if(!features.contains(feature))
        {
            features.add(feature);
        }
    }
    public void addUpgrade(FeatureUpgrade upgrade)
    {
        if(!upgrades.contains(upgrade))
        {
            upgrades.add(upgrade);
        }
    }
    public void setUpgradeSkin(UpgradeSkin skin)
    {
        this.skin = skin;
    }

    /*
     * Getters for the features and upgrades.
     */
    public List<PremiumFeature> getFeatures()
    {
        return features;
    }
    public List<FeatureUpgrade> getUpgrades()
    {
        return upgrades;
    }
    public UpgradeSkin getUpgradeSkin()
    {
        return skin;
    }

    /*
     * Helper function to grap a list of upgrades in the set that correspond to the given feature.
     */
    public List<FeatureUpgrade> getUpgrades(PremiumFeature feature)
    {
        List<FeatureUpgrade> featureUpgrades = new ArrayList<FeatureUpgrade>();
        for(FeatureUpgrade upgrade : upgrades)
        {
            if(feature.containsUpgrade(upgrade.id()))
                featureUpgrades.add(upgrade);
        }
        return featureUpgrades;
    }


    public FeatureUpgrade getUpgrade(String upgradeId)
    {
        if(upgrades != null)
        {
            for(FeatureUpgrade upgrade : upgrades)
            {
                if(upgrade.id().equals(upgradeId))
                    return upgrade;
            }
        }
        return null;
    }

    /*
     * If the upgrade input was run successfully this will search the set for the features that
     * contain the upgrade and enable them.
     */
    public void upgradeSuccessful(FeatureUpgrade upgrade)
    {
        for(PremiumFeature feature : features)
        {
            if(feature.containsUpgrade(upgrade.id()))
            {
                feature.setFeatureState(JHTTypes.FEATURE_STATE.FEATURE_ENABLED);
            }
        }
    }



    /*
     * If the upgrade input was run successfully this will search the set for the features that
     * contain the upgrade and enable them.
     */
    public void upgradeSuccessful(String upgradeId)
    {
        for(PremiumFeature feature : features)
        {
            if(feature.containsUpgrade(upgradeId))
            {
                feature.setFeatureState(JHTTypes.FEATURE_STATE.FEATURE_ENABLED);
            }
        }
    }

    /*
     * Find the feature given the id and type.  Return null if not found.
     */
    public PremiumFeature getFeature(String id, JHTTypes.FEATURE_TYPE featureType)
    {
        for(PremiumFeature feature : features)
        {
            if(feature.id().equals(id) && feature.getFeatureType() == featureType)
                return feature;
        }
        return null;
    }

    public boolean isEmpty() {
        return (features.size() == 0 && upgrades.size() == 0);
    }

}
