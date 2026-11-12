package com.jht.chimera.service.usbmedia.mtpums.lib.mtp;

import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import android.mtp.MtpStorageInfo;
import android.util.Log;

import com.jht.chimera.service.usbmedia.mtpums.lib.mtp.DeviceContent.ContentInfo;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles managing MTP devices and device content.  Implements methods
 * to enumerate music content and video content.  Retrieves content
 * in small packets to avoid copying complete media files to the host
 * for playback.  
 * 
 * Dependencies:
 * 
 * ClientServer - This class is used to host a local server to transport
 * the media content to the rendering end point.
 * 
 * DeviceContent - This class stores a tree of the directory, file, and
 * media structure from the MTP device.
 * 
 * @author Blong.Thor
 */
public class MtpController {
    private static final String TAG = "MtpController";
    private static final int SOCKET_ADDRESS = 8050;

    private Lock lockReadDevice = new ReentrantLock();
    private DeviceContent mDeviceContent;
    private ContentServer mLocalContentServer;
    private MtpDevice mMtpDevice;
    private MtpMediaController.OnScanCompletedListener mOnScanCompletedListener;

    MtpController(MtpDevice mtpDevice) {
        mMtpDevice = mtpDevice;
        mDeviceContent = new DeviceContent();

        startContentServer();
    }

    public void setOnScanCompletedListener(MtpMediaController.OnScanCompletedListener l) {
        mOnScanCompletedListener = l;
    }

    public void close() {
        stopContentServer();
    }

    public boolean isDeviceUnLocked() {
    	return getStorageList() != null;
    }

    private List<MtpStorageInfo> getStorageList() {
        if(mMtpDevice == null) {
            return null;
        }

        int[] storageIds = mMtpDevice.getStorageIds();
        if(storageIds == null) {
            Log.d(TAG, "getStorageList: fetching storage IDs fails");
            return null;
        }

        List<MtpStorageInfo> storageList = new ArrayList<>();
        for(int id : storageIds) {
            MtpStorageInfo info = mMtpDevice.getStorageInfo(id);
            if (info == null) {
                Log.d(TAG, "getStorageList: fetching storage info fails");
            } else {
                storageList.add(info);
            }

        }

        if (storageList.size() > 0) {
            return storageList;
        } else {
            return null;
        }



    }
    
    private boolean mStopContentListCreation = false;
    synchronized void stopContentListCreation()
    {
    	mStopContentListCreation = true;
    }
    private synchronized boolean shouldStopContentListCreation() {
    	return mStopContentListCreation;
    }

    /**
     * Create content list of folders and files stored on the MTP device.
     */
    public void createContentList() {
        Log.d(TAG, "createContentList: ");
        long time1, time2;
        time1 = System.currentTimeMillis();
    	mStopContentListCreation = false;

		@NonNls String[] excludeDirs = {
                "samsung",
                "ringtones",
                "alarms",
                "noifications",
                "pictures",
                "dcim",
                "books",
                "temp",
                "tmp",
                "cache",
                "cachedir",
                "log",
                "spool",
                "lost.dir",
                "metadata",
                "database",
                "screenshot",
                "__macosx",
                "config",
                "photo",
                "stacktrace",
                "theme",
                "wallpaper",
                "default"
		};

        mDeviceContent.clearList();
        List<MtpStorageInfo> storageInfo = getStorageList();
        if(storageInfo != null) {
        	for(MtpStorageInfo storage : storageInfo) {
        		if(shouldStopContentListCreation()) {
                    return;
                }
	            
	            // Root storage
	            int storageId = storage.getStorageId();
	            
	            //mDeviceContent.addContent(storage.getDescription(), storageId, 0, 0, 0, 0);
                Log.d(TAG, "createContentList: storage: " + storage.getDescription());
                long counter = 0;  // TODO remove me
	            
	            // Search entire tree
                List<MtpObjectInfo> objInfoList = getObjectList(storageId, 0);
	            if(objInfoList != null) {
                    Log.e(TAG, "objInfoList: " + objInfoList.size());  // TODO remove me
	            	while(objInfoList.size() > 0) {
	            		if(shouldStopContentListCreation()) {
                            return;
                        }

			            MtpObjectInfo info = objInfoList.get(0);
                        Log.i(TAG, "info = " + info.getName() + " | " + (counter++)); // TODO remove me
			            switch(info.getFormat()) {
                            case MtpConstants.FORMAT_ASSOCIATION:
                                Log.d(TAG, "folder name = " + info.getName());
                                // Filtering by folder names results in a lot of missed media.
                                // This is especially true in other cultures as folder names may
                                // not even be in English.  The filtering was done to speed up performance,
                                // but we sped that up by reading the header of every file and then reading the
                                // whole file in a second run to get the track time.

                                boolean ignore = false;
                                String folder = info.getName().toLowerCase();
                                if(folder.startsWith(".") || folder.startsWith("com.")) {
                                    objInfoList.remove(0);
                                    continue;
                                }
                                for(String dir : excludeDirs) {
                                    if(folder.contains(dir)) {
                                        ignore = true;
                                        break;
                                    }
                                }
                                if(!ignore) {
                                    List<MtpObjectInfo> folderContents = null;
                                    try {
                                        MtpMediaController.lockMTP();
    //				        			Log.d(TAG, "Getting objects for folder " + info.getName());
                                        folderContents = getObjectList(info.getStorageId(), info.getObjectHandle());
                                    } finally {
                                        MtpMediaController.unlockMTP();
                                    }
                                    if(folderContents != null) {
                                        Log.e(TAG, "==> ADD folder name = " + info.getName());
                                        objInfoList.addAll(folderContents);
                                    }
                                }
                                break;
    			            case MtpConstants.FORMAT_AAC:
    			            case MtpConstants.FORMAT_AIFF:
    			            case MtpConstants.FORMAT_OGG:
                            case MtpConstants.FORMAT_MP3:
                            case MtpConstants.FORMAT_WMA:
                            case MtpConstants.FORMAT_FLAC:
                                Log.d(TAG, "createContentList: audio name: " + info.getName() + ", handle: " + info.getObjectHandle());
//                                mediaController.addMusic(mDeviceContent.addContent(info));
                                mOnScanCompletedListener.onScanCompleted(mDeviceContent.addContent(info), true);
                                break;
                            case MtpConstants.FORMAT_AVI:
                            case MtpConstants.FORMAT_MP4_CONTAINER:
                            case MtpConstants.FORMAT_MPEG:
                            case MtpConstants.FORMAT_WMV:
                                Log.d(TAG, "createContentList: video name: " + info.getName());
                                addVideo(info);
                                break;
                            default:
                                if(info.getName().toLowerCase().endsWith(".m4a")) {
                                    Log.d(TAG, "createContentList: audio name: " + info.getName() + ", handle: " + info.getObjectHandle());
//                                    mediaController.addMusic(mDeviceContent.addContent(info));
                                    mOnScanCompletedListener.onScanCompleted(mDeviceContent.addContent(info), true);
                                } else if(info.getName().toLowerCase().endsWith(".m4v")) {
                                    Log.d(TAG, "createContentList: video name: " + info.getName() + ", handle: " + info.getObjectHandle());
                                    addVideo(info);
                                } else if(info.getName().toLowerCase().endsWith(".mkv")) {
                                    Log.d(TAG, "createContentList: video name: " + info.getName() + ", handle: " + info.getObjectHandle());
                                    addVideo(info);
                                } else {
                                    Log.d(TAG, "unknown object = " + info.getName() + ", " + info.getFormat());
                                }
                                break;
		                }
		                objInfoList.remove(0);
		            }
	            }
            }
        } else {
            Log.d(TAG, "No Devices");
            // The device may be locked
        }
        time2 = System.currentTimeMillis();
        Log.d(TAG, "createContentList: spend " + ((time2 - time1) / 1000) + " seconds to complete. ");
        mOnScanCompletedListener.onScanAllCompleted();
    }

    private List<MtpObjectInfo> getObjectList(int storageId, int objectHandle) {
        if(mMtpDevice == null) {
            return null;
        }

        if (objectHandle == 0) {
            // all objects in root of storage
            objectHandle = 0xFFFFFFFF;
        }
        int[] handles = mMtpDevice.getObjectHandles(storageId, 0, objectHandle);
        if (handles == null) {
            return null;
        }

        List<MtpObjectInfo> objectList = new ArrayList<>();
        for(int handle : handles) {
            MtpObjectInfo info = mMtpDevice.getObjectInfo(handle);
            if (info == null) {
                Log.w(TAG, "getObjectList: fetching object info fails");
            } else {
                objectList.add(info);
            }
        }

        return objectList;
    }
    
    private void addVideo(MtpObjectInfo info) {
    	ContentInfo i = mDeviceContent.addContent(info);
    	if(i == null) {
    	    return;
        }

//        controller.addVideo(i);
        mOnScanCompletedListener.onScanCompleted(i, false);
    }
    
    /**
     * Format content to a string.
     */
    public String toString() {
        return mDeviceContent.toString();
    }

    /**
     * Get the music list info.
     * 
     * @return list of music info
     */
    public ArrayList<ContentInfo> getMusicList() {
        return mDeviceContent.getMusicList();
    }

    /**
     * Get the video list info.
     * 
     * @return list of video info
     */
    public ArrayList<ContentInfo> getVideoList() {
        return mDeviceContent.getVideoList();
    }
    
    /**
     * Get the file size of the object in bytes.
     * 
     * @param handle
     * @return size of the object
     */
    public int getObjectSize(int handle) {
        MtpObjectInfo objectInfo = mMtpDevice.getObjectInfo(handle);
        if(null == objectInfo) {
            return 0;
        }
        
        return objectInfo.getCompressedSize();
    }

    /**
     * Create the content server.  Listen for media request from the content 
     * server.  When request is received search for the request.  If media 
     * is found then start streaming the media.  Events include media request, 
     * media position change, media stopped, error
     */
    private void startContentServer() {
        // Start server thread
        mLocalContentServer = new ContentServer();
        mLocalContentServer.init(SOCKET_ADDRESS);
        mLocalContentServer.setListener(mContentServerListener);
    }

    public void setFileName(String name) {
        mLocalContentServer.setFileName(name);
    }

    public void setHandle(int handle) {
        mLocalContentServer.setHandle(handle);
    }
    
    /**
     * Restart the content server
     * 
     */
    public void restartContentServer() {
    	stopContentServer();
    	startContentServer();
    }
    
    /**
     * Stop the content server
     * 
     */
    public void stopContentServer() {
    	if(null != mLocalContentServer) {
    		mLocalContentServer.stop();
    		mLocalContentServer = null;
    	}
    }
    
    /**
     * Handle requests from the content server.
     * 
     */
    ContentServerListener mContentServerListener = new ContentServerListener() {
    	int mLastHandle = 0;
    	int mHandle = 0;
    	int mSize = 0;
        int mTotalSizeRead = 0;
        
        MtpDevice mDevice;
        final int READ_BLOCK = 16384 * 10;
        
        byte[] mData = null;
        byte[] mStandardBuffer = new byte[READ_BLOCK];
    	
    	/**
         * Set media for request
         * 
         */
        @Override
        public boolean mediaRequest(String mediaName)
        {
        	return mediaRequest(mediaName, 0);
        }
        
        
        /**
         * Set media for request
         * 
         */
        @Override
        public boolean mediaRequest(String mediaName, int offset) {
            String objectName = "";
            int size = 0;
            int handle = 0;
            
            if(offset == 0) {
            	mTotalSizeRead = 0;
            }
            
        	ArrayList<ContentInfo> musicList = getMusicList();
            for(int i = 0; i < musicList.size(); i++) {
                if(musicList.get(i).getName().compareTo(mediaName) == 0) {
                    objectName = mediaName;
                    handle = musicList.get(i).getHandle();
                    size = musicList.get(i).getSize();
                    break;
                }
            }
            
            if(handle == 0) {
                ArrayList<ContentInfo> videoList = getVideoList();
                for(int i = 0; i < videoList.size(); i++) {
                    if(videoList.get(i).getName().compareTo(mediaName) == 0) {
                        objectName = mediaName;
                        handle = videoList.get(i).getHandle();
                        size = videoList.get(i).getSize();
                        break;
                    }
                }
            }
            
            if(handle != 0 && mLastHandle != handle) {
            	// New media
                Log.d(TAG, "Found handle, " + objectName + ", handle: " + handle + ", size: " + size);
                
                mHandle = handle;
                mSize = size;
                mDevice = mMtpDevice;
                mTotalSizeRead = 0;
                mLastHandle = handle;
            } else if(mLastHandle == handle && handle != 0) {
            	// Same media no updates
            	
            }
            
            // Update the total read size
            if(offset >= 0 && offset < mSize) {
        		Log.d(TAG, "new offset " + offset);
        		mTotalSizeRead = offset;
        	} else {
        		Log.d(TAG, "offset >= mSize " + offset + " >= " + mSize);
        		mTotalSizeRead = size;
        	}

            return handle > 0;
        }

        @Override
        public boolean mediaRequest(int handle, int offset) {
            boolean isMusic = false;
            String objectName = "";
            int size = 0;

            if(offset == 0) {
                mTotalSizeRead = 0;
            }

            ArrayList<ContentInfo> musicList = getMusicList();
            for(int i = 0; i < musicList.size(); i++) {
                if(musicList.get(i).getHandle() == handle) {
                    isMusic = true;
                    objectName = musicList.get(i).getName();
//                    handle = musicList.get(i).getHandle();
                    size = musicList.get(i).getSize();
                    break;
                }
            }

            if(!isMusic) {
                ArrayList<ContentInfo> videoList = getVideoList();
                for(int i = 0; i < videoList.size(); i++) {
                    if(videoList.get(i).getHandle() == handle) {
                        objectName =  musicList.get(i).getName();
//                        handle = videoList.get(i).getHandle();
                        size = videoList.get(i).getSize();
                        break;
                    }
                }
            }

            if(handle != 0 && mLastHandle != handle) {
                // New media
                Log.d(TAG, "Found handle, " + objectName + ", handle: " + handle + ", size: " + size);

                mHandle = handle;
                mSize = size;
                mDevice = mMtpDevice;
                mTotalSizeRead = 0;
                mLastHandle = handle;
            } else if(mLastHandle == handle && handle != 0) {
                // Same media no updates

            }

            // Update the total read size
            if(offset >= 0 && offset < mSize) {
                Log.d(TAG, "new offset " + offset);
                mTotalSizeRead = offset;
            } else {
                Log.d(TAG, "offset >= mSize " + offset + " >= " + mSize);
                mTotalSizeRead = size;
            }

            return handle > 0;
        }

        /**
         * The media delivery stopped
         * 
         */
        @Override
        public void mediaStopped()
        {
            Log.d(TAG, "mediaStopped()");
        }
        
        /**
         * Get the data for the media object.  The starting point is set by
         * calling the mediaRequest() method with an offset parameter.
         * 
         * @return a byte buffer sized accordingly
         */
        @Override
        public byte[] getMediaData() {
            lockReadDevice.lock();

        	int readSize = (mSize - mTotalSizeRead) >= READ_BLOCK ? READ_BLOCK : (mSize - mTotalSizeRead);
        	if(READ_BLOCK == readSize) {
        		mData = mStandardBuffer;
        	} else {
        		// Create temporary custom size buffer (usually for last part of the object)
        		mData = new byte[readSize];
        	}
        	
        	//Log.d(TAG, "readSize: " + readSize + " mTotalSizeRead: " + mTotalSizeRead);
        	if(0 < readSize) {
        		try {
        			MtpMediaController.lockMTP();
                    mDevice.getPartialObject(mHandle, mTotalSizeRead, mData.length, mData);
        		} catch (IOException e) {
                    Log.w(TAG, e);
                } finally {
        			MtpMediaController.unlockMTP();
        		}
        		mTotalSizeRead += readSize;
        	}

            lockReadDevice.unlock();

            return mData;
        }

        /**
         * Check if the media object data retrieval is complete.
         */
        @Override
        public boolean isFinish() {
        	return mTotalSizeRead >= mSize;
        }


		@Override
		public int getMediaSize() 
		{
			return mSize;
		}
    };

    /**
     * Get the handle for a media file name.
     * 
     * @param mediaName
     * @return the file handle
     */
    private int getHandle(String mediaName) {
        int handle = 0;

        ArrayList<ContentInfo> musicList = getMusicList();
        for (int i = 0; i < musicList.size(); i++) {
            if (musicList.get(i).getName().compareTo(mediaName) == 0) {
                //Log.d(TAG, "Found music match: " + mediaName);
                handle = musicList.get(i).getHandle();
                break;
            }
        }

        if (handle == 0) {
            ArrayList<ContentInfo> videoList = getVideoList();
            for (int i = 0; i < videoList.size(); i++) {
                if (videoList.get(i).getName().compareTo(mediaName) == 0) {
                    //Log.d(TAG, "Found video match: " + mediaName);
                    handle = videoList.get(i).getHandle();
                    break;
                }
            }
        }

        return handle;
    }

    /**
     * Get the a partial buffer from a file at the specified offest.
     * 
     * @param mediaName
     * @param offset
     * @param data byte array
     * @return the size the of file
     */
    public int getPartialBuffer(String mediaName, int offset, byte[] data) {
        lockReadDevice.lock();

        int handle = getHandle(mediaName);
        int size = getObjectSize(handle);
        int result = 0;

        if(0 < handle && 0 < size && 0 <= offset) {
            MtpDevice device = mMtpDevice;
	        
	        byte[] temp = null;
	        if(offset + data.length > size) {
	        	temp = new byte[size - offset];
        		/*try {
        			MtpMediaController.lockMTP();
        			//getPartialObjectSync(device, handle, offset, temp);
        		} finally {
        			MtpMediaController.unlockMTP();
        		}*/
	        	result = temp.length;
	        	System.arraycopy(temp, 0, data, 0, temp.length);
	        } else {
        		try {
        			MtpMediaController.lockMTP();
                    device.getPartialObject(handle, offset, data.length, data);
        		} catch (IOException e) {
                    Log.w(TAG, e);
                } finally {
        			MtpMediaController.unlockMTP();
        		}
	        	result = data.length;
	        }
        }

        lockReadDevice.unlock();

        return result;
    }

    public int getPartialBuffer(int handle, int offset, byte[] data) {
        lockReadDevice.lock();

        int size = getObjectSize(handle);
        int result = 0;

        if(0 < handle && 0 < size && 0 <= offset) {
            MtpDevice device = mMtpDevice;

            byte[] temp = null;
            if(offset + data.length > size) {
                Log.d(TAG, "getPartialBuffer: (" + size  + "-" +  offset + ") = " + (size - offset));
                temp = new byte[size - offset];
        		/*try {
        			MtpMediaController.lockMTP();
        			//getPartialObjectSync(device, handle, offset, temp);
        		} finally {
        			MtpMediaController.unlockMTP();
        		}*/
                result = temp.length;
                System.arraycopy(temp, 0, data, 0, temp.length);
            } else {
                try {
                    MtpMediaController.lockMTP();
                    device.getPartialObject(handle, offset, data.length, data);
                } catch (IOException e) {
                    Log.w(TAG, e);
                } finally {
                    MtpMediaController.unlockMTP();
                }
                result = data.length;
            }
        }

        lockReadDevice.unlock();

        return result;
    }

    /**
     * Get the a partial buffer from a file from the start of the file.
     * 
     * @param mediaName
     * @param data byte array
     * @return the size the of file
     */
    public int getPartialBufferFromStart(String mediaName, byte[] data) {
        return getPartialBuffer(mediaName, 0, data);
    }

    public int getPartialBufferFromStart(int handle, byte[] data) {
        return getPartialBuffer(handle, 0, data);
    }

    /**
     * Get the a partial buffer from a file at the end of the file.
     * 
     * @param mediaName
     * @param data byte array
     * @return the size the of file
     */
//    public int getPartialBufferFromEnd(String mediaName, byte[] data) {
//        lockReadDevice.lock();
//        int handle = getHandle(mediaName);
//        if(handle == 0) {
//            return 0;
//        }
//
//        int offset = getObjectSize(handle) - data.length;
//
//        int size = getPartialBuffer(mediaName, offset, data);
//
//        lockReadDevice.unlock();
//        return size;
//    }
}
