package com.jht.chimera.service.usbmedia.ios;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.util.SparseIntArray;

import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.drivers.usb.Usb;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.chimera.service.hal.lib.hardware.AudioSelectManager;
import com.jht.chimera.service.usbmedia.UsbMediaDevice;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.ControlSession;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.FileTransferSession;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.IIAP2Callback;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.Link;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.Packet;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.Parameter;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.ReadThread;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.WriteThread;
import com.jht.chimera.service.usbmedia.ios.lib.media.MediaItemCreator;
import com.jht.chimera.service.usbmedia.ios.lib.media.MediaPlaylist;
import com.jht.chimera.service.usbmedia.ios.lib.media.MediaTree;
import com.jht.chimera.service.usbmedia.ios.lib.media.Node;
import com.jht.chimera.service.usbmedia.ios.lib.media.Track;
import com.jht.datamatrixlib.DataMatrix;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings("CharsetObjectCanBeUsed")
public class IOSDeviceManager extends UsbMediaDevice {
    private static final String TAG = "IOSDeviceManager";
    public static final int OUTPUT_PACKET_SIZE = 194;
    public static final byte HID_OUTPUT_REPORT_ID = 0x14;

    private final UsbManager mUsbManager;
    private final UsbDevice mUsbDevice;
    //private int mLibUsbError;
    private final byte[] initData = {(byte) 0xFF, 0x55, 0x02, 0x00, (byte) 0xEE, 0x10};
    private ControlSession mControlSession;
    private WriteThread mIAP2WriteThread;
    private ReadThread mIAP2ReadThread;
    private Link mLink;
    private int mSampleRate;
    private final CopyOnWriteArrayList<String> mTrackIds = new CopyOnWriteArrayList<>();
    private Node mCurrentNode;
    private final BlockingQueue<Track> mTrackQueue = new LinkedBlockingQueue<>();
    private String mLocalMediaLibraryId;
//    private String mRadioMediaLibraryId;
    //private List<Integer> mInactiveFileId = new ArrayList<>();
    private boolean mIsNewTrack;
    private int mStartPlayingIndex;
    private final List<MediaPlaylist> mMediaPlaylists = new ArrayList<>();
    private final BlockingQueue<MediaPlaylist> mPlaylistQueue = new LinkedBlockingQueue<>();
    private final MediaTree mMediaTree = new MediaTree();
    private MediaItemCreator mMediaItemCreator;
    //private boolean mIsRadioScanned;
    //private boolean mIsLocalScanned;
    //private boolean mScanRadioLibrary;
    private final SparseIntArray mFileIdTypeMaps = new SparseIntArray();
//    private boolean mPlayRadio;
    private final CopyOnWriteArrayList<MediaItem> mCurrentMediaItems = new CopyOnWriteArrayList<>();
    //private int mediaItemNum;
    private final List<FileTransferSession> mFileTransSessions = new ArrayList<>();
    //private String mLocalLibraryRevision;
    //private String mRadioLibraryRevision;
    private IOSDeviceConnection mConnection;
    private int mPlaylistCount;
    private int mUpdateProgress;
    private MediaItem mMediaItem;
    private final DataMatrix mDataMatrix = new DataMatrix();
    private boolean mIsPlaying;

    /*
    enum LibUsbError {
        LIBUSB_SUCCESS(0),
        LIBUSB_ERROR_BUSY(-6),
        LIBUSB_ERROR_OTHER(-99),
        LIBUSB_ERROR_UNKNOWN(-100);

        private int value;

        LibUsbError(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static LibUsbError getError(int value) {
            LibUsbError result = LIBUSB_ERROR_UNKNOWN;
            for(LibUsbError e : LibUsbError.values()) {
                if(e.getValue() == value) {
                    result = e;
                    break;
                }
            }

            return result;
        }
    }
     */


    private final IIAP2Callback mIAP2Callback = new IIAP2Callback() {

        @Override
        public void onInit(byte[] data) {
            mLink.stopTimer();
            if(checkIAP2Support(data)) {
                mLink.sync();
                mControlSession = new ControlSession(mContext, mLink, mIAP2WriteThread);
                mMediaItemCreator = new MediaItemCreator(mContext, mTrackQueue, mPlaylistQueue);
                mMediaItemCreator.setOnReadyListener(item -> {
                    mCurrentMediaItems.add(item);
                    if (mCurrentNode.getCategory() != Node.Category.TOP_LEVEL && mCurrentNode.getCategory() != Node.Category.LIBRARY) {
                        Node child = new Node(mCurrentNode.getCategory(), item.id, item.title);
                        mCurrentNode.add(child);
                    }
                });
            }
        }

        @Override
        public void onReset() {
            mLink.sync();
        }

        @Override
        public void onSynchronize(Packet packet) {
            mLink.setLastReceivedSequence(packet.getSequence());
            Packet packetToWrite = new Packet(mLink, Packet.ControlMask.ACK, mLink.getLastReceivedSequence(), (byte) 0x00);
            mIAP2WriteThread.addPacket(packetToWrite);
        }

        @Override
        public void onReqAuthCertificate(Packet packet) {
            acknowledge(packet);
            mControlSession.sendAuthCertificate(packet);
        }

        @Override
        public void onReqAuthChallengeResponse(Packet packet) {
            acknowledge(packet);
            mControlSession.sendAuthResponse(packet);

        }

        @Override
        public void onAuthSucceeded(Packet packet) {
            acknowledge(packet);
        }

        @Override
        public void onAuthFailed(Packet packet) {
            acknowledge(packet);
        }

        @Override
        public void onStartIdentification(Packet packet) {
            acknowledge(packet);
            mControlSession.sendIdentificationInfo();
        }

        @Override
        public void onIdentificationAccepted(Packet packet) {
            acknowledge(packet);

            mControlSession.sendStartMediaLibraryInfo();
            mControlSession.sendStartNowPlayingUpdates();
            mControlSession.setAccessoryPowerSource();
            mControlSession.sendStartHID();
            mControlSession.sendStartUSBDeviceModeAudio();
        }

        @Override
        public void onDeviceInfoUpdate(Packet packet) {
            acknowledge(packet);
            Parameter param = new Parameter();
            param.parse(packet.getMessage().getData());
            byte[] data = param.getData();
            String deviceName = "";
            try {
                deviceName = new String(data, "UTF-8").replace("\0", "");
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, e);
            }

            DataMatrix.putString(UsbMediaDeviceKeys.DATA_DEVICE_NAME_STRING, deviceName);
            DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_HAS_VIDEO_BOOL, false);
            DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_CONNECTED_BOOL, true);

        }

        @Override
        public void onUSBDeviceModeAudioInfo(Packet packet) {
            int sampleRate = 0;
            acknowledge(packet);
            Parameter param = new Parameter();
            param.parse(packet.getMessage().getData());
            byte[] data = param.getData();
            switch (data[0]) {
                case 6:
                    sampleRate = 32000;
                    break;
                case 7:
                    sampleRate = 44100;
                    break;
                case 8:
                    sampleRate = 48000;
                    break;
            }

//            Log.d(TAG, "onUSBDeviceModeAudioInfo: sample rate " + sampleRate + " Hz");

            if(mSampleRate == sampleRate) {
                return;
            }

            DataMatrix.putInt(HALKeys.DATA_IOS_AUDIO_SAMPLE_RATE_INT, sampleRate);

            mSampleRate = sampleRate;
        }

        @Override
        public void onPowerUpdate(Packet packet) {
            acknowledge(packet);
            byte[] msgData = packet.getMessage().getData();
            int totalParamLen = msgData.length;
            int srcPos = 0;
            while(totalParamLen > 0) {
                byte[] dest = new byte[totalParamLen];
                System.arraycopy(msgData, srcPos, dest, 0, totalParamLen);
                Parameter param = new Parameter();
                param.parse(dest);
                int paramLen = param.getLength();
                int paramId = param.getId();
                if(paramId == 0) {
                    ByteBuffer buffer = ByteBuffer.allocate(paramLen - 4);
                    buffer.put(dest, 4, paramLen - 4);
                    buffer.position(0);
                    int current = buffer.getChar();
                    Log.d(TAG, "onPowerUpdate: maximum current drawn form accessory " + current + " mA");
                } else if(paramId == 1) {
                    ByteBuffer buffer = ByteBuffer.allocate(paramLen - 4);
                    buffer.put(dest, 4, paramLen - 4);
                    buffer.position(0);
                    int willCharge = buffer.get();
                    Log.d(TAG, "onPowerUpdate: device battery will charge " + (willCharge == 0 ? "NO" : "YES"));
                }
                srcPos += paramLen;
                totalParamLen -= paramLen;

            }

        }

        @Override
        public void onNowPlayingUpdate(Packet packet) {
//            Log.d(TAG, "============== Entering onNowPlayingUpdate ==============");
            acknowledge(packet);
            byte[] msgData = packet.getMessage().getData(); // all parameters' bytes including length, id for every parameter and sub-parameter
            int totalParamLen = msgData.length;
            int srcPos = 0;

            while (totalParamLen > 0) {
                byte[] paramByteArray = new byte[totalParamLen];
                System.arraycopy(msgData, srcPos, paramByteArray, 0, totalParamLen);
                Parameter param = new Parameter();
                param.parse(paramByteArray);
                int paramLen = param.getLength();
                int paramId = param.getId();

                int totalSubParamLen = paramLen - 4;
                int srcPos2 = 4;

                if (paramId == 0) {
//                    Log.d(TAG, "onNowPlayingUpdate: parameter id --> 0");
                    MediaItem currentItem = new MediaItem();
                    if (mMediaItem != null) {
                        currentItem.id = mMediaItem.id;
                        currentItem.title = mMediaItem.title;
                        currentItem.duration = mMediaItem.duration;
                        currentItem.album = mMediaItem.album;
                        currentItem.artist = mMediaItem.artist;
                        currentItem.genre = mMediaItem.genre;
                    }
                    currentItem.isTrack = true;
                    while (totalSubParamLen > 0) {
                        byte[] subParamByteArray = new byte[totalSubParamLen];
                        System.arraycopy(paramByteArray, srcPos2, subParamByteArray, 0, totalSubParamLen);
                        Parameter subParam = new Parameter();
                        subParam.parse(subParamByteArray);
                        int subParamLen = subParam.getLength();
                        int subParamId = subParam.getId();
                        byte[] subParamData = new byte[subParamLen - 4];
                        System.arraycopy(subParamByteArray, 4, subParamData, 0, subParamLen - 4);
                        switch (subParamId) {
                            case 0:
                                BigInteger itemId = new BigInteger(1, subParamData);
                                Log.d(TAG, "[KEVIN]onNowPlayingUpdate: MediaItem Item Id --> " + itemId);
                                currentItem.id = itemId.toString();
                                break;
                            case 1:
                                String title = "";
                                try {
                                    title = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                Log.d(TAG, "[KEVIN]onNowPlayingUpdate: MediaItem Item Title --> " + title);
                                currentItem.title = title;
                                break;
                            case 4:
                                ByteBuffer buffer = ByteBuffer.allocate(subParamData.length);
                                buffer.put(subParamData);
                                buffer.position(0);
                                long duration = buffer.getInt();
//                                Log.d(TAG, "onNowPlayingUpdate: Playback Duration --> " + duration + " ms");
                                currentItem.duration = (int) duration;
                                break;
                            case 6:
                                String albumTitle = "";
                                try {
                                    albumTitle = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
//                                Log.d(TAG, "onNowPlayingUpdate: Album Title --> " + albumTitle);
                                currentItem.album = albumTitle;
                                break;
                            case 12:
                                String artistTitle = "";
                                try {
                                    artistTitle = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
//                                Log.d(TAG, "onNowPlayingUpdate: Artist Title --> " + artistTitle);
                                currentItem.artist = artistTitle;
                                break;
                            case 16:
                                String genre = "";
                                try {
                                    genre = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
//                                Log.d(TAG, "onNowPlayingUpdate: Genre --> " + genre);
                                currentItem.genre = genre;
                                break;
                            case 18:
                                /*
                                try {
                                    String composer = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                 */
//                                Log.d(TAG, "onNowPlayingUpdate: Composer --> " + composer);
                                break;
                            case 26:
//                                Log.d(TAG, "[KEVIN]onNowPlayingUpdate: File Transfer Id --> " + (subParamData[0] & 0xff));
                                break;


                        }
                        totalSubParamLen -= subParamLen;
                        srcPos2 += subParamLen;
                    }
                    if (currentItem.id != null) {
                        mMediaItem = currentItem;
                        DataMatrix.putParcelable(UsbMediaDeviceKeys.DATA_CURRENT_MEDIA_ITEM_PARCELABLE, currentItem);
                    }
                } else if (paramId == 1) {
//                    Log.d(TAG, "onNowPlayingUpdate: parameter id --> 1");
                    while (totalSubParamLen > 0) {
                        byte[] subParamByte = new byte[totalSubParamLen];
                        System.arraycopy(paramByteArray, srcPos2, subParamByte, 0, totalSubParamLen);
                        Parameter subParam = new Parameter();
                        subParam.parse(subParamByte);
                        int subParamLen = subParam.getLength();
                        int subParamId = subParam.getId();
                        byte[] subParamData = new byte[subParamLen - 4];
                        System.arraycopy(subParamByte, 4, subParamData, 0, subParamLen - 4);
                        switch (subParamId) {
                            case 0:
                                /*String ps = "";
                                if (subParamData[0] == 0) {
                                    ps = "Stopped";
                                } else if (subParamData[0] == 1) {
                                    ps = "Playing";
                                } else if (subParamData[0] == 2) {
                                    ps = "Paused";
                                } else if (subParamData[0] == 3) {
                                    ps = "SeekForward";
                                } else {
                                    ps = "SeekBackward";
                                }
                                Log.d(TAG, "[KEVIN]onNowPlayingUpdate: Playback Status --> " + ps);*/
                                if (subParamData[0] == 1) {
                                    DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.IOS.ordinal());
                                    DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_PLAYING_BOOL, true);
                                } else {
                                    DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_PLAYING_BOOL, false);
                                }
                                break;
                            case 1:
                                ByteBuffer buffer = ByteBuffer.allocate(subParamData.length);
                                buffer.put(subParamData);
                                buffer.position(0);
                                long elapsed = buffer.getInt();
//                                Log.d(TAG, "onNowPlayingUpdate: Playback Elapsed Time --> " + elapsed + " ms");
                                DataMatrix.putInt(UsbMediaDeviceKeys.DATA_PLAYBACK_POSITION_INT, (int) elapsed);
                                break;
                            case 5:
//                                Log.d(TAG, "onNowPlayingUpdate: Playback Shuffle Mode --> " + subParamData[0]);
                                DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SHUFFLE_BOOL, subParamData[0] != 0);
                                break;
                            case 6:
//                                Log.d(TAG, "onNowPlayingUpdate: repeat mode --> " + subParamData[0]);
                                if (subParamData[0] == 0) {
                                    DataMatrix.putInt(UsbMediaDeviceKeys.DATA_REPEAT_MODE_ENUM_INT, JHTTypes.REPEAT_MODE.NONE.ordinal());
                                } else if (subParamData[0] == 1) {
                                    DataMatrix.putInt(UsbMediaDeviceKeys.DATA_REPEAT_MODE_ENUM_INT, JHTTypes.REPEAT_MODE.ONE.ordinal());
                                } else if (subParamData[0] == 2) {
                                    DataMatrix.putInt(UsbMediaDeviceKeys.DATA_REPEAT_MODE_ENUM_INT, JHTTypes.REPEAT_MODE.ALL.ordinal());
                                }
                                break;
                            case 7:
                                /*
                                try {
                                    String appName = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
//                                Log.d(TAG, "onNowPlayingUpdate: Playback App Name --> " + appName);
                                 */
                                break;
                            case 8:
                                /*
                                try {
                                    String s = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                 */
//                                Log.d(TAG, "onNowPlayingUpdate: Playback Media Library Id --> " + s);
                                /*if (s.equals(mRadioMediaLibraryId)) {
                                    mPlayRadio = true;
                                } else {
                                    mPlayRadio = false;
                                }*/
//                                Log.d(TAG, "onNowPlayingUpdate: play Apple Music Radio: " + mPlayRadio);
                                break;
                            /*case 10:
                                String radioStation = new String(subParamData, "UTF-8").replace("\0", "");
//                                Log.d(TAG, "onNowPlayingUpdate: Apple Music Radio station --> " + radioStation);
                                break;
                            case 11:
                                BigInteger radioStationPlaylistId = new BigInteger(1, subParamData);
//                                Log.d(TAG, "onNowPlayingUpdate: PBAppleMusicRadioMediaPlaylistID --> " + radioStationPlaylistId.toString());
                                break;*/
                            case 16:
                                /*
                                try {
                                    String appBundleId = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                 */
//                                Log.d(TAG, "onNowPlayingUpdate: Playback App Bundle Id --> " + appBundleId);
                                break;
                            /*case 17:
//                                Log.d(TAG, "onNowPlayingUpdate: PlaybackQueueListContentTransfer");*/
                        }
                        totalSubParamLen -= subParamLen;
                        srcPos2 += subParamLen;
                    }
                }

                srcPos += paramLen;
                totalParamLen -= paramLen;

            }
//            Log.d(TAG, "============== Done with onNowPlayingUpdate ==============");
        }

        @Override
        public void onMediaLibraryInfo(Packet packet) {
//            Log.d(TAG, "onMediaLibraryInfo: ");
            acknowledge(packet);
            byte[] msgData = packet.getMessage().getData(); // all parameters' bytes including length, id for every parameter and sub-parameter
            if(msgData == null) {
                return;
            }
            int totalParamLen = msgData.length;
            int srcPos = 0;
            while(totalParamLen > 0) {
                byte[] paramByteArray = new byte[totalParamLen];
                System.arraycopy(msgData, srcPos, paramByteArray, 0, totalParamLen);
                Parameter param = new Parameter();
                param.parse(paramByteArray);
                int paramLen = param.getLength();
                int paramId = param.getId();

                int totalSubParamLen = paramLen - 4;
                int srcPos2 = 4;

                if(paramId == 0) {
                    String libraryId = null;
                    int type = 0;
                    while(totalSubParamLen > 0) {
                        byte[] subParamByte = new byte[totalSubParamLen];
                        System.arraycopy(paramByteArray, srcPos2, subParamByte, 0, totalSubParamLen);
                        Parameter subParam = new Parameter();
                        subParam.parse(subParamByte);
                        int subParamLen = subParam.getLength();
                        int subParamId = subParam.getId();
                        byte[] subParamData = new byte[subParamLen - 4];
                        System.arraycopy(subParamByte, 4, subParamData, 0, subParamLen - 4);
                        switch(subParamId) {
                            case 0:
                                /*
                                try {
                                    String libraryName = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }*/
                                break;
                            case 1:
                                try {
                                    libraryId = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                break;
                            case 2:
                                type = subParamData[0];
                                break;
                        }

                        totalSubParamLen -= subParamLen;
                        srcPos2 += subParamLen;
                    }

                    if(type == 0) {
                        mLocalMediaLibraryId = libraryId;
                    }/* else if(type == 2) {
                        mRadioMediaLibraryId = libraryId;
                    }*/
                }

                srcPos += paramLen;
                totalParamLen -= paramLen;
            }

            if (mLocalMediaLibraryId != null) {
                mControlSession.sendStartMediaLibraryUpdates(mLocalMediaLibraryId, null);
            }

        }

        @Override
        public void onMediaLibraryUpdate(Packet packet) {
//            Log.d(TAG, "============== Entering onMediaLibraryUpdate ==============");
            //String libraryId = "";
            int type;
            acknowledge(packet);
            boolean duplicate;
            byte[] msgData = packet.getMessage().getData(); // all parameters' bytes including length, id for every parameter and sub-parameter
            int totalParamLen = msgData.length;
            int srcPos = 0;
            while(totalParamLen > 0) {
                byte[] paramByteArray = new byte[totalParamLen];
                System.arraycopy(msgData, srcPos, paramByteArray, 0, totalParamLen);
                Parameter param = new Parameter();
                param.parse(paramByteArray);
                int paramLen = param.getLength();
                int paramId = param.getId();

                if(paramId == 0) {
                    ByteBuffer dataBuffer = ByteBuffer.allocate(paramLen - 4);
                    dataBuffer.put(paramByteArray, 4, paramLen - 4);
                    dataBuffer.position(0);
                    /*
                    try {
                        libraryId = new String(dataBuffer.array(), "UTF-8").replace("\0", "");
                    } catch (UnsupportedEncodingException e) {
                        Log.w(TAG, e);
                    } */
//                    Log.d(TAG, "[MediaLibraryUpdate]onMediaLibraryUpdate: MediaItem library id --> " + libraryId);
                } else if(paramId == 1) {
                    ByteBuffer dataBuffer = ByteBuffer.allocate(paramLen - 4);
                    dataBuffer.put(paramByteArray, 4, paramLen - 4);
                    dataBuffer.position(0);
                    /*if(libraryId.equals(mLocalMediaLibraryId)) {

                        try {
                            mLocalLibraryRevision = new String(dataBuffer.array(), "UTF-8").replace("\0", "");
                        } catch (UnsupportedEncodingException e) {
                            Log.w(TAG, e);
                        }
//                        Log.d(TAG, "[MediaLibraryUpdate]onMediaLibraryUpdate: MediaLibraryRevision = " + mLocalLibraryRevision);
                    } else if(libraryId.equals(mRadioMediaLibraryId)) {
                        mRadioLibraryRevision = new String(dataBuffer.array(), "UTF-8").replace("\0", "");
//                        Log.d(TAG, "[MediaLibraryUpdate]onMediaLibraryUpdate: RadioLibraryRevision = " + mRadioLibraryRevision);
                    }*/
                } else if(paramId == 2) {
//                    Log.d(TAG, "[MediaLibraryUpdate]onMediaLibraryUpdate: parameter id = 2");
                    duplicate = false;
                    Track track = new Track();

                    int totalSubParamLen = paramLen - 4;
                    int srcPos2 = 4;

                    while(totalSubParamLen > 0) {
                        byte[] subParamByte = new byte[totalSubParamLen];
                        System.arraycopy(paramByteArray, srcPos2, subParamByte, 0, totalSubParamLen);
                        Parameter subParam = new Parameter();
                        subParam.parse(subParamByte);
                        int subParamLen = subParam.getLength();
                        int subParamId = subParam.getId();
                        byte[] subParamData = new byte[subParamLen - 4];
                        System.arraycopy(subParamByte, 4, subParamData, 0, subParamLen - 4);

                        switch(subParamId) {
                            case 0:
                                BigInteger itemId = new BigInteger(1, subParamData);
                                if(!mTrackIds.contains(itemId.toString())) {
//                                    Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Id --> " + itemId);
                                    duplicate = false;
                                    mTrackIds.add(itemId.toString());
                                    track.setId(itemId.toString());
                                } else {
                                    duplicate = true;
                                }
                                break;
                            case 1:
                                String title = "";
                                try {
                                    title = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                track.setTitle(title);
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Title --> " + title);
                                break;
                            case 2:
                                type = subParamData[0];
                                track.setType(type);
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Type --> " + ControlSession.MediaType.getMediaType(subParamData[0]));
                                break;
                            case 4:
                                ByteBuffer buffer = ByteBuffer.allocate(subParamData.length);
                                buffer.put(subParamData);
                                buffer.position(0);
                                int duration = buffer.getInt();
                                track.setDuration(duration);
                                break;
                            case 5:
                                BigInteger albumId = new BigInteger(1, subParamData);
                                track.setAlbumId(albumId.toString());
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Album Id --> " + albumId);
                                break;
                            case 6:
                                String albumTitle = null;
                                try {
                                    albumTitle = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                track.setAlbumTitle(albumTitle);
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Album --> " + albumTitle);
                                break;
                            case 11:
                            case 13:
                                BigInteger artistId = new BigInteger(1, subParamData);
                                track.setArtistId(artistId.toString());
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Artist Id --> " + artistId);
                                break;
                            case 12:
                                //                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item AlbumArtist Id --> " + albumArtistId);
                            case 14:
                                String artistTitle = "";
                                try {
                                    artistTitle = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                track.setArtistTitle(artistTitle);
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Artist --> " + artistTitle);
                                break;
                            //                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item AlbumArtist --> " + albumArtistTitle);
                            case 15:
                                BigInteger genreId = new BigInteger(1, subParamData);
                                track.setGenreId(genreId.toString());
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Genre Id --> " + genreId);
                                break;
                            case 16:
                                String genreTitle = "";
                                try {
                                    genreTitle = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                track.setGenreTitle(genreTitle);
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Genre --> " + genreTitle);
                                break;
                            case 17:
                                BigInteger composerId = new BigInteger(1, subParamData);
                                track.setComposerId(composerId.toString());
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Composer Id --> " + composerId);
                                break;
                            case 18:
                                String composerTitle = "";
                                try {
                                    composerTitle = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                track.setComposer(composerTitle);
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Item Composer --> " + composerTitle);
                                break;
                            case 19:
                                int compilation = subParamData[0];
                                boolean isCompilation = (compilation != 0);
                                track.setIsCompilation(isCompilation);
//                                Log.d(TAG, "onMediaLibraryUpdate: Is Part of Compilation --> " + (compilation == 0 ? "NO" : "YES"));
                            case 25:
                                //int resident = subParamData[0];
//                                Log.d(TAG, "onMediaLibraryUpdate: Resident On Device --> " + (resident == 0 ? "NO" : "YES"));

                        }

                        totalSubParamLen -= subParamLen;
                        srcPos2 += subParamLen;
                    }
                    try {
                        if(!duplicate) {
                            //if(ControlSession.MediaType.getMediaType(type) == ControlSession.MediaType.MUSIC) {
                            //    mediaItemNum++;
                            //}
                            mTrackQueue.put(track);
                        }
                    } catch (InterruptedException e) {
                        Log.w(TAG, e);
                    }
                } else if(paramId == 3) {
//                    Log.d(TAG, "[MediaLibraryUpdate]onMediaLibraryUpdate: parameter id = 3");
                    //duplicate = false;
                    MediaPlaylist playlist = new MediaPlaylist();
                    int totalSubParamLen = paramLen - 4;
                    int srcPos2 = 4;
                    while(totalSubParamLen > 0) {
                        byte[] subParamByte = new byte[totalSubParamLen];
                        System.arraycopy(paramByteArray, srcPos2, subParamByte, 0, totalSubParamLen);
                        Parameter subParam = new Parameter();
                        subParam.parse(subParamByte);
                        int subParamLen = subParam.getLength();
                        int subParamId = subParam.getId();
                        byte[] subParamData = new byte[subParamLen - 4];
                        System.arraycopy(subParamByte, 4, subParamData, 0, subParamLen - 4);

                        switch(subParamId) {
                            case 0:
                                BigInteger id = new BigInteger(1, subParamData);
                                playlist.setId(id.toString());
//                                Log.d(TAG, "[MediaLibraryUpdate]onMediaLibraryUpdate: MediaItem Playlist id --> " + id);
                                break;
                            case 1:
                                String name = "";
                                try {
                                    name = new String(subParamData, "UTF-8").replace("\0", "");
                                } catch (UnsupportedEncodingException e) {
                                    Log.w(TAG, e);
                                }
                                playlist.setName(name);
//                                Log.d(TAG, "[MediaLibraryUpdate]onMediaLibraryUpdate: MediaItem Playlist name --> " + name);
                                break;
                            case 2:
                                BigInteger pid = new BigInteger(1, subParamData);
                                playlist.setParentId(pid.toString());
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Playlist parent id --> " + pid);
                                break;
                            case 3:
                                int isGeniusMix = subParamData[0];
                                playlist.setGeniusMix(isGeniusMix != 0);
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Playlist is genius mix --> " + (isGeniusMix == 0 ? "NO" : "YES"));
                                break;
                            case 4:
                                int isFolder = subParamData[0];
                                playlist.setFolder(isFolder != 0);
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Playlist is folder --> " + (isFolder == 0 ? "NO" : "YES"));
                                break;
                            case 5:
                                int a = subParamData[0] & 0xff;
                                playlist.setFileTransferId(a);
//                                Log.d(TAG, "[MediaLibraryUpdate]onMediaLibraryUpdate: MediaItem Playlist file transfer id --> " + a);
                                break;
                            case 6:
                                int isAppleMusicRadioStation = subParamData[0];
                                playlist.setAppleMusicRadio(isAppleMusicRadioStation != 0);
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Playlist is Apple Music radio station --> " + (isAppleMusicRadioStation == 0 ? "NO" : "YES"));
                                break;
                            case 7:
//                                Log.d(TAG, "onMediaLibraryUpdate: MediaItem Playlist Content Transfer");
                                break;
                        }

                        totalSubParamLen -= subParamLen;
                        srcPos2 += subParamLen;
                    }
                    mMediaPlaylists.add(playlist);
//                    try {
//                        mPlaylistQueue.put(playlist);
//                    } catch (InterruptedException e) {
//                        Log.w(TAG, e);
//                    }
                } else if(paramId == 7) {
                    ByteBuffer dataBuffer = ByteBuffer.allocate(paramLen - 4);
                    dataBuffer.put(paramByteArray, 4, paramLen - 4);
                    dataBuffer.position(0);
                    int updateProgress = dataBuffer.get();
//                    Log.d(TAG, "[KEVIN]onMediaLibraryUpdate: Update Progress " + updateProgress);
                    if (updateProgress == 100 && mUpdateProgress != 100) {
                        Track exit = new Track();
                        exit.setTitle(mContext.getString(R.string.exit_tc));
                        try {
                            mTrackQueue.put(exit);

                        } catch (InterruptedException e) {
                            Log.w(TAG, e);
                        }

                        mControlSession.sendStopMediaLibraryUpdates();
                        mControlSession.sendStopMediaLibraryInfo();
                    }
                    mUpdateProgress = updateProgress;

                    /*if(updateProgress == 100) {
                        if(!mScanRadioLibrary) {
                            Track exit = new Track();
                            exit.setTitle("Exit");
                            try {
                                mTrackQueue.put(exit);

                            } catch (InterruptedException e) {
                                Log.w(TAG, e);
                            }
                        } else {
                            MediaPlaylist playlist = new MediaPlaylist();
                            playlist.setName("Exit");
                            try {
                                mPlaylistQueue.put(playlist);
                            } catch (InterruptedException e) {
                                Log.w(TAG, e);
                            }
                        }
                    }*/
                } else if(paramId == 8) {
                    ByteBuffer dataBuffer = ByteBuffer.allocate(paramLen - 4);
                    dataBuffer.put(paramByteArray, 4, paramLen - 4);
                    dataBuffer.position(0);
                    /*byte hidRemoteItems =*/ dataBuffer.get();
//                    Log.d(TAG, "onMediaLibraryUpdate: Is Hiding Remote Items: " + (hidRemoteItems == 0 ? "NO" : "YES"));

                } /*else if(paramId == 9) {
                    ByteBuffer dataBuffer = ByteBuffer.allocate(paramLen - 4);
                    dataBuffer.put(paramByteArray, 4, paramLen - 4);
                    dataBuffer.position(0);
                    byte canPlayAllSongs = dataBuffer.get();
                    Log.d(TAG, "onMediaLibraryUpdate: can play all songs: " + canPlayAllSongs);
                }*/

                srcPos += paramLen;
                totalParamLen -= paramLen;
            }
//            if(mLocalMediaLibraryId != null && mLocalMediaLibraryId.equals(libraryId)) {
//                Log.d(TAG, "[MediaLibraryUpdate]onMediaLibraryUpdate: music count: " + mediaItemNum);
//            }

//            Log.d(TAG, "============== Done with onMediaLibraryUpdate ==============");

        }

        @Override
        public void onSetupDatagram(Packet packet) {
            int fileSizeInBytes;
            acknowledge(packet);
            if(packet.getData().length > 10) {
                // version 2
                /*byte[] fileSize = new byte[8];
                System.arraycopy(packet.getData(), 2, fileSize, 0, 8);
                Log.d(TAG, "[File Transfer Session] onSetupDatagram: File Size Byte: " + Packet.byteArray2Hex(fileSize));*/
                ByteBuffer b = ByteBuffer.allocate(8);
                b.put(packet.getData(), 2, 8);
                b.position(0);
                fileSizeInBytes = (int) b.getLong();
                if (packet.getData().length > 10) {
                    ByteBuffer buffer = ByteBuffer.allocate(2);
                    buffer.put(packet.getData(), 10, 2);
                    buffer.position(0);
                    int fileType =  buffer.getChar();
                    mFileIdTypeMaps.put(packet.getFileTransId(), fileType);
//                    Log.d(TAG, "[File Transfer Session] onSetupDatagram: file type: " + fileType + ", file transfer id: " + packet.getFileTransId());
                    /*if (fileType == 4) {
                        byte[] playlistId = new byte[8];
                        System.arraycopy(packet.getData(), 12, playlistId, 0, 8);
                        String mPlaylistId = new BigInteger(1, playlistId).toString();
                        Log.d(TAG, "[File Transfer Session] onSetupDatagram: playlist id: " + mPlaylistId);
                        byte[] libraryId = new byte[packet.getData().length - 20];
                        System.arraycopy(packet.getData(), 20, libraryId, 0, packet.getData().length - 20);
                    }*/
                }
            } else {
                // version 1
                //byte[] fileData = new byte[packet.getData().length - 2];
                //System.arraycopy(packet.getData(), 2, fileData, 0, packet.getData().length - 2);
//                Log.d(TAG, "[File Transfer Session] onSetupDatagram: File Size: " + Packet.byteArray2Hex(fileData));
                ByteBuffer b = ByteBuffer.allocate(packet.getData().length - 2);
                b.put(packet.getData(), 2, packet.getData().length - 2);
                b.position(0);
                fileSizeInBytes = (int) b.getLong();
            }

            FileTransferSession fileSession = new FileTransferSession(mContext, mLink, mIAP2WriteThread, packet.getFileTransId(), fileSizeInBytes);
            fileSession.sendStartDatagram();
            mFileTransSessions.add(fileSession);
        }

        @Override
        public void onFirstData(Packet packet) {
            acknowledge(packet);
            int fileTransId = packet.getFileTransId();
            FileTransferSession fileTransSession = getFileTransSession(fileTransId);
            if(fileTransSession == null) {
                return;
            }

            byte[] fileData = new byte[packet.getData().length - 2];
            System.arraycopy(packet.getData(), 2, fileData, 0, packet.getData().length - 2);
            fileTransSession.setFileData(fileTransId, fileData);
        }

        @Override
        public void onFirstAndOnlyData(Packet packet) {
            acknowledge(packet);
            int fileTransId = packet.getFileTransId();
            FileTransferSession fileTransSession = getFileTransSession(fileTransId);
            if(fileTransSession == null) {
                return;
            }

            int fileType = mFileIdTypeMaps.get(fileTransId);
//            Log.d(TAG, "[KEVIN]onFirstAndOnlyData: file type: " + fileType + ", file transfer id: " + fileTransId);
            MediaPlaylist playlist = null;
//            int fileTransferId = packet.getFileTransId();

            byte[] fileData = new byte[packet.getData().length - 2];
            System.arraycopy(packet.getData(), 2, fileData, 0, packet.getData().length - 2);
            if(fileType == 2) {
                DataMatrix.putBytes(UsbMediaDeviceKeys.DATA_ALBUM_ART_BYTES, fileData);
            } else if(fileType == 4) {
                for(MediaPlaylist _playlist : mMediaPlaylists) {
                    if(_playlist.getFileTransferId() == fileTransId) {
                        playlist = _playlist;
                        break;
                    }
                }

                int count = fileData.length / 8;
//                Log.d(TAG, "[File Transfer Session] onFirstAndOnlyData: file type: 4, count: " + count);
                for (int i = 0; i < count; i++) {
                    byte[] temp = new byte[8];
                    System.arraycopy(fileData, i * 8, temp, 0, 8);
                    String mediaItemId = new BigInteger(1, temp).toString();
//                    Log.d(TAG, "[File Transfer Session] onFirstAndOnlyData: file type: 4, media item id: " + mediaItemId);
                    if(playlist != null) {
                        playlist.addMediaItemId(mediaItemId);
                    }
                }

                if(playlist != null) {
                    mPlaylistCount++;
                    try {
                        mPlaylistQueue.put(playlist);
                    } catch (InterruptedException e) {
                        Log.w(TAG, e);
                    }
                }
//                Log.d(TAG, "[File Transfer Session] onFirstAndOnlyData: mPlaylistCount: " + mPlaylistCount + ", mMediaPlaylists: " + mMediaPlaylists.size());
                if(mPlaylistCount == mMediaPlaylists.size() && mUpdateProgress == 100) {
                    playlist = new MediaPlaylist();
                    playlist.setName(mContext.getString(R.string.exit_tc));
                    try {
                        mPlaylistQueue.put(playlist);
                    } catch (InterruptedException e) {
                        Log.w(TAG, e);
                    }
                }

            }
            fileTransSession.sendSuccessDatagram();
        }

        @Override
        public void onData(Packet packet) {
            acknowledge(packet);
            int fileTransId = packet.getFileTransId();
            FileTransferSession fileTransSession = getFileTransSession(fileTransId);
            if(fileTransSession == null) {
                return;
            }
            byte[] fileData = new byte[packet.getData().length - 2];
            System.arraycopy(packet.getData(), 2, fileData, 0, packet.getData().length - 2);
            fileTransSession.setFileData(fileTransId, fileData);
        }

        @Override
        public void onLastData(Packet packet) {
            acknowledge(packet);
            int fileTransId = packet.getFileTransId();
            FileTransferSession fileTransSession = getFileTransSession(fileTransId);
            if(fileTransSession == null) {
                return;
            }

            MediaPlaylist playlist = null;
            int fileType = mFileIdTypeMaps.get(fileTransId);
//            Log.d(TAG, "[KEVIN]onLastData: file type: " + fileType + ", file transfer id: " + fileTransId);
            byte[] fileData = new byte[packet.getData().length - 2];
            System.arraycopy(packet.getData(), 2, fileData, 0, packet.getData().length - 2);
            fileTransSession.setFileData(fileTransId, fileData);
            if(fileType == 2) {
                DataMatrix.putBytes(UsbMediaDeviceKeys.DATA_ALBUM_ART_BYTES, fileTransSession.getFileData());
            } else if(fileType == 4) {
                for(MediaPlaylist _playlist : mMediaPlaylists) {
                    if(_playlist.getFileTransferId() == fileTransId) {
                        playlist = _playlist;
                        break;
                    }
                }

                byte[] _fileData = fileTransSession.getFileData();
                int count = _fileData.length / 8;
                for (int i = 0; i < count; i++) {
                    byte[] temp = new byte[8];
                    System.arraycopy(_fileData, i * 8, temp, 0, 8);
                    String mediaItemId = new BigInteger(1, temp).toString();
                    if(playlist != null) {
                        playlist.addMediaItemId(mediaItemId);
                    }
//                    Log.d(TAG, "[File Transfer Session] onLastData: media item id: " + mediaItemId);
                }

                if(playlist != null) {
                    try {
                        mPlaylistQueue.put(playlist);
                    } catch (InterruptedException e) {
                        Log.w(TAG, e);
                    }
                }

                if(mPlaylistQueue.size() == mMediaPlaylists.size()) {
                    playlist = new MediaPlaylist();
                    playlist.setName(mContext.getString(R.string.exit_tc));
                    try {
                        mPlaylistQueue.put(playlist);
                    } catch (InterruptedException e) {
                        Log.w(TAG, e);
                    }
                }
            }

            fileTransSession.sendSuccessDatagram();
        }

        @Override
        public void onCancel(Packet packet) {
            acknowledge(packet);
            //int fileTransId = packet.getFileTransId();
            //FileTransferSession fileTransSession = getFileTransSession(fileTransId);
            //if(fileTransSession == null) {
            //    return;
            //}

            //mInactiveFileId.add(packet.getFileTransId());
        }

        private FileTransferSession getFileTransSession(int fileTransId) {
            FileTransferSession fileTransSession = null;
            for(FileTransferSession f : mFileTransSessions) {
                if(f.getFileTransferId() == fileTransId) {
                    fileTransSession = f;
                    break;
                }
            }

            return fileTransSession;
        }
    };

    public IOSDeviceManager(Context context, UsbDevice usbDevice) {
        super(context);

        mDataMatrix.createEventAdapter(UsbMediaDeviceKeys.ACTION_AUTO_PLAY, event -> {
            if (!mIsPlaying) {
                play(true);
            }
        });

        AudioSelectManager.monitorAudioLoss(AudioSourceInput.IOS, mDataMatrix, () -> pause(true));

        mUsbDevice = usbDevice;
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        startMFiProcess();
    }

    private void startMFiProcess() {
        if(Usb.setConfiguration(mUsbDevice.getVendorId(), mUsbDevice.getProductId(), 2)) {
            DataMatrix.putInt(HALKeys.DATA_IOS_AUDIO_SAMPLE_RATE_INT, 48000);
            mConnection = new IOSDeviceConnection(mUsbManager, mUsbDevice);
            if(mConnection.open()) {
                mIAP2WriteThread = new WriteThread(mConnection);
                mIAP2ReadThread = new ReadThread(mConnection, true, mIAP2Callback);
                mLink = new Link(mIAP2WriteThread);
                mLink.init();
            }
        }
    }

    private boolean checkIAP2Support(byte[] buffer) {
        byte[] temp = new byte[initData.length];
        System.arraycopy(buffer, 0, temp, 0, temp.length);
        if(Arrays.equals(initData, temp)) {
            Log.d(TAG, "The iOS device supports iAP2.");
            return true;
        }

        Log.d(TAG, "The iOS device doesn't support iAP2.");
        return false;
    }

    /*private void scanMediaLibrary(boolean scanRadioLibrary) {
        Log.d(TAG, "[MediaLibraryUpdate]scanMediaLibrary: scan Radio library" + scanRadioLibrary);
        mScanRadioLibrary = scanRadioLibrary;
        if(scanRadioLibrary) {
            mIsRadioScanned = true;
            mControlSession.sendStartMediaLibraryUpdates(mRadioMediaLibraryId, null);
        } else {
            mIsLocalScanned = true;
            mControlSession.sendStartMediaLibraryUpdates(mLocalMediaLibraryId, null);
        }

    }*/

    private void acknowledge(Packet packet) {
        mLink.setLastReceivedSequence(packet.getSequence());
        Packet packetToWrite = new Packet(mLink, Packet.ControlMask.ACK, mLink.getLastReceivedSequence(), (byte) 0x00);
        mIAP2WriteThread.addPacket(packetToWrite);
    }

    @Override
    public void scan() {

    }

    @Override
    public void selectTopLevel(boolean video) {
        if (video) {
            return;
        }

        DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.IOS.ordinal());
        if(mMediaItemCreator != null) {
            mMediaItemCreator.stop();
        }

        mCurrentNode = mMediaTree.resetToTop();
        mMediaItemCreator.setCurrentNode(mCurrentNode);
        mMediaItemCreator.start();
    }

    /**
     * This selects the media item input.
     * Return true if this is a container (e.g., folder).
     *
     * @return True if this is a container.
     */
    @Override
    public boolean select(final MediaItem item) {
        if(item.isTrack) {
            Log.d(TAG, "select: item: " + item.toString());
            mStartPlayingIndex = mCurrentMediaItems.indexOf(item);
//            mPlayRadio = false;
            mIsNewTrack = true;
            return false;
        }

//        Log.d(TAG, "[KEVIN]select: current node: " + mCurrentNode.getCategory() + ", parent node: " + (mCurrentNode.getParent() != null ? mCurrentNode.getParent().getCategory() : null));
        /*if(mCurrentNode.getCategory() == Node.Category.RADIO && (mCurrentNode.getParent() != null && mCurrentNode.getParent().getCategory() == Node.Category.TOP_LEVEL)) {
            // play radio
            Log.d(TAG, "play Apple Music Radio");
            mPlayRadio = true;
            mStartPlayingIndex = mCurrentMediaItems.indexOf(item);
            mPlayed = false;
            return false;
        }*/

        if(mMediaItemCreator != null) {
            mMediaItemCreator.stop();
        }

        mCurrentMediaItems.clear();

        for(Node node : mCurrentNode) {
            if(node.getId().equals(item.id)) {
                /*if(node.getCategory() == Node.Category.RADIO) {
                    if(!mIsRadioScanned) {
                        scanMediaLibrary(true);
                    }
                } else {
                    if(!mIsLocalScanned) {
                        scanMediaLibrary(false);
                    }
                }*/

                mCurrentNode = mMediaTree.next(node);
                break;
            }
        }

        mCurrentNode.getParent().setId(item.id);
        mCurrentNode.getParent().setName(item.title);
        mMediaItemCreator.setSelectedMediaItem(item);
        mMediaItemCreator.setCurrentNode(mCurrentNode);
        mMediaItemCreator.start();

        return true;
    }

    /**
     * Go up a level in the media tree.  Update the current tracks items in
     * the MediaItem Device content on a separate thread.
     *
     */
    @Override
    public void back() {
        if(mMediaItemCreator != null) {
            mMediaItemCreator.stop();
        }

        mCurrentMediaItems.clear();

        mCurrentNode = mMediaTree.previous();
        if(mCurrentNode == null) {
            return;
        }
//        Log.d(TAG, "[KEVIN]back: current node: " + mCurrentNode.getCategory());

        if(mCurrentNode.getCategory() != Node.Category.LIBRARY && mCurrentNode.getCategory() != Node.Category.TOP_LEVEL) {
            MediaItem _item = new MediaItem();
            _item.id = mCurrentNode.getParent().getId();
            _item.title = mCurrentNode.getParent().getName();
            mMediaItemCreator.setSelectedMediaItem(_item);
        }

        mMediaItemCreator.setCurrentNode(mCurrentNode);
        mMediaItemCreator.start();
    }

    @Override
    public void play(boolean auto) {
        DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.IOS.ordinal());

        if (auto) {    // auto play, maybe come back from TV app, reset sample rate
            // Play HID Usage
            mControlSession.sendAccessoryHID(new byte[]{0x20});
            mControlSession.sendAccessoryHID(new byte[]{0x00});

            mIsPlaying = true;

            return;
        }

        if(mIsNewTrack) {
            Log.d(TAG, "play: " + mCurrentMediaItems.get(mStartPlayingIndex));
            List<String> ids = new ArrayList<>();
            for(MediaItem item : mCurrentMediaItems) {
                ids.add(item.id);
            }
            mControlSession.sendPlayMediaLibraryItems(ids, mStartPlayingIndex, mLocalMediaLibraryId);
            /*if(!mPlayRadio) {
                Log.d(TAG, "play: " + mCurrentMediaItems.get(mStartPlayingIndex).toString());
                mControlSession.sendPlayMediaLibraryItems(ids, mStartPlayingIndex, mLocalMediaLibraryId);
            } else {
                // play radio
                Log.d(TAG, "play: play Apple Music Radio: id: " + ids.get(mStartPlayingIndex) + ", media library id: " + mRadioMediaLibraryId);
                mControlSession.playRadio(ids.get(mStartPlayingIndex), 0, mRadioMediaLibraryId);
            }*/
            mIsNewTrack = false;
        } else {
            // Play/Pause HID Usage
            mControlSession.sendAccessoryHID(new byte[]{0x01});
            mControlSession.sendAccessoryHID(new byte[]{0x00});
        }

        mIsPlaying = true;

    }

    @Override
    public void pause(boolean auto) {
        if (!auto) {
            // Play/Pause HID Usage
            mControlSession.sendAccessoryHID(new byte[]{0x01});
            mControlSession.sendAccessoryHID(new byte[]{0x00});
        } else {    // auto pause
            // Pause HID Usage
            mControlSession.sendAccessoryHID(new byte[]{0x40});
            mControlSession.sendAccessoryHID(new byte[]{0x00});
        }

        mIsPlaying = false;

    }

    @Override
    public void next() {
        mControlSession.sendAccessoryHID(new byte[]{0x02});
        mControlSession.sendAccessoryHID(new byte[]{0x00});
    }

    @Override
    public void previous() {
        mControlSession.sendAccessoryHID(new byte[]{0x04});
        mControlSession.sendAccessoryHID(new byte[]{0x00});
    }

    @Override
    public void fastForward() {
        mControlSession.sendAccessoryHID(new byte[]{0x02});
    }

    @Override
    public void rewind() {
        mControlSession.sendAccessoryHID(new byte[]{0x04});
    }

    @Override
    public void seek(int pos) {

    }

    @Override
    public void repeat() {
        mControlSession.sendAccessoryHID(new byte[]{0x10});
        mControlSession.sendAccessoryHID(new byte[]{0x00});
    }

    @Override
    public void repeatDown() {
        mControlSession.sendAccessoryHID(new byte[]{0x10});
    }

    @Override
    public void touchUp() {
        mControlSession.sendAccessoryHID(new byte[]{0x00});
    }

    @Override
    public void shuffle() {
        mControlSession.sendAccessoryHID(new byte[]{0x08});
        mControlSession.sendAccessoryHID(new byte[]{0x00});
    }

    @Override
    public void shuffleDown() {
        mControlSession.sendAccessoryHID(new byte[]{0x08});
    }

    @Override
    public void playPauseButtonDown() {
        mControlSession.sendAccessoryHID(new byte[]{0x01});
    }

    @Override
    protected byte[] getAlbumArt(com.jht.chimera.service.usbmedia.mtpums.lib.media.Track track) {
        return new byte[0];
    }

    @Override
    public String getUri(MediaItem item) {
        return null;
    }

    @Override
    public void disconnect() {
        super.disconnect();

        mConnection.close();
        mIAP2ReadThread.setConnected(false);

        if(mIAP2ReadThread != null) {
            mIAP2ReadThread.stop();
        }
        if(mIAP2WriteThread != null) {
            mIAP2WriteThread.stop();
        }

        if(mMediaItemCreator != null) {
            mMediaItemCreator.stop();
        }


        mDataMatrix.finish();
    }
}
