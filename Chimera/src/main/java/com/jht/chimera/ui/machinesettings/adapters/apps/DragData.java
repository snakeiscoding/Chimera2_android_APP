package com.jht.chimera.ui.machinesettings.adapters.apps;

import android.view.View;

/**
 * Contains information about the icon that's being dragged
 *
 * Created by Giancarlo.Cuadros on 2/6/2019.
 */

public class DragData {
    private View mView;
    private int mCurrentPosition;
    private int mPage;
    private boolean mIsActive; // Whether the app being dragged is from the hidden category
    private String mItemTitle;
    private String mId;

    DragData(String id, View view, int currentPosition, int page, boolean isActive, String itemTitle) {
        this.mId = id;
        this.mView = view;
        this.mCurrentPosition = currentPosition;
        this.mPage = page;
        this.mIsActive = isActive;
        this.mItemTitle = itemTitle;
    }

    public String getId() {
        return mId;
    }

    public String getItemTitle() {
        return mItemTitle;
    }

    public void setPosition(int position) {
        this.mCurrentPosition = position;
    }

    public void setPage(int page) {
        this.mPage = page;
    }

    public View getView() {
        return mView;
    }

    public int getHolderLayoutPosition() {
        return mCurrentPosition;
    }

    public int getPage() {
        return mPage;
    }

    public boolean isActive() {
        return mIsActive;
    }
}
