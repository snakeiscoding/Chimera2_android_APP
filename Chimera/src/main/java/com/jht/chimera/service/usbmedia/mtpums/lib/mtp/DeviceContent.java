package com.jht.chimera.service.usbmedia.mtpums.lib.mtp;

import android.mtp.MtpConstants;
import android.mtp.MtpObjectInfo;
import android.util.Log;

import java.util.ArrayList;

/**
 * Stores a tree of the content on a device.
 * 
 * @author Blong.Thor
 */
public class DeviceContent {
    private static final String TAG = "DeviceContent";
    private ArrayList<ContentInfo> mContentInfos;
    
    /**
     * Holds information pertaining to an item.  
     * 
     * @author Blong.Thor
     */
    public class ContentInfo {
        private String name;
        private int id;
        private int handle;
        private int format;
        private int parentId;
        private int size;
        
        /**
         * Constructor
         * 
         * @param n name
         * @param i storage id
         * @param h handle
         * @param f format
         * @param p parent id
         */
        public ContentInfo(String n, int i, int h, int f, int p, int s) {
            name = n;
            id = i;
            handle = h;
            format = f;
            parentId = p;
            size = s;
        }
        
        /**
         * Prevent usage of default constructor.
         */
        @SuppressWarnings("unused")
        private ContentInfo() { }
        
        /**
         * Get item name
         * @return name
         */
        public String getName()
        {
            return name;
        }
        
        /**
         * Get storage ID
         * @return storage id
         */
        public int getId()
        {
            return id;
        }
        
        /**
         * Get handle
         * @return handle
         */
        public int getHandle()
        {
            return handle;
        }
        
        /**
         * Get format
         * @return format
         */
        public int getFormat()
        {
            return format;
        }
        
        /**
         * Get parent ID
         * @return parent id
         */
        public int getParentId()
        {
            return parentId;
        }
        
        /**
         * Get the object size
         * @return size in bytes
         */
        public int getSize() {
            return size;
        }
    }
    
    /**
     * Constructor
     */
    public DeviceContent() {
        mContentInfos = new ArrayList<>();
    }
	
    /**
     * Add an item to the device content tree.
     *
     */
    public ContentInfo addContent(MtpObjectInfo mtpObjectInfo) {
    	int format = mtpObjectInfo.getFormat();
    	
    	if(mtpObjectInfo.getName().endsWith(".m4a")) { //NON-NLS
            format = MtpConstants.FORMAT_AAC;
        } else if(mtpObjectInfo.getName().endsWith(".m4v")) { //NON-NLS
            format = MtpConstants.FORMAT_MP4_CONTAINER;
        }

        ContentInfo info = null;
        try {
            info = new ContentInfo(
                    mtpObjectInfo.getName(),
                    mtpObjectInfo.getStorageId(),
                    mtpObjectInfo.getObjectHandle(),
                    format,
                    mtpObjectInfo.getParent(),
                    mtpObjectInfo.getCompressedSize());
            mContentInfos.add(info);
        } catch(Exception e) {
            Log.e(TAG, "addContent: video " + mtpObjectInfo.getName() + " not supported.");
        }
        
        return info;
        
    }
    
    public void removeContent(ContentInfo info) {
    	mContentInfos.remove(info);
    }
    
    /**
     * Remove all content for the list.
     */
    public void clearList() {
        mContentInfos.removeAll(mContentInfos);
    }
    
    public int getSize() {
        return mContentInfos.size();
    }
    
    /**
     * Get the music files.
     * 
     * @return list of music
     */
    public ArrayList<ContentInfo> getMusicList() {
        ArrayList<ContentInfo> musicList = new ArrayList<ContentInfo>();
        
        for(int i = 0; i < mContentInfos.size(); i++) {
            if(isMusic(mContentInfos.get(i))) {
                musicList.add(mContentInfos.get(i));
            }
        }

        return musicList;
    }
    
    /**
     * Get the video files.
     * 
     * @return list of video
     */
    public ArrayList<ContentInfo> getVideoList() {
        ArrayList<ContentInfo> videoList = new ArrayList<>();
        
        for(int i = 0; i < mContentInfos.size(); i++) {
            if(isVideo(mContentInfos.get(i))) {
                videoList.add(mContentInfos.get(i));
            }
        }

        return videoList;
    }
    
    /**
     * Return string representation of the content.
     * 
     * @return content string
     */
    public String toString() {
        String contentListString = "";
        for(int i = 0; i < mContentInfos.size(); i++) {
            contentListString += mContentInfos.get(i).getName();
            contentListString += "\r\n";
        }
        return contentListString;
    }
    
    /**
     * Check if content is music.
     * 
     * @param c
     * @return true if content is music
     */
    public boolean isMusic(ContentInfo c) {
        return (MtpConstants.FORMAT_AAC == c.getFormat()
                || MtpConstants.FORMAT_AIFF == c.getFormat()
                || MtpConstants.FORMAT_OGG == c.getFormat()
                || MtpConstants.FORMAT_MP3 == c.getFormat()
                || MtpConstants.FORMAT_FLAC == c.getFormat());
    }
    
    /**
     * Check if content is video.
     * 
     * @param c
     * @return true if content is video
     */
    public boolean isVideo(ContentInfo c) {
        return (MtpConstants.FORMAT_AVI == c.getFormat()
                || MtpConstants.FORMAT_MP4_CONTAINER == c.getFormat()
                || MtpConstants.FORMAT_MPEG == c.getFormat()
                || MtpConstants.FORMAT_UNDEFINED_VIDEO == c.getFormat());
    }
    
}
