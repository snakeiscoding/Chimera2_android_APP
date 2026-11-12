package com.jht.chimera.ui.mymedia;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.chimera.ui.mymedia.adapters.MediaItemListAdapter;

import java.util.List;
import java.util.Stack;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MediaBrowser#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MediaBrowser extends Fragment {
    private static final String TAG = "MediaBrowser";
    private static MediaBrowser instance;

    private Context mContext;
    private View mView;
    private Stack<String> mCategoryStack;
    private String mLastCategory;
    private LinearLayout llBrowseUp;
    private TextView tvCategory;
    private MediaItemListAdapter mAdapter;
    private OnBrowseListener mOnBrowseListener;
    private RecyclerView mMediaItemList;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public interface OnBrowseListener {
        void onItemClick(MediaItem item, int position);
        void onBrowseUp();
    }

    public static MediaBrowser newInstance() {
        if(instance == null) {
            instance = new MediaBrowser();
        }

        return instance;
    }

    public MediaBrowser() {
        // Required empty public constructor
    }

    public MediaItemListAdapter getmAdapter() {
        return mAdapter;
    }


    public void setOnBrowseListener(OnBrowseListener l) {
        mOnBrowseListener = l;
    }

    public void setBrowseUpButton(String name) {
        llBrowseUp.setVisibility(View.VISIBLE);
        tvCategory.setText(name);

        if(mCategoryStack.isEmpty()) {
            mCategoryStack.push("TopLevel"); //NON-NLS
        } else {
            mCategoryStack.push(mLastCategory);
        }

        mLastCategory = name;
    }

    public void update(MediaItem item) {
//        mAdapter.add(item);
    }

    public void update(List<MediaItem> items) {
//        mAdapter.add(items);
        mAdapter.submitList(items);
//        mAdapter.update(items);
    }

    public void add(MediaItem item) {
        mAdapter.add(item);
    }

    public void addList(List<MediaItem> list){
        mAdapter.addList(list);
    }

    public void clear() {
        mAdapter.clear();
    }

    public RecyclerView getmMediaItemList() {
        return mMediaItemList;
    }


    public void setCurrentTrack(MediaItem item) {
        mAdapter.setCurrentTrack(item);

    }

    public void updateMediaTitle(MediaItem item, String song){
        mAdapter.updateMediaItemTitle(item, song);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCategoryStack = new Stack<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.media_browser, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMediaItemList = view.findViewById(R.id.media_item_list);
        ((SimpleItemAnimator)mMediaItemList.getItemAnimator()).setSupportsChangeAnimations(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        mMediaItemList.setLayoutManager(layoutManager);
        mAdapter = new MediaItemListAdapter(mContext);
        mAdapter.setOnItemClickListener((v, position) -> {
            MediaItem item = mAdapter.getMediaItem(position);
            mOnBrowseListener.onItemClick(item, position);
        });
        mMediaItemList.setAdapter(mAdapter);

        llBrowseUp = view.findViewById(R.id.ll_browse_up);
        llBrowseUp.setVisibility(View.GONE);

        tvCategory = view.findViewById(R.id.tv_category);

        ImageButton ibBrowseUp = view.findViewById(R.id.ib_browse_up);
        llBrowseUp.setOnClickListener(v -> {
            mLastCategory = mCategoryStack.pop();
            if(mLastCategory.equals("TopLevel")) { //NON-NLS
                llBrowseUp.setVisibility(View.GONE);
            } else {
                llBrowseUp.setVisibility(View.VISIBLE);
                tvCategory.setText(mLastCategory);
            }
            mOnBrowseListener.onBrowseUp();
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    public void hide() {
        mView.setVisibility(View.GONE);
    }

    public void show() {
        mView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
