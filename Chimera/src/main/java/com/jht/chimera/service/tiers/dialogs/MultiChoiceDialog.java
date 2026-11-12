package com.jht.chimera.service.tiers.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.service.tiers.FeatureUpgrade;
import com.jht.chimera.service.tiers.adapters.UpgradeItemsAdapter;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;

public class MultiChoiceDialog extends DialogBase implements UpgradeItemsAdapter.OnUpgradeListener, View.OnClickListener {
    private static final String TAG = "MultiChoiceDialog";
    private RecyclerView upgradeRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private List<FeatureUpgrade> upgrades;
    private TextView upgradeMessage;
    private TextView dialogTitle;
    private View outerContainer;
    private TextView button0Text;
    private TextView button1Text;
    private ImageButton button0;
    private ImageButton button1;
    private OnClickListener mButton0OnClickListener;
    private OnClickListener mButton1OnClickListener;
    private int mSelected = -1;

    public MultiChoiceDialog(Context context) {
        this(context, null, 0);
    }

    public MultiChoiceDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiChoiceDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        upgradeMessage = findViewById(R.id.upgrade_message);
        dialogTitle = findViewById(R.id.dialog_title);
        outerContainer = findViewById(R.id.outer_container);
        button0Text = findViewById(R.id.text_button0);
        button1Text = findViewById(R.id.text_button1);
        button0 = findViewById(R.id.button0);
        button1 = findViewById(R.id.button1);


        button0.setOnClickListener(this);
        button1.setOnClickListener(this);
        upgradeRecyclerView = findViewById(R.id.upgrade_items);
        upgradeRecyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(context);
        upgradeRecyclerView.setLayoutManager(layoutManager);
        upgradeRecyclerView.addItemDecoration(new DividerItemDecoration(context, LinearLayoutManager.VERTICAL));
        upgradeRecyclerView.addItemDecoration(new DividerItemDecoration(context, LinearLayoutManager.HORIZONTAL));

    }

    @Override
    protected int getLayoutResId() {
        return R.layout.multi_choice_dialog_box;
    }

    public void setTitle(String title) {
        dialogTitle.setText(title);
    }

    public void setCustomBackgroundColor(int color) {
        Log.e(TAG, "OUTERCONTAINER: " + color);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;

        double y = 0.2126 * Math.pow(r/255.0, 2.2)  +  0.7151 * Math.pow(g/255.0, 2.2)  +  0.0721 * Math.pow(b/255.0, 2.2);
        if(y <= .18) {
            //use white
            upgradeMessage.setTextColor(Color.WHITE);
            if(mAdapter != null) {
                ((UpgradeItemsAdapter)mAdapter).setUpgradeItemTextColor(Color.WHITE);
            }


        } else {
            upgradeMessage.setTextColor(Color.BLACK);
            if(mAdapter != null) {
                ((UpgradeItemsAdapter)mAdapter).setUpgradeItemTextColor(Color.BLACK);
            }
            //use black
        }

        if(mAdapter != null)
            mAdapter.notifyDataSetChanged();
        outerContainer.setBackgroundColor(color);
    }

    public void setCustomBackgroundDrawable(BitmapDrawable drawable){
        outerContainer.setBackground(drawable);
    }

    public void setButton0Text(String text) {
        button0Text.setText(text);
    }

    public void setButton1Text(String text) {
        button1Text.setText(text);
    }

    public void setButton0OnClickListener(OnClickListener listener) {
        mButton0OnClickListener = listener;
    }

    public void setButton0BackgroundDrawable(StateListDrawable stateListDrawable) {
        button0.setBackground(stateListDrawable);
    }

    public void setButton1BackgroundDrawable(StateListDrawable stateListDrawable) {
        button1.setBackground(stateListDrawable);
    }

    public void setButton1OnClickListener(OnClickListener listener) {
        mButton1OnClickListener = listener;
    }


    public void addUpgrades(List<FeatureUpgrade> upgrades) {
        this.upgrades = upgrades;
        mAdapter = new UpgradeItemsAdapter(upgrades, this);
        upgradeRecyclerView.setAdapter(mAdapter);
    }

    public int getSelectedChoice() {
        return mSelected;
    }

    @Override
    public void onUpgradeClick(int position) {
        mSelected = position;
        int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
        LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);
        upgradeMessage.setText(upgrades.get(position).getText(languageProperty.locale));
        Log.e(TAG, "callback here it is: " + position + ", " + upgrades.get(position).getTitle(languageProperty.locale));
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.button0) {
            if(mButton0OnClickListener != null) {
                mButton0OnClickListener.onClick(v);
            }
        } else if(v.getId() == R.id.button1) {
            if(mButton1OnClickListener != null) {
                mButton1OnClickListener.onClick(v);
            }
        }
    }
}
