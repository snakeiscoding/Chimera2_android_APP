package com.jht.chimera.ui.mainapp.goscreen;

import androidx.fragment.app.Fragment;

public class CategoryItem {
    /** Title that shoes on the button that opens the category page */
    public String buttonTitle;
    /** Title that shows at the top of the screen when this category is open */
    public String pageTitle;
    public int icon;
    public Fragment fragment;

    public CategoryItem(String buttonTitle, String pageTitle, int icon, Fragment fragment){
        this.buttonTitle = buttonTitle;
        this.pageTitle = pageTitle;
        this.icon = icon;
        this.fragment = fragment;
    }

    public CategoryItem(String name, int icon, Fragment fragment) {
        this(name, name, icon, fragment);
    }
}
