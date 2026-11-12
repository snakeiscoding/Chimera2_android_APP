package com.jht.chimera.lib.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import com.jht.chimera.R;


public class TwoButtonDialog extends Dialog {
    public final String TAG = "TwoButtonDialog";
    private String mText;
    private TextView mTitle;
    private OnDialogResultListener onDialogResultListener;

    public interface OnDialogResultListener {
        void onRightButtonResult();

        void onLeftButtonResult();
    }

    public void setOnDialogResultListener(OnDialogResultListener listener) {
        this.onDialogResultListener = listener;
    }

    public TwoButtonDialog(Context context) {
        super(context);
    }

    public TwoButtonDialog setTitle(String text) {
        if (mTitle != null) {
            mTitle.setText(text);
        }

        mText = text;

        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_two_button);

        mTitle = findViewById(R.id.titleMessage);


        if (mText != null) {
            setTitle(mText);
        }

        TextView leftButton = findViewById(R.id.leftButton);
        leftButton.setOnClickListener(view -> {
            if (onDialogResultListener != null) {
                onDialogResultListener.onLeftButtonResult();
            } else {
                dismiss();
            }
        });

        TextView rightButton = findViewById(R.id.rightButton);
        rightButton.setOnClickListener(view -> {
            if (onDialogResultListener != null) {
                onDialogResultListener.onRightButtonResult();
            } else {
                dismiss();
            }
        });
    }

}
