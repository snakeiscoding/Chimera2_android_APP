package com.jht.chimera.lib.widget.statbar;

/**
 * An item that is tracked and displayed by a {@link StatColumn}.
 * <p>
 * Created by chris.foyer on 8/8/2017.
 */
public class StatItem {
    private boolean mHidden = false; // This is used by StatColumn for showing temporary stats
    private String mTitle;
    private String mValue;
    private int mOrder;
    private int mStatId;
    private Integer mIcon = null;
    private Integer mBackgroundColor = null;
    private Integer mTextColor = null;
    private IStatView mStatView = null;

    // For the stat column to track the current position of each stat item in the layout
    int displayPosition;

    public StatItem(String title, String value, int order, int statId) {
        mTitle = title;
        mValue = value;
        mOrder = order;
        mStatId = statId;
    }

    /**
     * Sets the stat view that this stat item is associated with. When this is called it will
     * automatically call both update methods with the new values associated with this item.
     *
     * @param statView The stat view that will display the values
     */
    void setStatView(IStatView statView) {
        mStatView = statView;
        if (mTitle != null) sendTitleChange(mTitle);
        if (mValue != null) sendValueChange(mValue);
        sendIconChange(mIcon);
        sendBackgroundChange(mBackgroundColor);
        sendTextColorChange(mTextColor);
    }

    /**
     * Sets the title of a stat item. The title should describe the value it contains.
     *
     * @param title The title of the stat item
     */
    public void setTitle(String title) {
        if (!mTitle.equals(title)) {
            mTitle = title;
            if (mStatView != null) sendTitleChange(title);
        }
    }

    /**
     * Sets the value of a stat item. The value should be the data that the title is describing.
     *
     * @param value The value of the stat item
     */
    public void setValue(String value) {
        if (!mValue.equals(value)) {
            mValue = value;
            if (mStatView != null) sendValueChange(value);
        }
    }

    public void setIcon(Integer iconId) {
        mIcon = iconId;
        if (mStatView != null) sendIconChange(mIcon);
    }

    public void setStatBackground(Integer color) {
        mBackgroundColor = color;
        if (mStatView != null) sendBackgroundChange(color);
    }

    public void setStatTextColor(Integer color) {
        mTextColor = color;
        if (mStatView != null) sendTextColorChange(color);
    }

    /**
     * Gets the order that this stat item should appear in inside a stat column. Used internally
     * by {@link StatColumn}.
     *
     * @return The order of this stat item
     */
    public int getOrder() {
        return mOrder;
    }

    /**
     * Sets whether the stat item is hidden. If a stat item is hidden it will not send events to
     * the associated stat view. This is used by {@link StatColumn} for showing a temporary column
     * over the top of a primary stat item. When it is no longer hidden it will immediately send
     * events for the value and the title.
     *
     * @param hidden Whether the stat item should enter or leave hidden mode.
     */
    void setHidden(boolean hidden) {
        if (hidden && !mHidden) {
            mHidden = true;
        } else if (!hidden && mHidden) {
            mHidden = false;
            sendValueChange(mValue);
            sendTitleChange(mTitle);
            sendIconChange(mIcon);
            sendBackgroundChange(mBackgroundColor);
            sendTextColorChange(mTextColor);
        }
    }

    private void sendValueChange(String value) {
        if (!mHidden && mStatView != null) {
            mStatView.valueChanged(value);
        }
    }

    private void sendTitleChange(String title) {
        if (!mHidden && mStatView != null) {
            mStatView.titleChanged(title);
        }
    }

    private void sendIconChange(Integer iconId) {
        if (!mHidden && mStatView != null) {
            mStatView.setIcon(iconId);
        }
    }

    private void sendBackgroundChange(Integer color) {
        if (!mHidden && mStatView != null) {
            mStatView.setStatBackgroundColor(color);
        }
    }

    private void sendTextColorChange(Integer color) {
        if (!mHidden && mStatView != null) {
            mStatView.setTextColor(color);
        }
    }

    public IStatView getStatView() {
        return mStatView;
    }

    public int getStatId() {
        return mStatId;
    }
}
