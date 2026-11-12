package com.jht.chimera.lib.widget.statarray;

import android.graphics.Color;
import android.util.Pair;
import android.view.View;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.widget.statbar.IStatColumnNotifications;
import com.jht.chimera.lib.widget.statbar.IStatView;
import com.jht.chimera.lib.widget.statbar.StatColumn;
import com.jht.chimera.lib.widget.statbar.StatItem;
import com.jht.datamatrixlib.DMCallback;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("WeakerAccess")
public class StatManager {
    private StatValueUpdaterCreator mStatUpdater;
    private StatItem mMetricDistanceItem;
    private StatItem mImperialDistanceItem;

    private List<StatColumn> mStatColumns = new ArrayList<>();

    // Shows the goal when the goal is incremented
    private StatItem mGoalTempStatItem;
    private Timer mShowTempStatTimer = null;

    int mMyZoneBgColor = Color.GRAY;

    // Tracks the callbacks that have been added so they can be removed later
    private List<Pair<StatValueUpdater, StatValueUpdater.IStatValueUpdate>> mCallbacksList = new ArrayList<>();

    private DataMatrix mDataMatrix = new DataMatrix();

    private IStatManagerNotifications mColumnCallback;

    public StatManager() {
        mStatUpdater = StatValueUpdaterCreator.getInstance();

        StatValueUpdater goalUpdater = StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.TOTAL_GOAL);
        mGoalTempStatItem = new StatItem(goalUpdater.getDefaultTitle(), goalUpdater.getDefaultValue(), 0, StatValueUpdaterCreator.TOTAL_GOAL);
        goalUpdater.addCallbackInterface(new StatValueUpdater.IStatValueUpdate() {
            @Override
            public void statValueChanged(String value) {
                mGoalTempStatItem.setValue(value);
            }

            @Override
            public void statTitleChanged(String title) {
                mGoalTempStatItem.setTitle(title);
            }

            @Override
            public void statNextValueChanged(String value) {}
        });
    }

    private void setupDataMatrixCallbacks() {
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_INCREASE_WORKOUT_GOAL, keyReturnedBack -> displayGoalTemporarily(), false);

        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_DECREASE_WORKOUT_GOAL, keyReturnedBack -> displayGoalTemporarily(), false);

        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_RESET_STATS, key -> resetStatColumns(), false);

        // Update stat items if the frame type changes
        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, value -> updateStatItems(), true);

        // Update stat items if the unit changes outside the workout
        mDataMatrix.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, this::setSelectedUnit, true);

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_GOAL_TYPE_INT, goalType -> {
            if (goalType == WorkoutKeys.Constants.GOAL_TYPE_NONE) {
                removeGoalStats();
            } else {
                addGoalStats();
            }
        }, false);

        mDataMatrix.createBoolAdapter(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL, new DMCallback<Boolean>() {
            @Override
            public void dataMatrixCallback(Boolean myZoneConnected) {
                if (myZoneConnected) {
                    addMyZoneStats();
                } else {
                    removeMyZoneStats();
                }
            }
        }, false);

        mDataMatrix.createIntAdapter(HALKeys.DATA_MYZONE_EFFORT_INT, effort -> {
            // If MyZone is not connected do not change the background
            if (!DataMatrix.getBoolean(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL)) {
                return;
            }

            int backgroundColor = getMyZoneBackgroundColor(effort);
            int textColor = getMyZoneTextColor(effort);
            int icon = getMyZoneIcon(effort);
            if (backgroundColor != mMyZoneBgColor) {
                mMyZoneBgColor = backgroundColor;
                setMyZoneStatBackgrounds(mMyZoneBgColor, textColor, icon);
            }
        }, false);
    }

    private int getMyZoneTextColor(int hrPercent) {
        int textColor;

        // These colors/zones are off the MyZone website https://www.myzone.org/status-rankings
        if (hrPercent < 50) { // MyZone light gray
            textColor = 0xffed2224; // Same as red color according to MyZone
        } else if (hrPercent < 60) { // MyZone gray
            textColor = Color.WHITE;
        } else if (hrPercent < 70) { // MyZone blue
            textColor = Color.WHITE;
        } else if (hrPercent < 80) { // MyZone green
            textColor = Color.WHITE;
        } else if (hrPercent < 90) { // MyZone yellow
            textColor = Color.BLACK; // Black to increase contrast
        } else { // MyZone red
            textColor = Color.WHITE;
        }

        return textColor;
    }

    private int getMyZoneBackgroundColor(int hrPercent) {
        int bgColor;

        // These colors/zones are off the MyZone website https://www.myzone.org/status-rankings
        if (hrPercent < 50) { // MyZone light gray
            bgColor = 0xffdddddd;
        } else if (hrPercent < 60) { // MyZone gray
            bgColor = 0xff757679;
        } else if (hrPercent < 70) { // MyZone blue
            bgColor = 0xff0031f9;
        } else if (hrPercent < 80) { // MyZone green
            bgColor = 0xff058b44;
        } else if (hrPercent < 90) { // MyZone yellow
            bgColor = 0xfffff200;
        } else { // MyZone red
            bgColor = 0xffed2224;
        }

        return bgColor;
    }

    private int getMyZoneIcon(int hrPercent) {
        int icon;

        // When in the red zone the red icon is washed out, so switch to black
        if (hrPercent < 90) {
            icon = R.drawable.logo_myzone;
        } else { // MyZone red
            icon = R.drawable.logo_myzone_black;
        }

        return icon;
    }

    private void setMyZoneStatBackgrounds(int bgColor, int textColor, int icon) {
        StatItem myZoneHrPercent = getMatchingItem(mStatColumns.get(0).getStatItems(), StatValueUpdaterCreator.MYZONE_HR_PERCENT);
        if (myZoneHrPercent != null) {
            myZoneHrPercent.setStatBackground(bgColor);
            myZoneHrPercent.setStatTextColor(textColor);
            myZoneHrPercent.setIcon(icon);
        }

        StatItem heartRate = getMatchingItem(mStatColumns.get(0).getStatItems(), StatValueUpdaterCreator.HEART_RATE);
        if (heartRate != null) {
            heartRate.setStatBackground(bgColor);
            heartRate.setStatTextColor(textColor);
            heartRate.setIcon(icon);
        }
    }

    private void removeMyZoneStats() {
        StatItem hrItem = getMatchingItem(mStatColumns.get(0).getStatItems(), StatValueUpdaterCreator.HEART_RATE);
        if (hrItem != null) {
            hrItem.setIcon(null);
            hrItem.setStatBackground(null); // reset to standard color
            hrItem.setStatTextColor(null);
        }

        StatItem caloriesItem = getMatchingItem(mStatColumns.get(1).getStatItems(), StatValueUpdaterCreator.CALORIES);
        if (caloriesItem != null) {
            caloriesItem.setIcon(null);
        }

        StatColumn statColumn = mStatColumns.get(0);
        List<StatItem> statItems = new ArrayList<>(statColumn.getStatItems());
        for (StatItem item : statItems) {
            if (item.getStatId() == StatValueUpdaterCreator.MYZONE_HR_PERCENT || item.getStatId() == StatValueUpdaterCreator.MYZONE_MEPS) {
                statColumn.removeStatItem(item);
            }
        }
        removeMyZoneCallbacks();
    }

    private void addMyZoneStats() {
        if (!myZoneStatsShown()) {
            int myZoneEffort = DataMatrix.getInt(HALKeys.DATA_MYZONE_EFFORT_INT);
            int bgColor = getMyZoneBackgroundColor(myZoneEffort);
            int textColor = getMyZoneTextColor(myZoneEffort);
            int icon = getMyZoneIcon(myZoneEffort);
            StatColumn statColumn = mStatColumns.get(0);
            StatValueUpdater myZoneHrPercent = mStatUpdater.getValue(StatValueUpdaterCreator.MYZONE_HR_PERCENT);
            // Add MyZone HR Percent as the first stat in the first category of stats always. -1 is order so it appears as first item
            StatItem hrPercentItem = new StatItem(myZoneHrPercent.getDefaultTitle(), myZoneHrPercent.getCurrentValue(), -1, StatValueUpdaterCreator.MYZONE_HR_PERCENT);
            hrPercentItem.setIcon(icon);
            hrPercentItem.setStatBackground(bgColor);
            hrPercentItem.setStatTextColor(textColor);
            statColumn.addStatItem(hrPercentItem);

            StatValueUpdater myZoneMeps = mStatUpdater.getValue(StatValueUpdaterCreator.MYZONE_MEPS);
            // Meps will be the last item in the first column currently
            StatItem mepsItem = new StatItem(myZoneMeps.getDefaultTitle(), myZoneMeps.getCurrentValue(), 3, StatValueUpdaterCreator.MYZONE_MEPS);
            mepsItem.setIcon(R.drawable.logo_myzone); // Background does not change, so we don't need to adjust the icon for this
            statColumn.addStatItem(mepsItem);

            removeMyZoneCallbacks();
            addCallback(myZoneHrPercent, new StatValueUpdater.IStatValueUpdate() {
                @Override
                public void statValueChanged(String value) {
                    hrPercentItem.setValue(value);
                }
                @Override
                public void statTitleChanged(String title) {
                    hrPercentItem.setTitle(title);
                }
                @Override
                public void statNextValueChanged(String value) {}

            });

            addCallback(myZoneMeps, new StatValueUpdater.IStatValueUpdate() {
                @Override
                public void statValueChanged(String value) {
                    mepsItem.setValue(value);
                }
                @Override
                public void statTitleChanged(String title) {
                    mepsItem.setTitle(title);
                }
                @Override
                public void statNextValueChanged(String value) {}
            });

            StatItem hrItem = getMatchingItem(mStatColumns.get(0).getStatItems(), StatValueUpdaterCreator.HEART_RATE);
            if (hrItem != null) {
                hrItem.setIcon(icon);
                hrItem.setStatBackground(bgColor);
                hrItem.setStatTextColor(textColor);
            }

            StatItem caloriesItem = getMatchingItem(mStatColumns.get(1).getStatItems(), StatValueUpdaterCreator.CALORIES);
            if (caloriesItem != null) {
                caloriesItem.setIcon(R.drawable.logo_myzone);
            }

            // Set the hr percent as the primary item
            mStatColumns.get(0).changePrimaryItem(hrPercentItem.getOrder());
            DataMatrix.putInt(UiServiceKeys.DATA_COL_1_STAT_SELECTED_ORDER_INT, -1); // Set this so it defaults to the effort stat
        }
    }

    private void removeMyZoneCallbacks() {
        for (Pair<StatValueUpdater, StatValueUpdater.IStatValueUpdate> callbackItem : mCallbacksList) {
            int id = callbackItem.first.getId();
            if (id == StatValueUpdaterCreator.MYZONE_HR_PERCENT || id == StatValueUpdaterCreator.MYZONE_MEPS) {
                callbackItem.first.removeCallback(callbackItem.second);
            }
        }
    }

    private void removeGoalStats() {
        StatColumn statColumn = mStatColumns.get(2);
        List<StatItem> statItems = new ArrayList<>(statColumn.getStatItems());
        for (StatItem item : statItems) {
            if (item.getStatId() == StatValueUpdaterCreator.TOTAL_GOAL ||
                    item.getStatId() == StatValueUpdaterCreator.GOAL_REMAINING) {
                statColumn.removeStatItem(item);
            }
        }
        removeGoalCallbacks();
    }

    private void addGoalStats() {
        if (!goalStatsShown()) {
            StatColumn statColumn = mStatColumns.get(2);
            StatValueUpdater goalRemaining = mStatUpdater.getValue(StatValueUpdaterCreator.GOAL_REMAINING);
            StatValueUpdater goal = mStatUpdater.getValue(StatValueUpdaterCreator.TOTAL_GOAL);
            // goal remaining is currently always ordering 1, total goal is 2. Found from index of column where they appear in StatArrays
            StatItem goalRemainingItem = new StatItem(goalRemaining.getDefaultTitle(), goalRemaining.getCurrentValue(), 1, StatValueUpdaterCreator.GOAL_REMAINING);
            StatItem totalGoalItem = new StatItem(goal.getDefaultTitle(), goalRemaining.getCurrentValue(), 2, StatValueUpdaterCreator.GOAL_REMAINING);
            statColumn.addStatItem(goalRemainingItem);
            statColumn.addStatItem(totalGoalItem);

            removeGoalCallbacks();
            addCallback(goalRemaining, new StatValueUpdater.IStatValueUpdate() {
                @Override
                public void statValueChanged(String value) {
                    goalRemainingItem.setValue(value);
                }
                @Override
                public void statTitleChanged(String title) {
                    goalRemainingItem.setTitle(title);
                }
                @Override
                public void statNextValueChanged(String value) {}
            });

            addCallback(goal, new StatValueUpdater.IStatValueUpdate() {
                @Override
                public void statValueChanged(String value) {
                    totalGoalItem.setValue(value);
                }
                @Override
                public void statTitleChanged(String title) {
                    totalGoalItem.setValue(title);
                }
                @Override
                public void statNextValueChanged(String value) {}
            });
        }
    }

    private void removeGoalCallbacks() {
        for (Pair<StatValueUpdater, StatValueUpdater.IStatValueUpdate> callbackItem : mCallbacksList) {
            int id = callbackItem.first.getId();
            if (id == StatValueUpdaterCreator.TOTAL_GOAL || id == StatValueUpdaterCreator.GOAL_REMAINING) {
                callbackItem.first.removeCallback(callbackItem.second);
            }
        }
    }

    private boolean goalStatsShown() {
        // Goal stats are always shown on the second column
        StatColumn statColumn = mStatColumns.get(2);
        for (StatItem item : statColumn.getStatItems()) {
            if (item.getStatId() == StatValueUpdaterCreator.TOTAL_GOAL ||
                    item.getStatId() == StatValueUpdaterCreator.GOAL_REMAINING) {
                return true;
            }
        }

        return false;
    }

    private boolean myZoneStatsShown() {
        StatColumn statColumn = mStatColumns.get(0);
        for (StatItem item : statColumn.getStatItems()) {
            if (item.getStatId() == StatValueUpdaterCreator.MYZONE_HR_PERCENT) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handles the set up of the stat columns. Adds the views to {@link StatColumn} objects and
     * gets the current frame stats to display. It also sets up click events for each of the
     * columns and handles logic to switch the current displayed units when the distance unit
     * is changed. After this is called all stat logic should be complete for the sent stat views.
     * <p>
     * This method expects that the of stat view lists will be at least enough to display all the
     * columns denoted in the stat array.
     *
     * @param statViews The views that the stat items will be displayed on
     */
    public void setupStatColumns(List<List<IStatView>> statViews, IStatManagerNotifications columnCallback) {
        mColumnCallback = columnCallback;
        int[][] statIds = StatArrays.getCurrentFrameArray();
        for (int i = 0; i < statIds.length; i++) {
            int[] columnIds = statIds[i];
            List<? extends IStatView> columnViews = statViews.get(i);
            List<StatItem> statItems = getStatItems(columnIds);
            StatColumn statColumn = new StatColumn(statItems, columnViews);
            mStatColumns.add(statColumn);
        }

        for (int i = 0; i < mStatColumns.size() && i < statViews.size(); i++) {
            StatColumn statColumn = mStatColumns.get(i);
            List<? extends IStatView> statViewColumn = statViews.get(i);
            setupColumnClickEvents(statColumn, statViewColumn);
            ColumnListener listener = new ColumnListener(i);
            statColumn.addListener(listener);
        }

        setupDataMatrixCallbacks();
    }

    public void stopStatColumns() {
        removeCallbacks();
        mDataMatrix.finish();
    }

    /**
     * Gets the stat items for a single column from a column of stat IDs.
     *
     * @param columnIds The IDs of the stats that should be displayed by the items
     * @return The list of stat items
     */
    private List<StatItem> getStatItems(int[] columnIds) {
        List<StatItem> statItems = new ArrayList<>();
        for (int i = 0; i < columnIds.length; i++) {
            // Save the distance items so we can tell when they are selected
            int id = columnIds[i];
            StatValueUpdater statValue = mStatUpdater.getValue(id);
            final StatItem statItem = new StatItem(statValue.getDefaultTitle(), statValue.getDefaultValue(), i, id);

            // If we change the selected distance unit we change displayed units for the console
            if (id == StatValueUpdaterCreator.DISTANCE_METRIC) {
                mMetricDistanceItem = statItem;
            } else if (id == StatValueUpdaterCreator.DISTANCE_IMPERIAL) {
                mImperialDistanceItem = statItem;
            }

            addCallback(statValue, new StatValueUpdater.IStatValueUpdate() {
                @Override
                public void statValueChanged(String value) {
                    statItem.setValue(value);
                }
                @Override
                public void statTitleChanged(String title) {
                    statItem.setTitle(title);
                }
                @Override
                public void statNextValueChanged(String value) {}
            });

            statItems.add(statItem);
        }
        return statItems;
    }

    private void addCallback(StatValueUpdater statValueUpdater, StatValueUpdater.IStatValueUpdate updateInterface) {
        Pair<StatValueUpdater, StatValueUpdater.IStatValueUpdate> callbackPair = new Pair<>(statValueUpdater, updateInterface);
        mCallbacksList.add(callbackPair);
        statValueUpdater.addCallbackInterface(updateInterface);
    }

    private void removeCallbacks() {
        for (Pair<StatValueUpdater, StatValueUpdater.IStatValueUpdate> callbackPair : mCallbacksList) {
            StatValueUpdater statValueUpdater = callbackPair.first;
            StatValueUpdater.IStatValueUpdate callback = callbackPair.second;
            statValueUpdater.removeCallback(callback);
        }

        mCallbacksList.clear();
        mMetricDistanceItem = null;
        mImperialDistanceItem = null;
    }

    private void setupColumnClickEvents(final StatColumn column, List<? extends IStatView> statItems) {
        int i = 0;
        for (IStatView statLayout : statItems) {
            statLayout.setOnClickListener(new StatItemLayoutClickListener(column, i));
            i++;
        }
    }

    private class StatItemLayoutClickListener implements View.OnClickListener {
        private StatColumn mStatColumn;
        private int mIndex;

        StatItemLayoutClickListener(StatColumn column, int statIndex) {
            mStatColumn = column;
            mIndex = statIndex;
        }

        @Override
        public void onClick(View v) {
            mStatColumn.statViewClicked(mIndex);
        }
    }

    /**
     * Gets whether any of the columns are currently open.
     *
     * @return Whether any columns are currently open
     */
    public boolean areStatsOpen() {
        for (StatColumn column : mStatColumns) {
            if (column.isOpen()) return true;
        }

        return false;
    }

    public void closeStats() {
        for (StatColumn column : mStatColumns) {
            column.closeStats();
        }
    }

    /**
     * Resets the stat columns ordering when a workout ends
     */
    public void resetStatColumns() {
        for (StatColumn column : mStatColumns) {
            column.resetOrdering();
        }

        resetDmColumnIndexes();
        setSelectedUnit(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
    }

    private void resetDmColumnIndexes() {
        DataMatrix.putInt(UiServiceKeys.DATA_COL_1_STAT_SELECTED_ORDER_INT, 0);
        DataMatrix.putInt(UiServiceKeys.DATA_COL_2_STAT_SELECTED_ORDER_INT, 0);
        DataMatrix.putInt(UiServiceKeys.DATA_COL_3_STAT_SELECTED_ORDER_INT, 0);
        DataMatrix.putInt(UiServiceKeys.DATA_COL_4_STAT_SELECTED_ORDER_INT, 0);
        DataMatrix.putInt(UiServiceKeys.DATA_COL_5_STAT_SELECTED_ORDER_INT, 0);
    }

    /**
     * Updates the stat items for the current frame without needing to put in the views again.
     */
    public void updateStatItems() {
        FrameConfigManager.getInstance().refreshFrame();
        int[][] statArray = StatArrays.getCurrentFrameArray();
        removeCallbacks();

        for (int i = 0; i < mStatColumns.size() && i < statArray.length; i++) {
            StatColumn column = mStatColumns.get(i);
            List<StatItem> columnStatItems = getStatItems(statArray[i]);
            column.updateAllStatItems(columnStatItems);
        }
    }

    /**
     * Sets the unit that's currently selected on the stat bar
     *
     * @param metric Whether to select the metric or imperial stat
     */
    public void setSelectedUnit(boolean metric) {
        StatColumn distanceColumn = mStatColumns.get(3);
        StatItem distanceItem = distanceColumn.getStatItemAtPosition(0);
        if ((metric && (distanceItem != mMetricDistanceItem)) ||
                (!metric && (distanceItem != mImperialDistanceItem))) {

            for (int i = 0; i < distanceColumn.getStatItems().size(); i++) {
                StatItem item = distanceColumn.getStatItems().get(i);
                if (item != null) {
                    if ((metric && item.getStatId() == StatValueUpdaterCreator.DISTANCE_METRIC) ||
                            (!metric && item.getStatId() == StatValueUpdaterCreator.DISTANCE_IMPERIAL)) {
                        distanceColumn.changePrimaryItem(item.getOrder());
                        DataMatrix.putInt(UiServiceKeys.DATA_COL_4_STAT_SELECTED_ORDER_INT, item.getOrder());
                        break;
                    }
                }
            }
        }
    }

    private void displayGoalTemporarily() {
        final StatColumn goalStatColumn = mStatColumns.get(2);
        goalStatColumn.showTempStatItem(mGoalTempStatItem);
        stopTempStatTimer();
        mShowTempStatTimer = new Timer();
        mShowTempStatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                goalStatColumn.hideTempStatItem();
            }
        }, 3000);
    }

    private void stopTempStatTimer() {
        if (mShowTempStatTimer != null) {
            mShowTempStatTimer.cancel();
            mShowTempStatTimer.purge();
            mShowTempStatTimer = null;
        }
    }

    private class ColumnListener implements IStatColumnNotifications {
        private int mColumnIndex;

        public ColumnListener(int columnIndex) {
            mColumnIndex = columnIndex;
        }

        @Override
        public void columnOpened() {
            if (mColumnCallback != null) {
                mColumnCallback.columnOpened();
            }
        }

        @Override
        public void columnClosed() {
            if (mColumnCallback != null) {
                mColumnCallback.columnClosed();
            }
        }

        @Override
        public void primaryViewSwitched(StatItem newPrimaryItem) {
            setDmColumnOrder(newPrimaryItem.getOrder());

            // Change whether we display metric, only change if needed
            boolean useMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
            int currentRunState = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT);
            if (currentRunState == WorkoutKeys.Constants.RUN_STATE_RUNNING || currentRunState == WorkoutKeys.Constants.RUN_STATE_PAUSE) {
                if (!useMetric && newPrimaryItem.equals(mMetricDistanceItem)) {
                    DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, true);
                } else if (useMetric && newPrimaryItem.equals(mImperialDistanceItem)) {
                    DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, false);
                }
            }
        }

        private void setDmColumnOrder(int order) {
            switch (mColumnIndex) {
                case 0:
                    DataMatrix.putInt(UiServiceKeys.DATA_COL_1_STAT_SELECTED_ORDER_INT, order);
                    break;
                case 1:
                    DataMatrix.putInt(UiServiceKeys.DATA_COL_2_STAT_SELECTED_ORDER_INT, order);
                    break;
                case 2:
                    DataMatrix.putInt(UiServiceKeys.DATA_COL_3_STAT_SELECTED_ORDER_INT, order);
                    break;
                case 3:
                    DataMatrix.putInt(UiServiceKeys.DATA_COL_4_STAT_SELECTED_ORDER_INT, order);
                    break;
                case 4:
                    DataMatrix.putInt(UiServiceKeys.DATA_COL_5_STAT_SELECTED_ORDER_INT, order);
                    break;
            }
        }
    }

    /**
     * Method that sets the stat with the passed in ID to primary displayed stat in the column. May
     * be used in Athena.
     *
     * @param columnIndex The column to set
     * @param id          The ID to set
     */
    private void setDisplayedStat(int columnIndex, int id) {
        StatColumn column = mStatColumns.get(columnIndex);
        StatItem itemToSet = getMatchingItem(column.getStatItems(), id);
        if (itemToSet != null) {
            column.changePrimaryItem(itemToSet.getOrder());
        }
    }

    private StatItem getMatchingItem(List<StatItem> items, int id) {
        for (StatItem item : items) {
            if (item.getStatId() == id) {
                return item;
            }
        }

        return null;
    }

    /**
     * Updates the indexes of the stat columns to be the values from the data matrix
     */
    public void updateColumnIndexes() {
        int index0 = DataMatrix.getInt(UiServiceKeys.DATA_COL_1_STAT_SELECTED_ORDER_INT);
        mStatColumns.get(0).changePrimaryItem(index0);
        int index1 = DataMatrix.getInt(UiServiceKeys.DATA_COL_2_STAT_SELECTED_ORDER_INT);
        mStatColumns.get(1).changePrimaryItem(index1);
        int index2 = DataMatrix.getInt(UiServiceKeys.DATA_COL_3_STAT_SELECTED_ORDER_INT);
        mStatColumns.get(2).changePrimaryItem(index2);
        int index3 = DataMatrix.getInt(UiServiceKeys.DATA_COL_4_STAT_SELECTED_ORDER_INT);
        mStatColumns.get(3).changePrimaryItem(index3);
        int index4 = DataMatrix.getInt(UiServiceKeys.DATA_COL_5_STAT_SELECTED_ORDER_INT);
        mStatColumns.get(4).changePrimaryItem(index4);
    }

    public interface IStatManagerNotifications {
        void columnOpened();
        void columnClosed();
    }

    public IStatView getCaloriesStatView() {
        // calories is always on the 2nd column so skip iterating the other columns
        int numStats = mStatColumns.get(1).getStatViews().size();
        for (int i = 0; i < numStats; i++) {
            if (mStatColumns.get(1).getStatItemAtPosition(i).getStatId() == StatValueUpdaterCreator.CALORIES) {
                return mStatColumns.get(1).getStatItemAtPosition(i).getStatView();
            }
        }

        return null;
    }
}
