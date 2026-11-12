package com.jht.chimera.ui.machinesettings.viewpager;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.androiduiwidgets.viewpager.IPageHolder;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.datamatrixlib.DataMatrix;


/**
 * Basic functionality for each engineering page.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class SettingPageBase implements IPageHolder {

    // The page.
    private View page;

    // The context for shorthand.
    private Context context;

    // True if the page is shown.
    private boolean show = false;


    /**
     * Create the page.
     */
    public SettingPageBase() {
    }


    /**
     * @return The root view for the page.
     */
    protected View page() {
        return page;
    }

    abstract protected View onCreate(Context context, ViewGroup container);

    protected void onDestroy() {
    }

    @Override
    public void create(ViewGroup container) {
        context = container.getContext();
        page = onCreate(context, container);
        show(null, true);
    }

    @Override
    public void destroy(ViewGroup container) {
        container.removeView(page);
        onDestroy();
    }

    @Override
    public boolean isViewFromObject(View view) {
        return page == view;
    }

    protected int hiddenTranslation() {
        return 50;
    }


    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        show = false;
        if (page != null) {
            
            // hide soft keyboard
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(page.getWindowToken(), 0);

            hideAnimation(page, listener, start);

        }
    }

    public void hideAnimation(final View view, final Animator.AnimatorListener listener, boolean start) {
        final int translationX = hiddenTranslation();
        view.post(new Runnable() {
            @Override
            public void run() {
                view.animate().alpha(0).translationX(translationX).setDuration(350).setListener(new Animator.AnimatorListener() {
                    private void animationFinished() {
                        view.setTranslationX(hiddenTranslation());
                        view.setAlpha(0);
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (listener != null) listener.onAnimationStart(animation);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animationFinished();
                        if (listener != null) listener.onAnimationEnd(animation);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        animationFinished();
                        if (listener != null) listener.onAnimationCancel(animation);

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        if (listener != null) listener.onAnimationRepeat(animation);
                    }
                }).start();
            }
        });

    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {

        show = true;
        if (page != null) {
            showAnimation(page, listener, start);
        }
    }

    public void showAnimation(final View view, final Animator.AnimatorListener listener, boolean start) {

        view.setTranslationX(hiddenTranslation());
        view.post(new Runnable() {
            @Override
            public void run() {
                view.animate().alpha(1).translationX(0).setDuration(350).setListener(new Animator.AnimatorListener() {
                    private void animationFinished() {
                        view.setTranslationX(0);
                        view.setAlpha(1);
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (listener != null) listener.onAnimationStart(animation);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animationFinished();
                        if (listener != null) listener.onAnimationEnd(animation);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        animationFinished();
                        if (listener != null) listener.onAnimationCancel(animation);

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        if (listener != null) listener.onAnimationRepeat(animation);
                    }
                }).start();
            }
        });

    }

    protected Context getContext() {
        return context;
    }

    /* Better to use id.xml resource to avoid missing ids in other flavors
    protected <T extends View> T findViewById(String id) {
        // Both look ups work, reflection seems simpler, can find debate online
        //int res = page.getResources().getIdentifier(id, "id", context.getPackageName());
        int res = CommonRoutines.getResId(id, R.id.class);
        return page.findViewById(res);
    }*/
    protected <T extends View> T findViewById(int id) {
        return page.findViewById(id);
    }

    protected boolean isShown() {
        return show;
    }

    protected void setVisible(View view, int visible) {
        if (view != null) view.setVisibility(visible);
    }

    protected void setText(TextView view, String text) {
        if (view != null) view.setText(text);
    }
    protected void setClick(View view, View.OnClickListener l) {
        if (view != null) view.setOnClickListener(l);
    }

    protected void linkCheckBox(CheckBox checkBox, Object key) {
        linkCheckBox(checkBox, key, false);
    }

    protected void initCheckbox(CheckBox checkbox, Object dataMatrixKey) {
        if (checkbox == null) return;
        checkbox.setChecked(DataMatrix.getBoolean(dataMatrixKey));
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(dataMatrixKey, isChecked));
    }


    protected void linkCheckBox(CheckBox checkBox, Object key, boolean defaultValue) {
        if (checkBox != null) {
            checkBox.setChecked(DataMatrix.getBoolean(key, defaultValue));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(key, isChecked));
        }
    }

    protected void linkUserIntInput(UserTextInput input, Object key) {
        if (input != null) {
            input.setInputProperties(input.getInputProperties().inputValue("" + DataMatrix.getInt(key)));
            input.setOnTextChanged(s -> DataMatrix.putInt(key, CommonRoutines.parseInt(s)));
        }
    }

    protected void linkUserTextInput(UserTextInput input, Object key) {
        if (input != null) {
            input.setInputProperties(input.getInputProperties().inputValue(DataMatrix.getString(key, "")));
            input.setOnTextChanged(s -> DataMatrix.putString(key, s));
        }
    }

    protected void linkOption(OptionSelectorItem item, int options, Object key, OptionSelectorItem.IOnOptionSelected onOptionSelected)
    {
        if (item != null) {
            item.setOptions(options, DataMatrix.getInt(key));
            item.setOnOptionSelected((pos, option) -> {
                DataMatrix.putInt(key, pos);
                if (onOptionSelected != null) {
                    onOptionSelected.onOptionSelected(pos, option);
                }
            });
        }
    }
}
