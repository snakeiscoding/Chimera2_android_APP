package com.jht.chimera.ui.mainapp.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.widget.Toast;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.datamatrixlib.DataMatrix;

public class UpdateDialog extends Dialog {

    public UpdateDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ALERT);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(R.layout.update_dialog);

        findViewById(R.id.cornerCloseButton).setOnClickListener(v -> dismiss());
        findViewById(R.id.install_later).setOnClickListener(v -> {
            DataMatrix.putBoolean(MainAppKeys.DATA_INSTALL_AFTER_WORKOUT_BOOL, false);
            dismiss();
        });
        findViewById(R.id.install).setOnClickListener(v -> {
            DataMatrix.putBoolean(MainAppKeys.DATA_INSTALL_AFTER_WORKOUT_BOOL, false);
            DataMatrix.putBoolean(MainAppKeys.DATA_IS_SAFE_TO_UPDATE_BOOL, true);

            boolean isPersistent = Settings.Secure.getInt(getContext().getContentResolver(), "jht_disable_persistent", 0) == 0; //NON-NLS
            if (!isPersistent) Toast.makeText(getContext(), "Warning: Persistent process is off can lead to software update failures", Toast.LENGTH_LONG).show(); //NON-NLS
        });
        findViewById(R.id.install_after_workout).setOnClickListener(v -> {
            DataMatrix.putBoolean(MainAppKeys.DATA_INSTALL_AFTER_WORKOUT_BOOL, true);
            dismiss();
        });

    }
}
