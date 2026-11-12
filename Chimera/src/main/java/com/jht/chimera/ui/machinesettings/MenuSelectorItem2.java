package com.jht.chimera.ui.machinesettings;


import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.MenuSelectorItem;
import com.jht.chimera.R;

import java.util.List;

/**
 * This is a simple implementation of the ISelectorItem.  This only needs a label for every item.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class MenuSelectorItem2 extends MenuSelectorItem {

    private Drawable triangleRight;
    private Drawable triangleDown;


    /**
     * Default constructor for an item with children.
     *
     * @param label The label for the item.
     */
    public MenuSelectorItem2(String label, @NonNull Context context, @Nullable List<ISelectorItem> subItems) {
        super(label, context, subItems);
        init(context);
    }

    public MenuSelectorItem2(String label, @NonNull Context context) {
        super(label, context);
        init(context);
    }

    private void init(Context context) {
        triangleRight = ContextCompat.getDrawable(context, R.drawable.triangle_right_2);
        triangleDown = ContextCompat.getDrawable(context, R.drawable.triangle_down_2);
    }

    @Override
    public Drawable postImage() {
        if (numChildren() > 0 && showSubItems()) {
            return triangleDown;
        }
        return triangleRight;
    }

    @Override
    public boolean showPostImage() {
        return true;
    }


}
