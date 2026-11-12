package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.ui.machinesettings.adapters.keypress.KeyPressSelectorItem;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;


/**
 * Implements the key test page gluing the page to the model layer.
 */
public class KeyTest extends SettingPageBase {

    DataMatrix dataMatrix = new DataMatrix();
    FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
    SelectorAdapterBase adapter;
    RecyclerView table;
    SelectorItemBase base;

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        View view = LayoutInflater.from(context).inflate(R.layout.key_test, null);
        container.addView(view);
        table = view.findViewById(R.id.eng_key_test_table);
        SelectorDefaultAnimations.setDefaultAnimations(context, table);
        return view;
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        base = new SelectorItemBase(new ArrayList<>());
        adapter = new SelectorAdapterBase(getContext(), base, null);
        table.setAdapter(adapter);

        dataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, integer -> {
            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(integer);
            KeyPress.KeyState keyState = KeyPress.getKeyState(integer);
            base.addItem(new KeyPressSelectorItem(keyCode, keyState));
            if (adapter != null) {
                adapter.notifyItemInserted(base.numChildren() - 1);
            }
        });
    }


    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        table.setAdapter(null);
        base.removeAllChildren();
        table.setAdapter(null);
        dataMatrix.finish();
    }


}
