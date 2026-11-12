package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.os.Handler;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.jht.chimera.R;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.chimera.ui.machinesettings.adapters.apps.AppPageAdapter;
import com.jht.chimera.ui.machinesettings.adapters.apps.AppPageFragment;
import com.jht.chimera.ui.machinesettings.adapters.apps.DragData;
import com.jht.chimera.ui.machinesettings.adapters.apps.HiddenAppsAdapter;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;

import java.util.List;


/**
 * The page for determining app ordering on the console workout page.
 */
public class Applications extends SettingPageBase {

    private ViewPager visibleAppsPager;
    private RecyclerView hiddenAppsRecyclerView;
    private Handler mHandler = new Handler();

    public Applications() {}

    @Override
    protected View onCreate(Context context, ViewGroup viewGroup) {
        View view = LayoutInflater.from(context).inflate(R.layout.applications, null);
        viewGroup.addView(view);
        hiddenAppsRecyclerView = view.findViewById(R.id.hiddenAppsRecyclerView);
        hiddenAppsRecyclerView.setHasFixedSize(true);
        hiddenAppsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        visibleAppsPager = view.findViewById(R.id.visibleAppsPager);

        view.findViewById(R.id.touch_event_detector).setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                AppPageFragment frag = (AppPageFragment) ((AppPageAdapter) visibleAppsPager.getAdapter()).getItem(visibleAppsPager.getCurrentItem());
                frag.getNextView().setVisibility(View.INVISIBLE);
                frag.getPreviousView().setVisibility(View.INVISIBLE);
            }

            return true;
        });

        visibleAppsPager.setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                AppPageFragment frag = (AppPageFragment) ((AppPageAdapter) visibleAppsPager.getAdapter()).getItem(visibleAppsPager.getCurrentItem());
                frag.getNextView().setVisibility(View.INVISIBLE);
                frag.getPreviousView().setVisibility(View.INVISIBLE);
            }
            return true;
        });
        return view;
    }


    @Override
    public void show(Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        AppManager.getInstance().reset(); // make sure app list is up to date in case AppStore setting is changed
        AppIconManager.getInstance().reset();
        hiddenAppsRecyclerView.setAdapter(new HiddenAppsAdapter());

        hiddenAppsRecyclerView.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DROP:

                    AppPageFragment frag = (AppPageFragment) ((AppPageAdapter) visibleAppsPager.getAdapter()).getItem(visibleAppsPager.getCurrentItem());
                    frag.getNextView().setVisibility(View.INVISIBLE);
                    frag.getPreviousView().setVisibility(View.INVISIBLE);

                    String appType = event.getClipData().getItemAt(0).getText().toString();
                    if (appType.equals("active")) { //NON-NLS
                        DragData data = (DragData) event.getLocalState();
                        AppIcon app = AppIconManager.getInstance().getAppWithName(data.getId());

                        if (app != null && !isLastApp()) {
                            AppIconManager.getInstance().getHiddenApps().add(app);
                            hiddenAppsRecyclerView.getAdapter().notifyItemInserted(AppIconManager.getInstance().getHiddenApps().size() - 1);
                            int page = visibleAppsPager.getCurrentItem();
                            List<AppIcon> appOrder = AppIconManager.getInstance().getAppOrder();
                            appOrder.remove(app);

                            if ((double) appOrder.size() / (double) AppIconManager.APPS_PER_PAGE <= page) {
                                AppIconManager.getInstance().getAppOrder().remove(page);
                                mHandler.post(() -> visibleAppsPager.setCurrentItem(visibleAppsPager.getCurrentItem() - 1, true));
                            }

                            visibleAppsPager.getAdapter().notifyDataSetChanged();
                            List<AppPageFragment> pages = ((AppPageAdapter) visibleAppsPager.getAdapter()).getPages();
                            for (int i = 0; i < visibleAppsPager.getAdapter().getCount(); i++) {
                                AppPageFragment fragment = pages.get(i);
                                fragment.notifyAdapterOfChanges();
                            }

                            AppIconManager.getInstance().saveAppOrdering();
                        }
                    }
            }
            return true;
        });

        visibleAppsPager.setAdapter(new AppPageAdapter(((FragmentActivity) getContext()).getSupportFragmentManager()));
    }

    private boolean isLastApp() {
        List<AppIcon> appOrder = AppIconManager.getInstance().getAppOrder();
        return appOrder.size() <= 1;
    }
}