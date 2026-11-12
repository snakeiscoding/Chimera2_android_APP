package com.jht.chimera.ui.mainapp.goscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.workoutapps.AppIcon;

import java.util.List;


public class SetupAppPageFragment extends Fragment {
    private List<AppIcon> appIconList;
    private SetupAppAdapter appAdapter;
    private GridView applicationGrid;
    private List<AppIcon> apps;

    private View mView;
    SetupAppAdapter.OnIconClickListener onIconClickListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.app_icon_page, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // workaround for current app_icon_page.xml having fixed height to address a different layout issue, see cb4742f926395330119dad75279a7e0b4103c214
        View appGrid = view.findViewById(R.id.appGrid);
        appGrid.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

        updateIcons();
    }

    /**
     * Sets the page number of this fragment
     *
     * @param apps the apps for this page
     */
    public void setApps(List<AppIcon> apps) {
        this.apps = apps;
    }

    /**
     * Updates the icons for this page
     */
    public void updateIcons() {
        if (mView != null) {
            appIconList = apps;
            applicationGrid = mView.findViewById(R.id.appGrid);
            applicationGrid.setNumColumns(ProductConfig.getConsoleType().appGridColCount);
            appAdapter = new SetupAppAdapter(getActivity(), R.layout.app_icon, R.id.appIconText, appIconList);
            applicationGrid.setAdapter(appAdapter);
            appAdapter.setOnIconClickListener(onIconClickListener);
        }
    }

    void setOnIconClickListener(SetupAppAdapter.OnIconClickListener onIconClickListener) {
        this.onIconClickListener = onIconClickListener;
    }
}
