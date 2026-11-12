package com.jht.chimera.ui.mymedia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.BTSinkKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.chimera.lib.common.mediadevice.MediaItemParameter;
import com.jht.chimera.ui.mymedia.adapters.MediaItems;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AudioPlayerController extends MediaPlayerController {
    private static final String TAG = "AudioPlayerController";

    private View mView;
    private AudioPlayerView mAudioPlayer;
    private JHTTypes.REPEAT_MODE mRepeatMode = JHTTypes.REPEAT_MODE.NONE;
    private boolean mIsShuffle;
    private DataMatrix mDataMatrix = new DataMatrix();
    private boolean isSinkDeviceSet = false;
    private PeriodicTimer songPositionRequestTimer;
    private boolean mIsInPlayed;
    private boolean mMenuIsHidden = true;
    private List<MediaItem> mediaItemList;
    private PeriodicTimer firstFolderItemsTimer;

    public HashMap<Integer, List<MediaItem>> cachedFolderLists = new HashMap<>();
    public int cachedFoldersIndex = 1;
    private boolean blockedByBrowsingUp = false;

    private void initDataMatrix() {
        if(!isSinkDeviceSet) {
            UsbMediaDeviceKeys.registerEventMediaItemFound(mDataMatrix, item -> {
                if (item != null) {
                    if (item.id == null) {
                        if (!item.isVideo) {
                            mMediaBrowser.clear();
                        }
                    } else if (!item.isVideo) {
//                        Log.d(TAG, "item name: " + item.title + " " + item.id + " " +  item.isTrack + " " + item.isVideo);
                        mMediaBrowser.add(item);
                    }
                } else {
                    mMediaBrowser.clear();
                }
            });

            mDataMatrix.createIntAdapter(UsbMediaDeviceKeys.DATA_PLAYBACK_POSITION_INT, pos -> mAudioPlayer.updatePosition(pos));

            mDataMatrix.createParcelableAdapter(UsbMediaDeviceKeys.DATA_CURRENT_MEDIA_ITEM_PARCELABLE, MediaItem.class, current -> {
                if (current != null && !current.isVideo) {
                    if (mMenuIsHidden) {
                        mAudioPlayer.show();
                    }
                    Log.e(TAG, "DATA_CURRENT_MEDIA_ITEM_PARCELABLE adapter. title: " + current.title);
                    mMediaBrowser.setCurrentTrack(current);
                    mAudioPlayer.updateCurrentTrack(current);
                }
            });

            mDataMatrix.createBoolAdapter(UsbMediaDeviceKeys.DATA_IS_PLAYING_BOOL, isPlaying -> mAudioPlayer.updatePlayingStatus(isPlaying));

            mDataMatrix.createIntAdapter(UsbMediaDeviceKeys.DATA_REPEAT_MODE_ENUM_INT, value -> {
                if (mRepeatMode != JHTTypes.REPEAT_MODE.values()[value]) {
                    mRepeatMode = JHTTypes.REPEAT_MODE.values()[value];
                    mAudioPlayer.showRepeatButton(mRepeatMode);
                }
            });

            mDataMatrix.createBoolAdapter(UsbMediaDeviceKeys.DATA_IS_SHUFFLE_BOOL, isShuffle -> {
                if (mIsShuffle != isShuffle) {
                    mIsShuffle = isShuffle;
                    mAudioPlayer.showShuffleButton(isShuffle);
                }
            });
            mDataMatrix.createByteArrayAdapter(UsbMediaDeviceKeys.DATA_ALBUM_ART_BYTES, byteArray -> {
                if (byteArray != null) {
                    Bitmap albumArt = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                    mAudioPlayer.updateAlbumCover(albumArt);
                } else {
                    mAudioPlayer.updateAlbumCover(null);
                }
            });

            mDataMatrix.createBoolAdapter(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, isScanning -> Log.d(TAG, "initDataMatrix: is scanning: " + isScanning));
        }
        else {
            mDataMatrix.createParcelableAdapter(BTSinkKeys.ACTION_MEDIA_FOLDER_ITEM_DISCOVER_PARCELABLE, MediaItemParameter.class, value -> {


                if(!blockedByBrowsingUp) {
                    Log.e(TAG, "name: " + value.name + ", uid: " + value.uid + ", attrId: " + value.attrId + ", isPlayable: " + value.isPlayable);

                    MediaItem currentItem = new MediaItem();
                    boolean isAlreadyInList = false;
                    for (MediaItem item : mediaItemList) {
                        if (item.id.equals(value.uid)) {
                            Log.e(TAG, "already in list");
                            currentItem = item;
                            isAlreadyInList = true;
                        }
                    }

                    switch (value.attrId) {
                        case 1:
                            currentItem.title = value.name;
                            break;
                        case 2:
                            currentItem.artist = value.name;
                            break;
                        default:
                            if(!isAlreadyInList) {
                                currentItem.title = value.name;
                            }
                            break;
                    }

                    if (!isAlreadyInList) {

                        currentItem.id = value.uid;
                        currentItem.isTrack = value.isPlayable;
                        currentItem.currentFolderBytes = value.currentFolderBytes;


                        byte[] currentBytes = DataMatrix.getBytes(BTSinkKeys.DATA_BT_SINK_CURRENT_FOLDER_BYTES);
                        Log.e(TAG, "currentBytes: " + Arrays.toString(currentBytes) + ", current Item bytes: " + Arrays.toString(currentItem.currentFolderBytes));
                        if (currentBytes != null && Arrays.equals(currentBytes, currentItem.currentFolderBytes)) {
                            Log.e(TAG, "DATA_MEDIA_FOLDER_ITEMS_PARCELABLE adapter. About to add item");
                            mediaItemList.add(currentItem);
                            mMediaBrowser.add(currentItem);
                        } else if (currentBytes == null || DataMatrix.getInt(BTSinkKeys.DATA_BT_SINK_NUM_FOLDER_ITEMS_INT, -1) == 1) {
                            Log.e(TAG, "DATA_MEDIA_FOLDER_ITEMS_PARCELABLE adapter. About to add item because currentBytes is null");
                            mediaItemList.add(currentItem);
                            mMediaBrowser.add(currentItem);
                        }
                    }

                    DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, false);


                }

            });

            mDataMatrix.createParcelableAdapter(BTSinkKeys.DATA_MEDIA_FOLDER_ITEMS_PARCELABLE, MediaItems.class, items -> {

//                Log.e("emptyBTLibBug", "DATA_MEDIA_FOLDER_ITEMS_PARCELABLE adapter. Begin");
//                int offset = mMediaBrowser.getmAdapter().getItemCount();
//
//
//                Log.e("gsc", "BEFORE ABOUT TO ADD ITEM");
//
//                for(MediaItem item: items.getMediaItems()) {
//                    byte[] currentBytes = DataMatrix.getBytes(BTSinkKeys.DATA_BT_SINK_CURRENT_FOLDER_BYTES);
//
//                    if(currentBytes != null && Arrays.equals(currentBytes, item.currentFolderBytes)) {
//                        Log.e("emptyBTLibBug", "DATA_MEDIA_FOLDER_ITEMS_PARCELABLE adapter. About to add item");
//                        mMediaBrowser.add(item);
//                    } else if(currentBytes == null) {
//                        Log.e("emptyBTLibBug", "DATA_MEDIA_FOLDER_ITEMS_PARCELABLE adapter. About to add item because currentBytes is null");
//                        mMediaBrowser.add(item);
//                    }
//
//                }
//
//                DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, false);


            }, false);

            mDataMatrix.createByteArrayAdapter(BTSinkKeys.DATA_BT_SINK_CURRENT_FOLDER_BYTES, bytes -> {
                Log.e(TAG, "DATA_BT_SINK_CURRENT_FOLDER_BYTES adapter. About to clear all items, bytes: " + bytes);
//                mMediaBrowser.getmAdapter().clear();
//                mediaItemList.clear();
//                for(int i = 0; i < mMediaBrowser.getmAdapter().getMediaItems().size(); i++) {
//                    MediaItem mediaItem = mMediaBrowser.getmAdapter().getMediaItems().get(i);
//                    if(!Arrays.equals(mediaItem.currentFolderBytes, bytes)) {
//                        mMediaBrowser.getmAdapter().removeAt(i);
//                        Log.e("removed", "just removed: " + mediaItem.title);
//                    }
//                }
                //TODO remove the media items that are in the list already that do not belong
            });


            mDataMatrix.createBoolAdapter(BTSinkKeys.DATA_BT_SINK_PLAYBACK_IS_PLAYING_BOOL, isPlaying -> {
                if(isPlaying) {
                    if(songPositionRequestTimer != null) {
                        songPositionRequestTimer.start();
                    }
                    if (mMenuIsHidden) {
                        mAudioPlayer.show();
                    }
                }
                else {
                    if(songPositionRequestTimer != null) {
                        songPositionRequestTimer.stop();
                    }
                }

                mAudioPlayer.updatePlayingStatus(isPlaying);
            });

            mDataMatrix.createBoolAdapter(BTSinkKeys.DATA_BT_SET_SHUFFLE_BOOL, isShuffle -> {
                if (mIsShuffle != isShuffle) {
                    mIsShuffle = isShuffle;
                    mAudioPlayer.showShuffleButton(isShuffle);
                }
            });

            mDataMatrix.createIntAdapter(BTSinkKeys.DATA_BT_SET_REPEAT_INT, repeatStatus -> {
                if (mRepeatMode != JHTTypes.REPEAT_MODE.values()[repeatStatus - 1]) {
                    mRepeatMode = JHTTypes.REPEAT_MODE.values()[repeatStatus - 1];
                    mAudioPlayer.showRepeatButton(mRepeatMode);
                }
            });

            mDataMatrix.createStringAdapter(BTSinkKeys.DATA_BT_SINK_PLAYBACK_ARTIST_STRING, artist ->{
                mAudioPlayer.updateArtistName(artist);
            });

            mDataMatrix.createStringAdapter(BTSinkKeys.DATA_BT_SINK_PLAYBACK_SONG_STRING, song ->{
                mAudioPlayer.updateSongName(song);

                List<MediaItem> possibleMediaItems = new ArrayList<>();

                for(MediaItem item : mMediaBrowser.getmAdapter().getMediaItems()) {
                    if(item.title != null && !item.title.isEmpty() && (song.indexOf(item.title) != -1)) {
                        possibleMediaItems.add(item);
                    }
                }


                if(possibleMediaItems.size() > 1){
                    MediaItem longestMatchingTitleItem = possibleMediaItems.get(0);
                    for(int i = 1; i < possibleMediaItems.size(); i++){
                        if(possibleMediaItems.get(i).title.length() > longestMatchingTitleItem.title.length()){
                            longestMatchingTitleItem = possibleMediaItems.get(i);
                        }
                    }
                    mMediaBrowser.setCurrentTrack(longestMatchingTitleItem);
                    return;
                } else if(possibleMediaItems.size() == 1){
                    mMediaBrowser.setCurrentTrack(possibleMediaItems.get(0));
                    return;
                }

                MediaItem item = DataMatrix.getParcelable(BTSinkKeys.DATA_BT_SINK_MISSING_TITLE_MEDIA_ITEM_PARCELABLE, MediaItem.class);
                if(item != null){
                    DataMatrix.putParcelable(BTSinkKeys.DATA_BT_SINK_MISSING_TITLE_MEDIA_ITEM_PARCELABLE, null);
                    mMediaBrowser.updateMediaTitle(item, song);
                }

            });

            mDataMatrix.createParcelableAdapter(BTSinkKeys.DATA_BT_SINK_PLAY_STATUS_PARCELABLE, Bundle.class, bundle -> {
                Log.e(TAG, "status: " + bundle.getInt("status") + ", position: " +
                        bundle.getLong("song_position") + ", length: " + bundle.getLong("song_length"));
                mAudioPlayer.setMaxBarProgress(((int) bundle.getLong("song_length")));
                mAudioPlayer.updatePosition((int)bundle.getLong("song_position"));
            });

            mDataMatrix.createBoolAdapter(BTSinkKeys.DATA_BT_SINK_CAN_START_SONG_POSITION_TIMER_BOOL, canStart -> {
                if(!canStart) {
                    Log.e(TAG, "STOPPING TIMER");
                    songPositionRequestTimer.stop();
                } else {
                    songPositionRequestTimer.start();
                }
            });


        }
    }

    @Override
    protected boolean isVideoPlayer() {
        return false;
    }

    @Override
    protected void updateItems() {
//        mHandler.post(() -> mMediaBrowser.add(mMediaItems));
//        mMediaBrowser.update(mMediaItems);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DataMatrix.getBoolean(HALKeys.DATA_IS_USING_BLE_BROWSING_BOOL)) {
            Log.e(TAG, "AudioPlayerController will be set to sink device");
            isSinkDeviceSet = true;
            mediaItemList = new ArrayList<>();
        }

        if(isSinkDeviceSet) {
            songPositionRequestTimer = new PeriodicTimer(0, 1000, () -> DataMatrix.putInt(BTSinkKeys.DATA_BT_SINK_GET_PLAYBACK_STATUS_INT, 0));
            songPositionRequestTimer.start();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.audio_player_controller, container, false);

        return mView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMediaBrowser = (MediaBrowser) getChildFragmentManager().findFragmentById(R.id.audio_browser);
        mAudioPlayer = (AudioPlayerView) getChildFragmentManager().findFragmentById(R.id.audio_player_view);

        mMediaBrowser.setOnBrowseListener(new MediaBrowser.OnBrowseListener() {
            @Override
            public void onItemClick(MediaItem item, int position) {
                DataMatrix.putParcelable(BTSinkKeys.DATA_BT_SINK_MISSING_TITLE_MEDIA_ITEM_PARCELABLE, item);
                Log.d(TAG, "Click item: " + item.toString());
                mSelectedItem = item;
                select(item);
                if(isSinkDeviceSet) {
                    if(item.isTrack) {
                        Log.e(TAG, "its a track");
                        mIsInPlayed = true;
                        if (mMenuIsHidden) {
                            mAudioPlayer.show();
                        }
                        mMediaBrowser.setCurrentTrack(mSelectedItem);
                        DataMatrix.putString(HALKeys.ACTION_PLAY_ITEM_COMMAND_STRING, item.id);

                    } else {
                        if(firstFolderItemsTimer != null) {
                            firstFolderItemsTimer.stop();
                            firstFolderItemsTimer = null;
                        }
                        DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, true);

                        int counter = DataMatrix.getInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT);
                        DataMatrix.putInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT, ++counter);
                        //clearList();
                        mediaItemList.clear();
                        Log.e(TAG, "onItemClick item listener. About to clear all items");


                        List<MediaItem> mediaItems = mMediaBrowser.getmAdapter().getMediaItems();

                        cachedFolderLists.put(cachedFoldersIndex, new ArrayList<>(mediaItems));
                        cachedFoldersIndex++;
                        blockedByBrowsingUp = false;

                        mMediaBrowser.clear();
                        mMediaBrowser.setBrowseUpButton(mSelectedItem.title);
                        Bundle bundle = new Bundle();
                        bundle.putInt("direction", 1);
                        bundle.putString("uid", item.id);
//                        Tuple2<Integer, String> tuple = new Tuple2<>(1, item.id);
                        DataMatrix.putParcelable(HALKeys.ACTION_CHANGE_PATH_COMMAND_PARCELABLE, bundle);
                    }

                } else {
                    if (item.isTrack) {
                        mIsInPlayed = true;
                        if (mMenuIsHidden) {
                            mAudioPlayer.show();
                        }
                        mMediaBrowser.setCurrentTrack(mSelectedItem);
//                        mAudioPlayer.updateCurrentTrack(mSelectedItem);
                        play();
                    } else {
                        mMediaBrowser.clear();
//                    mMediaItems.clear();
                        mMediaBrowser.setBrowseUpButton(mSelectedItem.title);
                    }
                }

            }

            @Override
            public void onBrowseUp() {
                if(isSinkDeviceSet) {
                    if(firstFolderItemsTimer != null) {
                        firstFolderItemsTimer.stop();
                        firstFolderItemsTimer = null;
                    }
                    int counter = DataMatrix.getInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT);
                    if(counter > 0)
                        DataMatrix.putInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT, --counter);

                    //clearList();
                    mediaItemList.clear();
                    Log.e(TAG, "onBrowseUp");
                    mMediaBrowser.clear();

                    mMediaBrowser.addList(cachedFolderLists.get(cachedFoldersIndex - 1));
                    cachedFoldersIndex--;
                    blockedByBrowsingUp = true;

                    Bundle bundle = new Bundle();
                    bundle.putInt("direction", 0);
                    bundle.putString("uid", "0,0,0,0,0,0,0,0");

                    DataMatrix.putParcelable(HALKeys.ACTION_CHANGE_PATH_COMMAND_PARCELABLE, bundle);

                    DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, false);
                } else {
                    back();

                }

            }
        });

        Log.e(TAG, "About to clear all items");
        if(mMediaBrowser != null) {
            mMediaBrowser.clear();
        }
        if(mediaItemList != null) {
            mediaItemList.clear();
        }
        else {
            mediaItemList = new ArrayList<>();
        }


        initDataMatrix();

        if(isSinkDeviceSet) {
            mMediaBrowser.getmMediaItemList().addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if(dy > 0) {
                        int visibleItemCount = mMediaBrowser.getmMediaItemList().getLayoutManager().getChildCount();
                        int totalItemCount = mMediaBrowser.getmMediaItemList().getLayoutManager().getItemCount();
                        int pastVisibleItems = ((LinearLayoutManager)mMediaBrowser.getmMediaItemList().getLayoutManager()).findFirstVisibleItemPosition();

                        if((visibleItemCount + pastVisibleItems) >= (totalItemCount) && (mMediaBrowser.getmAdapter().getItemCount() < DataMatrix.getInt(BTSinkKeys.DATA_BT_SINK_NUM_FOLDER_ITEMS_INT, -1))) {
                            Log.e(TAG, "ADDING MORE ITEMS");
                            //DataMatrix.putInt(HALKeys.EVENT_GET_FOLDER_ITEMS, totalItemCount);
                            if(firstFolderItemsTimer == null)
                                startGetFolderItemTimer(DataMatrix.getInt(BTSinkKeys.DATA_BT_SINK_NUM_FOLDER_ITEMS_INT, -1));
                        }

                        Log.e(TAG, "visibleItemCount: " + visibleItemCount + ", totalItemCount: " + totalItemCount + ", pastVisibleItems: " + pastVisibleItems);
                    }
                }
            });

//            Bundle bundle = new Bundle();
//            bundle.putInt("direction", 0);
//            bundle.putString("uid", "0,0,0,0,0,0,0,0");
//
//            DataMatrix.putParcelable(HALKeys.ACTION_CHANGE_PATH_COMMAND_PARCELABLE, bundle);

            //this callback is triggered whenever we get a response from the change path bt command.
            //the callback contains the total number of folder items in the new path
            mDataMatrix.createIntAdapter(BTSinkKeys.DATA_BT_SINK_NUM_FOLDER_ITEMS_INT, value -> {

//                mediaItemList.clear();

                Log.e(TAG, "value: " + value);
                if(value != -1) {
                    if(!blockedByBrowsingUp) {
                        if(firstFolderItemsTimer == null)
                        {
                            startGetFolderItemTimer(value);
                        }
                        else {
                            firstFolderItemsTimer.stop();
                            firstFolderItemsTimer = null;
                            startGetFolderItemTimer(value);
                        }
                    }
                }

            }, false);

            mDataMatrix.createBoolAdapter(BTSinkKeys.DATA_BT_SINK_IS_AVRCP_READY_BOOL, isAVRCPReady -> {
                if(isAVRCPReady) {
                    DataMatrix.putInt(HALKeys.EVENT_GET_FOLDER_ITEMS, 0);
                }

            });

        }
        if(isSinkDeviceSet) {
            if(DataMatrix.getBoolean(BTSinkKeys.DATA_BT_SINK_PLAYBACK_IS_PLAYING_BOOL)) {
                mIsShuffle = DataMatrix.getBoolean(BTSinkKeys.DATA_BT_SET_SHUFFLE_BOOL);
                int repeat = DataMatrix.getInt(BTSinkKeys.DATA_BT_SET_REPEAT_INT);
                if(repeat > 0) {
                    mRepeatMode = JHTTypes.REPEAT_MODE.values()[repeat - 1];
                }
                mAudioPlayer.showRepeatButton(mRepeatMode);
                mAudioPlayer.showShuffleButton(mIsShuffle);


                mAudioPlayer.updatePlayingStatus(DataMatrix.getBoolean(BTSinkKeys.DATA_BT_SINK_PLAYBACK_IS_PLAYING_BOOL));
            } else {
                mAudioPlayer.hide();
            }


        } else {
            mAudioPlayer.hide();
            mIsShuffle = DataMatrix.getBoolean(UsbMediaDeviceKeys.DATA_IS_SHUFFLE_BOOL);
            mRepeatMode = JHTTypes.REPEAT_MODE.values()[DataMatrix.getInt(UsbMediaDeviceKeys.DATA_REPEAT_MODE_ENUM_INT)];
            MediaItem current = DataMatrix.getParcelable(UsbMediaDeviceKeys.DATA_CURRENT_MEDIA_ITEM_PARCELABLE, MediaItem.class);
            if (current != null && !current.isVideo) {
                if (mMenuIsHidden) {
                    mAudioPlayer.show();
                }
                mMediaBrowser.setCurrentTrack(current);
                mAudioPlayer.updateCurrentTrack(current);
            }

            byte[] albumArtByteArray = DataMatrix.getBytes(UsbMediaDeviceKeys.DATA_ALBUM_ART_BYTES);
            if (albumArtByteArray != null) {
                Bitmap albumArt = BitmapFactory.decodeByteArray(albumArtByteArray, 0, albumArtByteArray.length);
                mAudioPlayer.updateAlbumCover(albumArt);
            } else {
                mAudioPlayer.updateAlbumCover(null);
            }
        }

        selectTopLevel(false);
    }

    public void startGetFolderItemTimer(int value) {
        firstFolderItemsTimer = new PeriodicTimer(500, 500, () -> {
            int visibleItemCount = mMediaBrowser.getmMediaItemList().getLayoutManager().getChildCount();
            int totalItemCount = mMediaBrowser.getmMediaItemList().getLayoutManager().getItemCount();
            int pastVisibleItems = ((LinearLayoutManager)mMediaBrowser.getmMediaItemList().getLayoutManager()).findFirstVisibleItemPosition();
            int totalFolderItems = mMediaBrowser.getmAdapter().getItemCount();

            //we want my media to load 30 more items past what is visible.
            Log.e(TAG, "pastVisibleItems: " + pastVisibleItems + ", visibleItemCount: " + visibleItemCount + ", totalItemCount: " + totalItemCount + ", value: " + value + ", totalFolderItems: " + totalFolderItems);
            if(totalFolderItems < value && (pastVisibleItems + visibleItemCount + 30 > totalItemCount)){
                Log.e(TAG, "in startGetFolderItemTimer");
                DataMatrix.putInt(HALKeys.EVENT_GET_FOLDER_ITEMS, totalFolderItems);
            } else {
                if (firstFolderItemsTimer != null) {
                    firstFolderItemsTimer.stop();
                    firstFolderItemsTimer = null;
                }
            }
        });

        firstFolderItemsTimer.start();
    }


    public void clearList() {
        Log.e(TAG, "in clearList()");
        MediaItems mediaItems = DataMatrix.getParcelable(BTSinkKeys.DATA_MEDIA_FOLDER_ITEMS_PARCELABLE, MediaItems.class);
        if(mediaItems != null) {
           mediaItems.clear();
           DataMatrix.putParcelable(BTSinkKeys.DATA_MEDIA_FOLDER_ITEMS_PARCELABLE, mediaItems);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "AudioPlayerControllerView onDestroyView()");
        if (songPositionRequestTimer != null) {
            songPositionRequestTimer.stop();
            songPositionRequestTimer = null;
        }
        if(firstFolderItemsTimer != null) {
            firstFolderItemsTimer.stop();
            firstFolderItemsTimer = null;
        }

        mDataMatrix.finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "AudioPlayerControllerView onPause()");
        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_PAUSE);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "AudioPlayerControllerView onStop()");
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "AudioPlayerControllerView onResume()");
        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_AUTO_PLAY);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "AudioPlayerControllerView onDestroy()");
    }

    public void hide() {
        mView.setVisibility(View.GONE);
    }

    public void show() {
        mView.setVisibility(View.VISIBLE);
    }

    private void back() {
        mMediaBrowser.clear();
        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_BACK);
    }

    private void play() {
        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_PLAY);
    }

    public void setSinkDevice(boolean isSetSinkDevice) {
        this.isSinkDeviceSet = isSetSinkDevice;
    }

    public void showAudioPlayerView(boolean show) {
        mMenuIsHidden = show;
        if (show) {
            if (mIsInPlayed) {
                mAudioPlayer.show();
            }
        } else {
            mAudioPlayer.hide();
        }
    }

}
