package com.jht.chimera.ui.machinesettings.adapters.apps;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;

import java.util.Collections;
import java.util.List;

/**
 * The fragment for displaying a page of apps.
 */
public class AppPageFragment extends Fragment {
    private static final String TAG = "AppPageFragment";

    private RecyclerView mGridRecyclerView;
    private ImageView mNextPageImageView;
    private ImageView mPreviousPageImageView;
    private int mMovingGridItemPosition = -5;
    private String mMovingChild;
    private boolean mMovingChildIsDoneMoving = true;
    private DragData mDataOnNextPage;
    private DragData mDataOnPreviousPage;
    private Activity mActivity;

    private int mPage;

    private Handler mHandler = new Handler();

    /*Fragments need a default constructor or bad things happen. Check Android docs*/
    public static AppPageFragment newInstance(int page) {
        AppPageFragment fragment = new AppPageFragment();
        fragment.mPage = page;
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void notifyAdapterOfChanges() {
        if (mGridRecyclerView != null) {
            RecyclerView.Adapter adapter = mGridRecyclerView.getAdapter();
            if (adapter != null) adapter.notifyDataSetChanged();
            else Log.e(TAG, "Null adapter, could not notify!");
        }
    }

    // Triggers when holding over left last page
    private JHTTimer mPreviousPageTimer = new JHTTimer(new JHTTimer.IOnTimer() {
        @Override
        public void onTimer() {
            final ViewPager vp = getActivity().findViewById(R.id.visibleAppsPager);

            if (mDataOnPreviousPage.isActive()) {
                List<AppIcon> appOrdering = AppIconManager.getInstance().getAppOrder();
                mDataOnPreviousPage.setPosition(AppIconManager.APPS_PER_PAGE - 1);
                mDataOnPreviousPage.setPage(mDataOnPreviousPage.getPage() - 1);

                AppIcon app = AppIconManager.getInstance().getAppWithName(mDataOnPreviousPage.getId());
                if (app == null) {
                    Log.e(TAG, "Error, no app with name " + mDataOnPreviousPage.getId());
                    return;
                }

                appOrdering.remove(app);
                appOrdering.add(mPage * AppIconManager.APPS_PER_PAGE - 1, app);
                AppIconManager.getInstance().saveAppOrdering();

                mHandler.post(() -> {
                    vp.getAdapter().notifyDataSetChanged();
                    for (AppPageFragment fragment : ((AppPageAdapter) vp.getAdapter()).getPages()) {
                        fragment.notifyAdapterOfChanges();
                    }
                });

            }

            mHandler.post(() -> vp.setCurrentItem(vp.getCurrentItem() - 1, true));
        }
    });

    // Triggers when holding over the right next page
    JHTTimer mNextPageTimer = new JHTTimer(new JHTTimer.IOnTimer() {
        @Override
        public void onTimer() {
            final ViewPager vp = getActivity().findViewById(R.id.visibleAppsPager);
            if (mDataOnNextPage.isActive()) {
                List<AppIcon> appOrdering = AppIconManager.getInstance().getAppOrder();

                mDataOnNextPage.setPosition(0);
                mDataOnNextPage.setPage(mDataOnNextPage.getPage() + 1);

                AppIcon app = AppIconManager.getInstance().getAppWithName(mDataOnNextPage.getId());
                if (app == null) {
                    Log.e(TAG, "Error, no app with name " + mDataOnNextPage.getId());
                    return;
                }

                appOrdering.remove(app);
                appOrdering.add((mPage + 1) * AppIconManager.APPS_PER_PAGE, app);

                AppIconManager.getInstance().saveAppOrdering();

                mHandler.post(() -> {
                    vp.getAdapter().notifyDataSetChanged();
                    List<AppPageFragment> pages = ((AppPageAdapter) vp.getAdapter()).getPages();
                    for (int i = 0; i < vp.getAdapter().getCount(); i++) {
                        AppPageFragment fragment = pages.get(i);
                        fragment.notifyAdapterOfChanges();
                    }
                });
            }

            mHandler.post(() -> vp.setCurrentItem(vp.getCurrentItem() + 1, true));
        }
    });

    public ImageView getPreviousView() {
        return mPreviousPageImageView;
    }

    public ImageView getNextView() {
        return mNextPageImageView;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.application_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup rootView = (ViewGroup) view;
        mNextPageImageView = rootView.findViewById(R.id.nextApplicationPage);
        mPreviousPageImageView = rootView.findViewById(R.id.previousApplicationPage);
        mPreviousPageImageView.setVisibility(View.INVISIBLE);
        mNextPageImageView.setVisibility(View.INVISIBLE);

        mPreviousPageImageView.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundResource(R.drawable.app_page_drag_background_active_left);
                    mDataOnPreviousPage = (DragData) event.getLocalState();
                    mPreviousPageTimer.start(1000);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundResource(R.drawable.app_page_drag_background_left);
                    mPreviousPageTimer.stop();
                    break;
            }
            return true;
        });

        mNextPageImageView.setOnDragListener((v, event) -> {

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundResource(R.drawable.app_page_drag_background_active_right);
                    mDataOnNextPage = (DragData) event.getLocalState();
                    mNextPageTimer.start(1000);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundResource(R.drawable.app_page_drag_background_right);
                    mNextPageTimer.stop();
                    break;
            }
            return true;
        });


        mGridRecyclerView = rootView.findViewById(R.id.applicationGrid);
        mGridRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), ProductConfig.getConsoleType().appGridColCount));
        final GridAdapter gridAdapter = new GridAdapter(mPage, rootView);
        mGridRecyclerView.setAdapter(gridAdapter);

        mGridRecyclerView.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public void onMoveFinished(RecyclerView.ViewHolder item) {
                if (mMovingChild != null) {
                    String child = ((TextView) item.itemView.findViewById(R.id.active_app_item_title)).getText().toString();
                    if (mMovingChild.equals(child)) {
                        mMovingChildIsDoneMoving = true;
                    }
                }
                super.onMoveFinished(item);
            }

            @Override
            public void onAddFinished(RecyclerView.ViewHolder item) {
                super.onAddFinished(item);
            }
        });

        mGridRecyclerView.setOnDragListener((v, event) -> {

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_LOCATION:
                    if (mMovingChildIsDoneMoving) {
                        View child = mGridRecyclerView.findChildViewUnder(event.getX(), event.getY());
                        List<AppIcon> hiddenApps = AppIconManager.getInstance().getHiddenApps();
                        int gridCurrentPosition = mGridRecyclerView.getChildAdapterPosition(child);
                        if (mMovingGridItemPosition == -5) {
                            if (!((DragData) event.getLocalState()).isActive() && gridCurrentPosition != -1) {
                                DragData data = (DragData) event.getLocalState();
                                mMovingGridItemPosition = gridCurrentPosition;

                                AppIcon app = AppIconManager.getInstance().getAppWithName(data.getId());
                                hiddenApps.remove(app);
                                List<AppIcon> appOrder = AppIconManager.getInstance().getAppOrder();
                                List<AppIcon> appPage = AppIconManager.getInstance().getAppsOnPage(mPage);
                                appOrder.add(mPage * AppIconManager.APPS_PER_PAGE + mMovingGridItemPosition, app);

                                ViewPager vp = mActivity.findViewById(R.id.visibleAppsPager);
                                vp.getAdapter().notifyDataSetChanged();
                                for (AppPageFragment fragment : ((AppPageAdapter) vp.getAdapter()).getPages()) {
                                    fragment.notifyAdapterOfChanges();
                                }

                                mMovingChild = ((TextView) data.getView().findViewById(R.id.app_item_title)).getText().toString();
                                RecyclerView hiddenAppsPager = mActivity.findViewById(R.id.hiddenAppsRecyclerView);
                                hiddenAppsPager.getAdapter().notifyItemRemoved(data.getHolderLayoutPosition());

                                data.setPosition(mMovingGridItemPosition);
                                data.setPage(mPage);

                            } else if (((DragData) event.getLocalState()).isActive() && gridCurrentPosition != -1) {
                                DragData dragData = (DragData) event.getLocalState();
                                int previousPage = dragData.getPage();
                                ViewPager vp = mActivity.findViewById(R.id.visibleAppsPager);
                                if (vp.getCurrentItem() == previousPage) {
                                    if (gridCurrentPosition != ((DragData) event.getLocalState()).getHolderLayoutPosition()) {
                                        if (mGridRecyclerView != null) {
                                            RecyclerView.ViewHolder holder = mGridRecyclerView.findViewHolderForAdapterPosition(dragData.getHolderLayoutPosition());
                                            if (holder != null) {
                                                TextView appTitle = holder.itemView.findViewById(R.id.active_app_item_title);
                                                mMovingChild = appTitle.getText().toString();
                                                mMovingGridItemPosition = gridCurrentPosition;
                                                mMovingChildIsDoneMoving = false;
                                                gridAdapter.notifyItemMoved(((DragData) event.getLocalState()).getHolderLayoutPosition(), mMovingGridItemPosition);
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (gridCurrentPosition != -1) {
                                if (mMovingGridItemPosition != gridCurrentPosition) {
                                    int oldPosition = mMovingGridItemPosition;
                                    mMovingGridItemPosition = gridCurrentPosition;
                                    mMovingChildIsDoneMoving = false;
                                    gridAdapter.notifyItemMoved(oldPosition, gridCurrentPosition);
                                }
                            }
                        }
                    }

                    break;
                case DragEvent.ACTION_DROP:
                    // Handle dropping the app onto the page
                    mPreviousPageImageView.setVisibility(View.INVISIBLE);
                    mNextPageImageView.setVisibility(View.INVISIBLE);

                    List<AppIcon> appPage = AppIconManager.getInstance().getAppsOnPage(mPage);
                    if (mMovingGridItemPosition != -5) {
                        DragData data = (DragData) event.getLocalState();
                        int indexOnPage = data.getHolderLayoutPosition();
                        int endPosition = mMovingGridItemPosition;
                        if (indexOnPage < endPosition) {
                            for (int i = indexOnPage; i < endPosition; i++) {
                                Collections.swap(appPage, i, i + 1);
                            }
                        } else {
                            for (int i = indexOnPage; i > endPosition; i--) {
                                Collections.swap(appPage, i, i - 1);
                            }
                        }

                        AppIconManager.getInstance().saveAppOrdering();

                        mMovingGridItemPosition = -5;
                        mMovingChild = null;
                    } else if (appPage.size() == 0) {
                        DragData data = (DragData) event.getLocalState();
                        List<AppIcon> hiddenApps = AppIconManager.getInstance().getHiddenApps();
                        AppIcon app = AppIconManager.getInstance().getAppWithName(data.getId());
                        appPage.add(app);
                        hiddenApps.remove(app);
                        ViewPager vp = mActivity.findViewById(R.id.visibleAppsPager);
                        vp.getAdapter().notifyDataSetChanged();
                        for (AppPageFragment fragment : ((AppPageAdapter) vp.getAdapter()).getPages()) {
                            fragment.notifyAdapterOfChanges();
                        }
                        RecyclerView hiddenAppsPager = mActivity.findViewById(R.id.hiddenAppsRecyclerView);
                        hiddenAppsPager.getAdapter().notifyItemRemoved(data.getHolderLayoutPosition());
                        data.setPosition(0);
                        data.setPage(mPage);
                        AppIconManager.getInstance().saveAppOrdering();
                    } else {

                        DragData data = (DragData) event.getLocalState();
                        if(!data.isActive()){
                            List<AppIcon> hiddenApps = AppIconManager.getInstance().getHiddenApps();
                            AppIcon app = AppIconManager.getInstance().getAppWithName(data.getId());
                            hiddenApps.remove(app);
                            List<AppIcon> appOrder = AppIconManager.getInstance().getAppOrder();
                            appOrder.add( app);

                            ViewPager vp = mActivity.findViewById(R.id.visibleAppsPager);
                            vp.getAdapter().notifyDataSetChanged();
                            for (AppPageFragment fragment : ((AppPageAdapter) vp.getAdapter()).getPages()) {
                                fragment.notifyAdapterOfChanges();
                            }

                            RecyclerView hiddenAppsPager = mActivity.findViewById(R.id.hiddenAppsRecyclerView);
                            hiddenAppsPager.getAdapter().notifyItemRemoved(data.getHolderLayoutPosition());
                            AppIconManager.getInstance().saveAppOrdering();
                        }

                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    mPreviousPageImageView.setVisibility(View.INVISIBLE);
                    mNextPageImageView.setVisibility(View.INVISIBLE);

                    ViewPager vp = mActivity.findViewById(R.id.visibleAppsPager);
                    vp.getAdapter().notifyDataSetChanged();
                    for (AppPageFragment fragment : ((AppPageAdapter) vp.getAdapter()).getPages()) {
                        fragment.notifyAdapterOfChanges();
                    }
                    mMovingGridItemPosition = -5;
                    mMovingChild = null;
                    mMovingChildIsDoneMoving = true;
                    break;
            }
            return true;
        });

    }
}
