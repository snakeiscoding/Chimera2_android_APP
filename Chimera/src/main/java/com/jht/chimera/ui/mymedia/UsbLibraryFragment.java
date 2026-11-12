package com.jht.chimera.ui.mymedia;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NotNull;

/**
 * A simple {@link Fragment} subclass.
 */
public class UsbLibraryFragment extends Fragment {
    private static final String TAG = "UsbLibraryFragment";

    private FragmentActivity mActivity;
    private AudioPlayerController mAudioController;
    private VideoPlayerController mVideoController;
    private OnMenuClickListener mOnMenuClickListener;
    private boolean mHasVideos;

    private ImageButton mAudioTab;
    private ImageButton mVideoTab;
    private FragmentManager mFragmentMgr;
    private LinearLayout mTitleLayout;
    private ImageView mBusyIndicator;
    private final DataMatrix mDataMatrix = new DataMatrix();


    public UsbLibraryFragment() {

    }


    public interface OnMenuClickListener {
        void onMenuClickListener();
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        mActivity = (FragmentActivity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!DataMatrix.getBoolean(HALKeys.DATA_IS_USING_BLE_BROWSING_BOOL))
            mHasVideos = DataMatrix.getBoolean(UsbMediaDeviceKeys.DATA_HAS_VIDEO_BOOL);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.usb_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFragmentMgr = mActivity.getSupportFragmentManager();
        mTitleLayout = view.findViewById(R.id.ll_usb_library_title);
        ImageButton mIbShowMenuPage = view.findViewById(R.id.ib_show_menu_page);
        mIbShowMenuPage.setOnClickListener(view1 -> mOnMenuClickListener.onMenuClickListener());

        TextView mTvDeviceName = view.findViewById(R.id.tv_device_name);
        if(DataMatrix.getBoolean(HALKeys.DATA_IS_USING_BLE_BROWSING_BOOL)) {
            mTvDeviceName.setText(DataMatrix.getString(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING));
        } else {
            mTvDeviceName.setText(DataMatrix.getString(UsbMediaDeviceKeys.DATA_DEVICE_NAME_STRING)); //NON-NLS
        }


        mAudioTab = view.findViewById(R.id.ib_audio_tab);
        mAudioTab.setOnClickListener(view12 -> {
            mAudioTab.setSelected(true);
            if(mHasVideos) {
                mVideoTab.setSelected(false);
                mVideoController.hide();
            }
            mAudioController.show();

        });

        mAudioController = (AudioPlayerController) getChildFragmentManager().findFragmentById(R.id.fl_audio_player_controller);


        mVideoTab = view.findViewById(R.id.ib_video_tab);
        mVideoTab.setOnClickListener(v -> {
            mVideoTab.setSelected(true);
            mAudioTab.setSelected(false);
            mAudioController.hide();
            mVideoController.show();
        });

        mVideoController = (VideoPlayerController) getChildFragmentManager().findFragmentById(R.id.fl_video_player_controller);
        if(mVideoController != null) {
            mVideoController.setOnBackListener(() -> {
                mTitleLayout.setVisibility(View.VISIBLE);
                mVideoController.showDefault();
            });
            mVideoController.setOnScreenSizeChangeListener(() -> mTitleLayout.setVisibility(View.GONE));
        }

        if(!mHasVideos) {
            mVideoTab.setVisibility(View.GONE);
            mVideoController.hide();
        }

        mAudioTab.performClick();
        mBusyIndicator = view.findViewById(R.id.my_media_busy_indicator);
        mBusyIndicator.setVisibility(View.INVISIBLE);

        initDataMatrix();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: ");
        super.onDestroyView();
        Fragment audioTab = mFragmentMgr.findFragmentByTag("audio"); //NON-NLS
        if(audioTab != null && audioTab.isAdded()) {
            mFragmentMgr.beginTransaction().remove(audioTab).commitAllowingStateLoss();
        }

        Fragment videoTab = mFragmentMgr.findFragmentByTag("video"); //NON-NLS
        if(videoTab != null && videoTab.isAdded()) {
            mFragmentMgr.beginTransaction().remove(videoTab).commitAllowingStateLoss();
        }
    }

    void setOnMenuClickListener(OnMenuClickListener l) {
        mOnMenuClickListener = l;
    }
    private void initDataMatrix() {
        mDataMatrix.createBoolAdapter(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, isScanning -> {
            if (isScanning) {
                mBusyIndicator.setVisibility(View.VISIBLE);
                mBusyIndicator.startAnimation(AnimationUtils.loadAnimation(mActivity, R.anim.spinning_clockwise));
            } else {
                mBusyIndicator.setVisibility(View.INVISIBLE);
                mBusyIndicator.clearAnimation();
            }
        });

    }

    public void showAudioPlayerView(boolean show) {
        mAudioController.showAudioPlayerView(show);
    }
}
