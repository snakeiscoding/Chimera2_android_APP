package com.jht.chimera.ui.machinesetup.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.LcbToFramelMapper;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;
import com.jht.chimera.ui.machinesetup.adapters.MachineItem;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

public class MachineSelectionPage extends MachineSetupPageBase {
    public class SpacesItemDecoration extends RecyclerView.ItemDecoration {

        SpacesItemDecoration() {
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = 0;
            if (parent.getAdapter() != null && parent.getChildLayoutPosition(view) % 2 == 0) {
                outRect.right = (int) view.getResources().getDimension(R.dimen.default_recycler_view_spacing);
            }
            else {
                outRect.right = 0;
            }
            outRect.bottom = 0;
            outRect.top = 0;

        }
    }

    private RecyclerView recyclerView;
    private JHTCommonTypes.MachineType[] selectionOrder = {
            JHTCommonTypes.MachineType.Treadmill,
            JHTCommonTypes.MachineType.Ascent,
            JHTCommonTypes.MachineType.LowerBodyAscent,
            JHTCommonTypes.MachineType.Elliptical,
            JHTCommonTypes.MachineType.RecumbentElliptical,
            JHTCommonTypes.MachineType.HybridBike,
            JHTCommonTypes.MachineType.UprightBike,
            JHTCommonTypes.MachineType.RecumbentBike,
            JHTCommonTypes.MachineType.Stepper,
            JHTCommonTypes.MachineType.Climbmill,
            JHTCommonTypes.MachineType.CardioSpinBike,
    };

    @SuppressLint("InflateParams")
    public MachineSelectionPage(final MachineSetupPageData pageData) {
        super(pageData);
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.machine_selection, null);
        container.addView(view);

        recyclerView = view.findViewById(R.id.machine_setup_machine_selection);

        recyclerView.setItemAnimator(SelectorDefaultAnimations.getDefaulItemAnimation());
        recyclerView.setLayoutAnimation(SelectorDefaultAnimations.getDefaultLayoutAnimation());
        GridLayoutManager manager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new SpacesItemDecoration());

        ((TextView)view.findViewById(R.id.text_lcb_type)).setText(String.format("LCB: 0x%02x",
                DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT)));

        return view;

    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        pageData().previousButton.setVisibility(View.VISIBLE);
        pageData().nextButton.setVisibility(View.VISIBLE);
        pageData().title.setText(R.string.machine_setup_tc);
        int lcb = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) ? 0 : DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT);
        List<JHTCommonTypes.MachineType> models = LcbToFramelMapper.getAvailableMachines(lcb);
        final SelectorItemBase machines = new SelectorItemBase(new ArrayList<>());

        List<JHTCommonTypes.MachineType> availableMachinesOrdered = new ArrayList<>();
        for (JHTCommonTypes.MachineType aSelectionOrder : selectionOrder) {
            if (models.contains(aSelectionOrder))
                availableMachinesOrdered.add(aSelectionOrder);
        }
        // Collect types that are not in selection order to the bottom? Or do those need to be hidden?
        for (JHTCommonTypes.MachineType m : models) {
            if (!availableMachinesOrdered.contains(m)) availableMachinesOrdered.add(m);
        }
        int numRows = availableMachinesOrdered.size() / 2 + availableMachinesOrdered.size() % 2;
        for (int i = 0; i < numRows; i++) {
            machines.addItem(new MachineItem(getContext(), availableMachinesOrdered.get(i)));
            int secondIndex = i + numRows;
            if (secondIndex < availableMachinesOrdered.size()) {
                machines.addItem(new MachineItem(getContext(), availableMachinesOrdered.get(secondIndex)));
            }
        }

        SelectorAdapterBase machineAdapter = new SelectorAdapterBase(getContext(), machines, item -> {
            pageData().nextButton.setVisibility(View.VISIBLE);
            pageData().machineTypeSelection = ((MachineItem) item).machineType();
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
