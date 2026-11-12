package com.jht.chimera.service.usbmedia.ios.lib.media;

import java.util.ArrayList;

public class Node extends ArrayList<Node> {
    private Category mCategory;
    private String mId;
    private String mName;
    private Node mParent;

    public enum Category {
        TOP_LEVEL((byte) 0x00),
        LIBRARY((byte) 0x01),
        PLAYLIST((byte) 0x02),
        ARTIST((byte) 0x03),
        ALBUM((byte) 0x04),
        GENRE((byte) 0x05),
        AUDIO_TRACK((byte) 0x06),
        RADIO((byte) 0x07),
        AUDIO_BOOK((byte) 0x08),
        PODCAST((byte) 0x09),
        COMPOSER((byte) 0x0A),
        COMPILATION((byte) 0x0B);

        private byte value;

        Category(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }

        public static Category getCategory(byte value) {
            Category category = null;
            for (Category c : Category.values()) {
                if (c.getValue() == value) {
                    category = c;
                    break;
                }
            }

            return category;
        }
    }

    public Node() {
    }

    public Node(Category category, String id, String name, Node parent) {
        this.mCategory = category;
        this.mId = id;
        this.mName = name;
        this.mParent = parent;
    }

    public Node(Category category, String id, String name) {
        this.mCategory = category;
        this.mId = id;
        this.mName = name;
    }

    public Category getCategory() {
        return mCategory;
    }

    public void setCategory(Category category) {
        this.mCategory = category;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public Node getParent() {
        return mParent;
    }

    public void setParent(Node parent) {
        this.mParent = parent;
    }


}
