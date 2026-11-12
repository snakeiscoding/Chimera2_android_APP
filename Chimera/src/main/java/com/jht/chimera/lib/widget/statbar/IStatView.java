package com.jht.chimera.lib.widget.statbar;

import android.view.View;

/**
 * A view can implement this interface and then be passed into a stat column as a valid candidate
 * for displaying stat data.
 * Created by chris.foyer on 8/8/2017.
 */
public interface IStatView
{
    /**
     * Sets the string value of the view displaying the stat title
     *
     * @param title The title to set to
     */
    void titleChanged(String title);

    /**
     * Sets the string value of the view displaying the stat value
     *
     * @param value The value to set to
     */
    void valueChanged(String value);

    /**
     * Sets the supplementary icon that is on stat views. This is not always set.
     * @param iconId The icon to display
     */
    void setIcon(Integer iconId);

    /**
     * Sets the background color of the stat. The background color should be set back to default if the value is null.
     * @param color The color to set, or null for the default color
     */
    void setStatBackgroundColor(Integer color);

    /**
     * Sets the text color to use for the stat. This will help with contrast in conjunction with the background color.
     * @param color The text color to set
     */
    void setTextColor(Integer color);

    /**
     * Sets the visibility of the stat item to either View.VISIBLE, View.INVISIBLE, or View.GONE.
     * When this is called the view is responsible for handling any required animations and
     * setting the visibility appropriately.
     *
     * @param visibility The visibility constants from View (View.VISIBLE etc)
     */
    void setStatVisibility(int visibility);

    /**
     * Sets the on click listener for the view
     * @param clickListener The click listener to set
     */
    void setOnClickListener(View.OnClickListener clickListener);
}
