package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.ui.mainapp.dialog.UpdateDialog;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarUpdateIndicator extends ImageView {

    DataMatrix mDataMatrix = new DataMatrix();
    private UpdateDialog dialog = new UpdateDialog(getContext());

    public TopBarUpdateIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarUpdateIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_software_update));
        setOnClickListener(v -> {
            dialog.show();
        });
        hideIcon();
    }

    void updateIcon() {
        if (DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL) &&
                DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP) {
            this.setVisibility(View.VISIBLE);
        } else {
            hideIcon();
        }
    }

    void hideIcon() {
        this.setVisibility(View.GONE);
    }


    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            mDataMatrix.finish();

            // update the icon if the update is ready
            mDataMatrix.createBoolAdapter(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL, value -> {
                if (value) updateIcon();
                else hideIcon();
            });

            // hide the update icon in the workout
            mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, runState -> {
                if (mDataMatrix.getBoolean(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL)) {
                    updateIcon();
                }
            });
        } else {
            mDataMatrix.finish();
        }
    }

}
