package com.jht.chimera.ui.machinesettings;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.widget.UIHelper;

/*
 * A generic dialog box.
 * We can add any view to the dialog box which will show the view in the center of the screen.
 * If you don't want to close the dialog while clicking outside the view,
 * then call the method setCancelable(false);
 * Note: Always set the size of the dialog using setSize method
 */
public class JHTDialog extends RelativeLayout {

    public interface JHTDialogListener {
        public void onOuterAreaTouch(JHTDialog dialog);

        public void onClose(JHTDialog dialog);
    }

    OnTouchListener mTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View arg0, MotionEvent arg1) {
            if (mJHTDialogListener != null)
                mJHTDialogListener.onOuterAreaTouch(JHTDialog.this);

            if (isCancelable) {
                close();
            }

            return true;
        }
    };

    private JHTDialogListener mJHTDialogListener;

    private boolean isShown = false, isCancelable = true;

    ViewGroup parentView = null;

    View contentView = null;

    private int width = -2, height = -2;

    protected Object mSource;

    public JHTDialog(Context context) {
        super(context);

        init();
    }

    public JHTDialog(Context context, int viewLayoutResId) {
        this(LayoutInflater.from(context).inflate(viewLayoutResId, null, false));
    }

    public JHTDialog(View v) {
        super(v.getContext());

        init();

        setView(v);
    }

    public JHTDialog setView(View v) {
        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        ((ViewGroup) findViewById(R.id.dynamicViewPlaceHolder)).addView(
                contentView = v, lp);

        return this;
    }

    public View getView() {
        return contentView;
    }

    public JHTDialog setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    private void init() {
        inflate(getContext(), R.layout.jhtdialog_layout, this);

        this.setOnTouchListener(mTouchListener);
    }

    public JHTDialog setJHTDialogListener(JHTDialogListener l) {
        mJHTDialogListener = l;
        return this;
    }

    public JHTDialog setCancelable(boolean cancelable) {
        isCancelable = cancelable;
        return this;
    }

    public boolean getIsCancelable() {
        return isCancelable;
    }

    public Object getSource() {
        return mSource;
    }

    public static JHTDialog show(Object source, View v) {
        JHTDialog d = new JHTDialog(v);

        return d.showDialog(source);
    }

    public JHTDialog showDialog(Object source) {
        if (width > 0 || height > 0)
            UIHelper.setSize(getView(), width, height);

        UIHelper.showDialog((Activity) getContext(), this);

        this.mSource = source;

        isShown = true;

        onLoadAnimation((Activity) getContext());

        return this;
    }

    public JHTDialog showDialog(Object source, int width, int height) {
        setSize(width, height);

        return showDialog(source);
    }

    public void onLoadAnimation(Activity activity) {

    }

    public void onCloseAnimation(Activity activity) {

    }

    /*
     * Same as hide
     */
    public void close() {
        if (isShown) {
            isShown = false;

            UIHelper.getContentView((Activity) getContext()).removeView(this);

            onCloseAnimation((Activity) getContext());

            if (mJHTDialogListener != null)
                mJHTDialogListener.onClose(this);
        }
    }

    public JHTDialog setInactiveActivityDimColor(int color) {
        this.setBackgroundColor(color);
        return this;
    }
}
