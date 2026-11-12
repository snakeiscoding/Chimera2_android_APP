package com.jht.chimera.lib.widget.dialog;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jht.chimera.R;
import com.jht.chimera.lib.widget.ScaleView;
import com.jht.chimera.lib.widget.selectron.SelectronButton;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Method;


public class MessageBoxDialog extends Dialog {
    @NonNls private final String TAG = "MessageBoxDialog";
    private LinearLayout mMessageBoxLayout;
    private LinearLayout mTitleLayout;
    private TextView mTitleTextView;
    private TextView mInfoTextView;
    private SelectronButton mImageButton0;
    private SelectronButton mImageButton1;
    private View.OnClickListener mButton0OnClickListener;
    private View.OnClickListener mButton1OnClickListener;
    private MessageBoxCallback mMessageBoxListener = null;
    private int autoCloseTimeout = 0;
    private int mResourceId = R.layout.message_box_layout;

    public interface MessageBoxCallback {
        void onClose();
    }

    public MessageBoxDialog(Context context, int resourceId) {
        super(context);
        mResourceId = resourceId;
        dispatchOnCreate();
    }

    public MessageBoxDialog(Context context) {
        super(context);
        dispatchOnCreate();
    }

    void dispatchOnCreate() {
        // looks like we need this since we created function to modify the widgets
        // we could just stop the settings in variables and remove this in the future
        try {
            Method method = Dialog.class.getDeclaredMethod("dispatchOnCreate", Bundle.class);
            method.setAccessible(true);
            method.invoke(this, new Object[]{null});
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(mResourceId);
        ScaleView.scaleContents(getWindow().getDecorView().findViewById(android.R.id.content));

        mMessageBoxLayout = findViewById(R.id.messageBoxMainLayout);
        mTitleLayout = findViewById(R.id.title_layout);
        mTitleTextView = findViewById(R.id.messageBoxTitleTextView);
        mInfoTextView = findViewById(R.id.messageBoxInfoTextView);
        Button mCloseButton = findViewById(R.id.messageBoxCloseImageButton);

        mImageButton0 = findViewById(R.id.imageButton0);
        mImageButton1 = findViewById(R.id.imageButton1);

        mImageButton0.setOnClickListener(v -> {
            if (mButton0OnClickListener != null) mButton0OnClickListener.onClick(v);
        });
        mImageButton1.setOnClickListener(v -> {
            if (mButton1OnClickListener != null) mButton1OnClickListener.onClick(v);
        });


        mCloseButton.setOnClickListener(v -> startMessageBoxCloseAnimation());
    }

    @Override
    protected void onStart() {
        super.onStart();

        AlphaAnimation fadeInAnimation = new AlphaAnimation(0, 1);
        fadeInAnimation.setDuration(200);
        mMessageBoxLayout.startAnimation(fadeInAnimation);

        if (autoCloseTimeout > 0) {
            // Auto tear down the dialog in 7 seconds
            Handler handler = new Handler();
            handler.postDelayed(() -> startMessageBoxCloseAnimation(), autoCloseTimeout * 1000);
        }
    }

    public void setMessageBox(String title, String message) {
        mTitleTextView.setText(title);
        mInfoTextView.setText(message);
    }

    public void setColor(int titleColor, int messageColor, int textColor) {
        ColorDrawable titleBackground = new ColorDrawable(titleColor);
        mTitleLayout.setBackground(titleBackground);
        ColorDrawable messageBackground = new ColorDrawable(messageColor);
        mInfoTextView.setBackground(messageBackground);
        mTitleTextView.setTextColor(textColor);
        mInfoTextView.setTextColor(textColor);
    }

    public void setTitle(String title) {
        if(title == null){
            mTitleLayout.setVisibility(View.GONE);
        }
        else{
            mTitleLayout.setVisibility(VISIBLE);
            mTitleTextView.setText(title);
        }
    }

    public void setMessage(String message) {
        mInfoTextView.setText(message);
    }

    public void setMessage(Spannable message) {
        mInfoTextView.setText(message);
    }

    public void closeMessageBox() {
        startMessageBoxCloseAnimation();
    }

    public void setAutoCloseTimeout (int autoCloseTimetou)
    {
        this.autoCloseTimeout = autoCloseTimetou;
    }

    private void startMessageBoxCloseAnimation() {
        if (isShowing()) {
            AlphaAnimation fadeOutAnimation = new AlphaAnimation(1, 0);
            fadeOutAnimation.setDuration(200);
            fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    dismiss();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            if (mMessageBoxListener != null) mMessageBoxListener.onClose();

            mMessageBoxLayout.startAnimation(fadeOutAnimation);
        }

    }

    public void setButtonsEnabled(boolean visible) {
        mImageButton0.setVisibility(visible ? VISIBLE : GONE);
        mImageButton1.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setButton1Enabled(boolean visible) {
        mImageButton0.setVisibility(GONE);
        mImageButton1.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setButtonsText(String button0Text, String button1Text){
        mImageButton0.setText(button0Text);
        mImageButton1.setText(button1Text);
    }

    public void setButton1Text(String text){
        mImageButton1.setText(text);
    }

    public void setButton0OnClickListener(View.OnClickListener listener) {
        mButton0OnClickListener = listener;
    }

    public void setButton1OnClickListener(View.OnClickListener listener) {
        mButton1OnClickListener = listener;
    }

    public void setOnCloseCallback(MessageBoxCallback messageBoxListener) {
        mMessageBoxListener = messageBoxListener;
    }

}