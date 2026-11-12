package com.jht.chimera.ui.mainapp.landmark;


import android.util.Log;

import com.jht.chimera.lib.landmark.Landmark;
import com.jht.chimera.lib.landmark.LandmarkManager;

import org.jetbrains.annotations.NonNls;

import java.util.List;

public class LandmarkLoader {
    @NonNls private static final String TAG = "LandmarkLoader";
    List<Landmark> mLandmarks;
    Landmark mSelectedLandmark;

    private LandmarkLoader() {
        init();
    }

    private static class SingletonHolder {
        private static final LandmarkLoader INSTANCE = new LandmarkLoader();
    }

    public static LandmarkLoader getInstance() {
        return LandmarkLoader.SingletonHolder.INSTANCE;
    }

    void init() {

        try {
            LandmarkManager landmarkManager = new LandmarkManager();
            mLandmarks = landmarkManager.loadFromXmlFile("landmarks.xml"); //NON-NLS
            reset();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void reset() {
        mSelectedLandmark = null;
        if (mLandmarks != null && mLandmarks.size() > 0) {
            mSelectedLandmark = mLandmarks.get(0);
        }
    }

    public Landmark getSelectedLandmark() {
        return mSelectedLandmark;
    }

    public List<Landmark> getLandmarks() {
        return mLandmarks;
    }

    public void selectLandmark(Landmark landmark) {
        mSelectedLandmark = landmark;
    }
}
