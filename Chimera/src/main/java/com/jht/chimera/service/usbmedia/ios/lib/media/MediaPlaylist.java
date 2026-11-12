package com.jht.chimera.service.usbmedia.ios.lib.media;

import java.util.ArrayList;
import java.util.List;

public class MediaPlaylist {
    private String id;
    private String name;
    private String parentId;
    private boolean isGeniusMix;
    private boolean isFolder;
    private int fileTransferId;
    private boolean isAppleMusicRadio;
    private List<String> mediaItemIds = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public boolean isGeniusMix() {
        return isGeniusMix;
    }

    public void setGeniusMix(boolean geniusMix) {
        isGeniusMix = geniusMix;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public int getFileTransferId() {
        return fileTransferId;
    }

    public void setFileTransferId(int fileTransferId) {
        this.fileTransferId = fileTransferId;
    }

    public boolean isAppleMusicRadio() {
        return isAppleMusicRadio;
    }

    public void setAppleMusicRadio(boolean appleMusicRadio) {
        isAppleMusicRadio = appleMusicRadio;
    }

    public List<String> getMediaItemIds() {
        return mediaItemIds;
    }

    public void addMediaItemId(String mediaItemId) {
        mediaItemIds.add(mediaItemId);
    }
}
