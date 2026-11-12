package com.jht.chimera.lib.widget.statbar;

import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages and controls one stat bar. This class is given a list of {@link IStatView} and connects
 * them to {@link StatItem}. To use create StatItem objects and provide a list of views that
 * implement IStatView.
 * <p>
 * You must also set up click events so that they are sent to {@link #statViewClicked(int)}. This
 * will control the behavior of the views and trigger callbacks for hiding and displaying.
 * <p>
 * Created by chris.foyer on 8/8/2017.
 */
public class StatColumn {
    private static final String TAG = "StatColumn";
    private List<StatItem> mStatItems;
    private List<IStatView> mStatViews;
    private StatItem mPrimaryItem;
    private boolean mStatsOpen = false; // Whether the stats are all visible
    private List<IStatColumnNotifications> mListeners = new ArrayList<>();

    /**
     * Creates a StatColumn with the provided stat items and stat views.
     *
     * @param statItems The initial statItems that will be used in the StatColumn
     * @param statViews The views that are part of the StatColumn
     */
    public StatColumn(List<StatItem> statItems, List<? extends IStatView> statViews) {
        if (statViews.size() >= statItems.size()) {
            mStatItems = new ArrayList<>(statItems);
            sortStatItemList(mStatItems);
            mStatViews = new ArrayList<>(statViews);
            mPrimaryItem = mStatItems.get(0);
            for (int i = 0; i < mStatItems.size(); i++) {
                StatItem statItem = mStatItems.get(i);
                IStatView statView = mStatViews.get(i);
                statItem.setStatView(statView);
                statItem.displayPosition = i;
            }
        } else {
            Log.e(TAG, "Not enough views to fit the number of stat items!");
        }
    }

    /**
     * Updates the stat items that are in the column.
     *
     * @param statItems The stat items in the column
     */
    public void updateAllStatItems(List<StatItem> statItems) {
        for(StatItem item : mStatItems) {
            item.setStatView(null);
        }
        if (mStatViews.size() >= statItems.size()) {
            mStatItems.clear();
            mStatItems = new ArrayList<>(statItems);
            sortStatItemList(mStatItems);
            mPrimaryItem = mStatItems.get(0);
            for (int i = 0; i < mStatItems.size(); i++) {
                StatItem statItem = mStatItems.get(i);
                IStatView statView = mStatViews.get(i);
                statItem.setStatView(statView);
                statItem.displayPosition = i;
            }
        } else {
            Log.e(TAG, "Not enough views to fit the number of stat items!");
        }
    }

    /**
     * Sorts stat items by their order
     *
     * @param statItems The stat item list to sort
     */
    private void sortStatItemList(List<StatItem> statItems) {
        Collections.sort(statItems, (statItem1, statItem2) -> {
            if (statItem1.getOrder() > statItem2.getOrder()) return 1;
            else if (statItem1.getOrder() == statItem2.getOrder()) return 0;
            else return -1;
        });
    }

    /**
     * Should be triggered when one of the views acting as a StatView is clicked. Will trigger
     * logic in the StatColumn to show and hide columns depending on the context.
     *
     * @param statIndex The index of the stat. 0 is the main stat that is always shown, 1 is the
     *                  first stat from it and so on.
     */
    public void statViewClicked(int statIndex) {
        if (statIndex == 0) {
            if (mStatsOpen) closeStats();
            else openStats();
        } else if (mStatsOpen) {
            if (statIndex > mStatItems.size()) return;
            StatItem newPrimary = getStatItemAtPosition(statIndex);
            if (newPrimary != null) {
                StatItem oldPrimary = mPrimaryItem;
                mPrimaryItem = newPrimary;
                if (mTempStatItem != null) newPrimary.setHidden(true);
                updateStatItemPositions();
                if (mTempStatItem != null) oldPrimary.setHidden(false);
                notifyPrimaryItemChanged();
                closeStats();
            } else {
                Log.e(TAG, "No stat found with position " + statIndex);
            }
        } else {
            Log.e(TAG, "This view should be hidden!");
        }
    }

    /**
     * Gets the stat currently displayed at a particular position
     * @param position The position to get the stat at
     * @return The stat item at the position
     */
    public StatItem getStatItemAtPosition(int position) {
        for (StatItem item : mStatItems) {
            if (item.displayPosition == position) return item;
        }
        return null;
    }

    /**
     * Updates the stat item positions based on the current primary item.
     */
    private void updateStatItemPositions() {
        int statViewPosition = 0;
        mPrimaryItem.displayPosition = 0;
        mPrimaryItem.setStatView(mStatViews.get(statViewPosition++));

        for (int i = 0; i < mStatItems.size(); i++) {
            StatItem statItem = mStatItems.get(i);
            if (statItem != mPrimaryItem) {
                statItem.displayPosition = statViewPosition;
                statItem.setStatView(mStatViews.get(statViewPosition++));
            }
        }
    }

    /**
     * Closes an open stat column.
     */
    public void closeStats() {
        if (mStatsOpen) {
            for (int i = mStatViews.size() - 1; i >= 1; i--) {
                IStatView statView = mStatViews.get(i);
                statView.setStatVisibility(View.INVISIBLE);
            }

            mStatsOpen = false;
            notifyClosed();
        }
    }

    /**
     * Opens a closed stat column.
     */
    public void openStats() {
        if (!mStatsOpen) {
            for (int i = mStatItems.size() - 1; i >= 1; i--) {
                IStatView statView = mStatViews.get(i);
                statView.setStatVisibility(View.VISIBLE);
            }

            mStatsOpen = true;
            notifyOpened();
        }
    }

    /**
     * Removes a stat item from the column and redraws the column correctly
     *
     * @param item The stat item that will be removed
     */
    public void removeStatItem(StatItem item) {
        boolean removed = mStatItems.remove(item);
        if (removed) {
            sortStatItemList(mStatItems);
            if (item == mPrimaryItem) {
                mPrimaryItem = mStatItems.get(0);
            }

            updateStatItemPositions();
            notifyPrimaryItemChanged();
        } else {
            Log.e(TAG, "The stat item to remove was not part of the list!");
        }
    }

    /**
     * Adds a stat item to the column and redraws the column correctly
     *
     * @param item A stat item that should be added to the view
     */
    public void addStatItem(StatItem item) {
        if (!mStatItems.contains(item)) {
            mStatItems.add(item);
            sortStatItemList(mStatItems);
            updateStatItemPositions();
        }
    }

    /**
     * Returns whether the stat bar is currently open.
     *
     * @return Whether the stat column is currently open.
     */
    public boolean isOpen() {
        return mStatsOpen;
    }

    /**
     * Adds a listener for watching when columns are opened, closed, or stats are changed.
     *
     * @param listener The listener to add
     */
    public void addListener(IStatColumnNotifications listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a listener from the list
     *
     * @param listener The listener to remove
     */
    public void removeListener(IStatColumnNotifications listener) {
        mListeners.remove(listener);
    }

    private void notifyPrimaryItemChanged() {
        for (IStatColumnNotifications listener : mListeners) {
            listener.primaryViewSwitched(mPrimaryItem);
        }
    }

    private void notifyOpened() {
        for (IStatColumnNotifications listener : mListeners) {
            listener.columnOpened();
        }
    }

    private void notifyClosed() {
        for (IStatColumnNotifications listener : mListeners) {
            listener.columnClosed();
        }
    }

    private StatItem mTempStatItem;
    private IStatView mTempStatView;

    /**
     * Shows a stat item over the top of the main item. The temporary stat item will stay as the
     * main item until the method {@link #hideTempStatItem()} is called.
     *
     * @param tempItem The temporary stat item to show in the primary slot
     */
    public void showTempStatItem(StatItem tempItem) {
        mTempStatItem = tempItem;
        mTempStatView = mTempStatItem.getStatView();
        mTempStatItem.setStatView(mStatViews.get(0));
        mPrimaryItem.setHidden(true);
    }

    /**
     * If a temporary stat item is being shown hides the temporary stat item.
     */
    public void hideTempStatItem() {
        if (mTempStatItem == null) return;
        mTempStatItem.setStatView(null);
        mPrimaryItem.setHidden(false);
        mTempStatItem = null;
    }

    public void resetOrdering() {
        StatItem newPrimary = null;
        for (StatItem item : mStatItems) {
            if (newPrimary == null || newPrimary.getOrder() > item.getOrder()) {
                newPrimary = item;
            }
        }

        mPrimaryItem = newPrimary;
        updateStatItemPositions();
    }

    /**
     * Changes the primary item shown to the item from the index parameter
     *
     * @param order The new index to set to
     */
    public void changePrimaryItem(int order) {
        StatItem primaryItem = null;
        for (StatItem item : mStatItems) {
            if (item.getOrder() == order) {
                primaryItem = item;
                break;
            }
        }

        if (primaryItem == null) {
            resetOrdering();
        } else {
            mPrimaryItem = primaryItem;
            updateStatItemPositions();
        }
    }

    public List<IStatView> getStatViews() {
        return mStatViews;
    }

    public List<StatItem> getStatItems() {
        return mStatItems;
    }
}
