package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarUserIndicator extends TextView {

    DataMatrix mDataMatrix = new DataMatrix();

    public TopBarUserIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarUserIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setUsername(getResources().getString(R.string.guest_tc));
    }

    void setUsername(String username) {
        this.setText(username);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            mDataMatrix.finish();
            mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_FIRST_NAME_STRING, value -> setUsername(value));

        } else {
            mDataMatrix.finish();
        }
    }
}
