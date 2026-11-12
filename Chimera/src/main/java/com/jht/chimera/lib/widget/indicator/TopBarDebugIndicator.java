package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarDebugIndicator extends LinearLayout {

    DataMatrix mDataMatrix = new DataMatrix();
    Context mContext;

    private AppCompatImageView mDemoModeIndicator;
    private AppCompatTextView mDebugText;

    public TopBarDebugIndicator(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public TopBarDebugIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public void init() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.top_bar_debug_layout, this, true);
        mDemoModeIndicator = findViewById(R.id.demo_mode_indicator);
        mDebugText = findViewById(R.id.debug_options_text_view);
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            updateText();
        }
    }

    void updateText() {
        boolean isDemoMode = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL);
        boolean isPersistent = Settings.Secure.getInt(mContext.getContentResolver(), "jht_disable_persistent", 0) == 0; //NON-NLS

        mDemoModeIndicator.setVisibility(isDemoMode ? View.VISIBLE : View.GONE);

        StringBuilder sb = new StringBuilder();
        if (BuildConfig.DEBUG) {
            sb.append("DEBUG BUILD"); //NON-NLS
            sb.append(isPersistent ? "\nPERS" : "\nNO PERS"); //NON-NLS
        } else {
            if (!isPersistent) sb.append("\nNO PERS"); //NON-NLS
        }
        String text = sb.toString();
        mDebugText.setText(text);
        if (!isDemoMode && text.isEmpty()) {
            setVisibility(GONE);
        } else {
            setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            if (visibility == VISIBLE) {
                mDataMatrix.finish();
                mDataMatrix.createBoolAdapter(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, value -> updateText());

            } else {
                mDataMatrix.finish();
            }
        }
    }
}
