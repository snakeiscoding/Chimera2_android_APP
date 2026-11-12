package com.jht.chimera.ui.mainapp.goscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.widget.indicator.PageIndicator;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.WorkoutSetupFragment;

import java.util.ArrayList;
import java.util.List;


public class AppsFragment extends Fragment {
    ViewPager mAppPager;
    AppPagerAdapter mAdapter;
    PageIndicator mPageIndicator;

    ParkerGoScreenModule.IGoScreenInterface iGoScreenInterface;
    WorkoutSetupFragment workoutSetupFragment = new WorkoutSetupFragment();
    AppIcon appIcon;

    public AppsFragment(ParkerGoScreenModule.IGoScreenInterface iGoScreenInterface) {
        this.iGoScreenInterface = iGoScreenInterface;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.apps_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPageIndicator = view.findViewById(R.id.pageIndicator);

        mAppPager = view.findViewById(R.id.appViewPager);
        mAdapter = new AppPagerAdapter(getChildFragmentManager(), mPageIndicator);

        mAppPager.setAdapter(mAdapter);

        mAppPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mPageIndicator.setCurrentPage(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }


    public class AppPagerAdapter extends FragmentStatePagerAdapter {
        List<SetupAppPageFragment> pages = new ArrayList<>();
        PageIndicator pageIndicator;

        public AppPagerAdapter(FragmentManager fm, PageIndicator pageIndicator) {
            super(fm);
            // getVisibleApps() can return a different list if view is changing, so do this only when constructing to avoid a possible getCount change exception
            List<AppIcon> apps = AppIconManager.getInstance().getEntertainmentApps();
            int count = (int) Math.ceil((double) apps.size() / (double) AppIconManager.APPS_PER_PAGE);
            for (int i = 0; i < count; i++) {
                SetupAppPageFragment appPage = new SetupAppPageFragment();
                appPage.setApps(AppIconManager.getInstance().getAppsForPage(apps, i));
                pages.add(appPage);

                appPage.setOnIconClickListener(onIconClickListener);
            }

            this.pageIndicator = pageIndicator;
            pageIndicator.setNumberOfPages(count);
        }


        @Override
        public Fragment getItem(int position) {
            return pages.get(position);
        }

        @Override
        public int getCount() {
            return pages.size();
        }

    }

    SetupAppAdapter.OnIconClickListener onIconClickListener = new SetupAppAdapter.OnIconClickListener() {
        @Override
        public void onClick(AppIcon appIcon) {
            AppsFragment.this.appIcon = appIcon;

            // TODO - Can be launched from apps tab, calendar, featured tab (but NOT during a workout)
            // TODO - at some point calendar will need to convey a workout id to ifit app?
            if (appIcon != null && appIcon.appInfo.useThirdPartyInterface) {
                LaunchAppUtil.initialLaunchAppByIcon(getContext(), appIcon.appInfo, getActivity().getWindow().getDecorView().findViewById(android.R.id.content));
                return;
            }

            GenericAppSetupFragment genericAppSetupFragment = new GenericAppSetupFragment();
            genericAppSetupFragment.setSetupInterface(new GenericAppSetupFragment.ISetupInterface() {
                @Override
                public void onWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters) {
                    if (appIcon != null) LaunchAppUtil.checkPromptSso(getContext(), appIcon.appInfo, getView(), ()->{
                        iGoScreenInterface.setCurrentFragment(genericAppSetupFragment);
                        iGoScreenInterface.onWorkoutStart(workoutIdentifier, parameters);
                    });
                }

                @Override
                public void onGetParameters(String workoutIdentifier) {
                    iGoScreenInterface.setCurrentFragment(genericAppSetupFragment);
                    if (appIcon != null) iGoScreenInterface.onWorkoutSelected(workoutIdentifier + ":" + appIcon.appInfo.uuid);
                    else iGoScreenInterface.onWorkoutSelected(workoutIdentifier);
                }
            });

            GoScreenWorkoutDisplayItem manualWorkout = new GoScreenWorkoutDisplayItem(appIcon.appName, "", appIcon.appInfo.uuid, "", null, null, "0");
            genericAppSetupFragment.loadWorkoutInfo(manualWorkout);
            workoutSetupFragment.loadNewSetupFragment(genericAppSetupFragment);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                    .replace(android.R.id.content, workoutSetupFragment).commit();

            getActivity().getSupportFragmentManager().executePendingTransactions();
        }
    };

}
