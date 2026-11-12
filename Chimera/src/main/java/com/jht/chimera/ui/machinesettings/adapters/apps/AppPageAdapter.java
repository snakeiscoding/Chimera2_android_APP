package com.jht.chimera.ui.machinesettings.adapters.apps;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Giancarlo.Cuadros on 1/25/2019.
 */

public class AppPageAdapter extends FragmentStatePagerAdapter {


//    private AppSettings appSettings;
    private List<AppPageFragment> pages = new ArrayList<>();

    public AppPageAdapter(FragmentManager fm) {
        super(fm);
        int count = getCount();

        while (pages.size() > count) {
            pages.remove(pages.size() - 1);
        }

        for (int i = 0; i < count; i++) {
            pages.add(AppPageFragment.newInstance(i));
        }
    }

    public List<AppPageFragment> getPages() {
        // Gets the pages in this adapter
        return pages;
    }

    @Override
    public Fragment getItem(int position) {
        if (position >= pages.size()) {
            pages.add(AppPageFragment.newInstance(position));
        }
        return pages.get(position);
    }

    @Override
    public int getCount() {
        // Get number of pages to display
        List<AppIcon> appIcons = AppIconManager.getInstance().getAppOrder();
        return (int) Math.ceil((double) appIcons.size() / (double) AppIconManager.APPS_PER_PAGE);
    }
}
