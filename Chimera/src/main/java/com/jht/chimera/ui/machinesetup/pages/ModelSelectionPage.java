package com.jht.chimera.ui.machinesetup.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.LcbToFramelMapper;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;
import com.jht.chimera.ui.machinesetup.adapters.ModelItem;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

public class ModelSelectionPage extends MachineSetupPageBase {

    private RecyclerView recyclerView;

    @SuppressLint("InflateParams")
    public ModelSelectionPage(final MachineSetupPageData pageData) {
        super(pageData);
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.model_selection, null);
        container.addView(view);

        recyclerView = view.findViewById(R.id.machine_setup_machine_selection);

        recyclerView.setItemAnimator(SelectorDefaultAnimations.getDefaulItemAnimation());
        recyclerView.setLayoutAnimation(SelectorDefaultAnimations.getDefaultLayoutAnimation());
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(manager);

        ((TextView)view.findViewById(R.id.text_lcb_type)).setText(String.format("LCB: 0x%02x",
                DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT)));
        
        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        pageData().previousButton.setVisibility(View.VISIBLE);
        pageData().nextButton.setVisibility(View.VISIBLE);
        pageData().title.setText(R.string.model_setup_tc);

        int lcb = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) ? 0 : DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT);
        List<FrameConfigEnum> models = LcbToFramelMapper.getAvailableModels(pageData().machineTypeSelection, lcb);
        SelectorItemBase machines = new SelectorItemBase(new ArrayList<>());

        for (FrameConfigEnum model : models) {
            machines.addItem(new ModelItem(getContext(), model));
        }

        SelectorAdapterBase machineAdapter = new SelectorAdapterBase(getContext(), machines, item -> {
            pageData().nextButton.setVisibility(View.VISIBLE);
            FrameConfigManager.getInstance().setFrame(((ModelItem) item).machineModel());
            pageData().loadSerialNumbers();
            if (ProductConfig.isLightCommercial()) {
                DialogFactory.message(recyclerView, getContext().getString(R.string.check_power_voltage_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }
        });
        recyclerView.setAdapter(machineAdapter);
        pageData().nextButton.setVisibility(View.GONE);

    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
    }

    @Override
    public boolean onNext() {
        return true;
    }

}
