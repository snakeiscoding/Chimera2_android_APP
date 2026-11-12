package com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.datamatrixlib.DataMatrix;

/**
 * A widget for displaying information on future stat values in a workout
 */
public class WorkoutInfoBox extends ConstraintLayout {
    private TextView levelTextView;
    private StatViews stat1Views;
    private StatViews stat2Views;
    private View statDividerView;

    public WorkoutInfoBox(@NonNull Context context) {
        super(context);
    }

    public WorkoutInfoBox(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WorkoutInfoBox(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.workout_info_box, this);

        levelTextView = findViewById(R.id.workout_info_level);

        stat1Views = new StatViews(findViewById(R.id.stat1_title),
                findViewById(R.id.stat1_average_label),
                findViewById(R.id.stat1_max_label),
                findViewById(R.id.stat1_average),
                findViewById(R.id.stat1_max));

        stat2Views = new StatViews(findViewById(R.id.stat2_title),
                findViewById(R.id.stat2_average_label),
                findViewById(R.id.stat2_max_label),
                findViewById(R.id.stat2_average),
                findViewById(R.id.stat2_max));

        statDividerView = findViewById(R.id.stat_divider);
        stat1Views.setNumberAlignment();
        stat2Views.setNumberAlignment();
    }

    private static class StatViews {
        private final TextView statTitleTextView;
        private final TextView averageLabelTextView;
        private final TextView maxLabelTextView;
        private final TextView averageTextView;
        private final TextView maxTextView;

        public StatViews(TextView statTitleTextView, TextView averageLabelTextView,
                  TextView maxLabelTextView, TextView averageTextView, TextView maxTextView) {
            this.statTitleTextView = statTitleTextView;
            this.averageLabelTextView = averageLabelTextView;
            this.maxLabelTextView = maxLabelTextView;
            this.averageTextView = averageTextView;
            this.maxTextView = maxTextView;
        }

        public void setStat(WorkoutStatInfo statInfo) {
            statTitleTextView.setText(statInfo.getName());
            averageTextView.setText(statInfo.getAverage());
            maxTextView.setText(statInfo.getMax());
        }

        public void setStatVisibility(int visibility) {
            statTitleTextView.setVisibility(visibility);
            averageLabelTextView.setVisibility(visibility);
            maxLabelTextView.setVisibility(visibility);
            averageTextView.setVisibility(visibility);
            maxTextView.setVisibility(visibility);
        }

        public void setNumberAlignment(){
            int languageId = DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT);
            if (languageId == LanguageProperty.Arabic.ordinal()){
                averageTextView.setGravity(Gravity.END);
                maxTextView.setGravity(Gravity.END);
            } else {
                averageTextView.setGravity(Gravity.START);
                maxTextView.setGravity(Gravity.START);
            }
            maxTextView.setTextAlignment(TEXT_ALIGNMENT_GRAVITY);
            averageTextView.setTextAlignment(TEXT_ALIGNMENT_GRAVITY);
        }
    }



    /**
     * Sets the display to show the information for one stat
     * @param level The level to display at the top
     * @param stat The information for the stat
     */
    public void setStatInfo(String level, @NonNull WorkoutStatInfo stat) {
        setStatInfo(level, stat, null);
    }

    /**
     * Sets the display to show the information for two stats
     * @param level The level to display
     * @param stat1 The information for the first stat
     * @param stat2 The information for the second stat
     */
    public void setStatInfo(String level, @NonNull WorkoutStatInfo stat1, @Nullable WorkoutStatInfo stat2) {
        levelTextView.setText(level);
        stat1Views.setStat(stat1);

        if (stat2 == null) {
            stat2Views.setStatVisibility(View.GONE);
            statDividerView.setVisibility(View.GONE);
        } else {
            stat2Views.setStatVisibility(View.VISIBLE);
            statDividerView.setVisibility(View.VISIBLE);
            stat2Views.setStat(stat2);
        }
    }
}
