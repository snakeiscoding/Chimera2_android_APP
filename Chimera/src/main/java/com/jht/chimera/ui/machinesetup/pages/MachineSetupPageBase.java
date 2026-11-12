package com.jht.chimera.ui.machinesetup.pages;

import android.content.Context;
import android.view.View;

import com.jht.androiduiwidgets.viewpager.StandardPageBase;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;

public abstract class MachineSetupPageBase extends StandardPageBase {

    private MachineSetupPageData pageData;

    public MachineSetupPageBase(final MachineSetupPageData pageData) {
        super(true);
        this.pageData = pageData;
    }

    protected MachineSetupPageData pageData() {
        return pageData;
    }

    abstract public boolean onNext();

    public void updateText(Context context) {
    }

    /**
     * Null safe helper so that Machine Setup pages can update view visibility.
     *
     * @param view      view to be updated
     * @param visible   visibility to set: GONE, VISIBLE, or INVISIBLE
     */
    protected void setVisible(View view, int visible) {
        if (view != null) {
            view.setVisibility(visible);
        }
    }
}
