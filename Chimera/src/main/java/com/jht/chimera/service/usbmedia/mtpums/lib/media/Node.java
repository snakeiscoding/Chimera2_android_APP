package com.jht.chimera.service.usbmedia.mtpums.lib.media;

import java.util.ArrayList;

public class Node extends ArrayList<Node> {
    public enum Category {
        TOP_LEVEL, ARTIST, ALBUM, GENRE, AUDIO_TRACK, VIDEO_TRACK
    }

    private Category mCategory;
    private String mId;
    private String mName;
    private Node mParent;


    public Node(Category category, String id, String name) {
        this.mCategory = category;
        this.mId = id;
        this.mName = name;
    }

    public Category getCategory() {
        return mCategory;
    }

    public void setCategory(Category category) {
        mCategory = category;
    }
    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public Node getParent() {
        return mParent;
    }

    public void setParent(Node parent) {
        mParent = parent;
    }


}
