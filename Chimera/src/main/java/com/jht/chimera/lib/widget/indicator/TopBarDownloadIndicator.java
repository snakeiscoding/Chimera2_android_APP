package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarDownloadIndicator extends ImageView {

    DataMatrix mDataMatrix = new DataMatrix();

    public TopBarDownloadIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarDownloadIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.download_arrow));
        hideDownloading();
    }

    void showDownloading() {
        this.setVisibility(View.VISIBLE);
    }

    void hideDownloading() {
        this.setVisibility(View.GONE);
    }


    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            if (visibility == VISIBLE) {
                mDataMatrix.finish();
                mDataMatrix.createBoolAdapter(TopBarKeys.DATA_IS_UPDATE_DOWNLOADING_BOOL, value -> {
                    if (value) {
                        showDownloading();
                    } else {
                        hideDownloading();
                    }
                });

            } else {
                mDataMatrix.finish();
            }
        }
    }

}
