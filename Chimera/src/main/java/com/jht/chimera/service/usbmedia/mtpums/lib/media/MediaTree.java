package com.jht.chimera.service.usbmedia.mtpums.lib.media;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.context.ContextUtil;

public class MediaTree {
    private TreeBase mCurrentTree;
    private int mLevel;

    public MediaTree() {
        mCurrentTree = new TopLevel();
    }

    public Node getNode() {
        return mCurrentTree.get();
    }

    public Node next(Node node) {
        Node next;
        if(node == null || node.getCategory() == Node.Category.TOP_LEVEL) {
            next = resetToTop();
            mLevel = 0;
        } else {
            next = mCurrentTree.next(node, mLevel);
            mLevel++;
        }

        return next;
    }

    public Node previous() {
        mLevel--;
        return mCurrentTree.previous();

    }

    public Node[] getChild() {
        return mCurrentTree.getChild();
    }

    public Node resetToTop() {
        mCurrentTree = new TopLevel();
        return mCurrentTree.get();
    }

    public int getLevel() {
        return mLevel;
    }


    abstract class TreeBase {

        protected TreeBase lastTree;

        abstract protected Node get();
        abstract protected Node[] getChild();
        abstract protected Node next(Node node);
        abstract protected Node next(Node node, int level);
        abstract protected Node previous();
        protected void setParent(Node current, Node parent) {
            current.setParent(parent);
        }


    }

    class TopLevel extends TreeBase {
        Node node = new Node(Node.Category.TOP_LEVEL, "0", "TopLevel"); //NON-NLS
//        Node child = new Node(Node.Category.ARTIST, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_artists_tc));
        Node[] child = {
                new Node(Node.Category.ARTIST, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_artists_tc)),
                new Node(Node.Category.ALBUM, "1", ContextUtil.getApplicationContext().getString(R.string.med_app_albums_tc)),
                new Node(Node.Category.GENRE, "2", ContextUtil.getApplicationContext().getString(R.string.med_app_genre_tc)),
                new Node(Node.Category.AUDIO_TRACK, "3", ContextUtil.getApplicationContext().getString(R.string.songs_tc))
        };

        public TopLevel() {
            for(Node n : child) {
                node.add(n);
            }
        }

        @Override
        protected Node get() {
            return node;
        }

        @Override
        protected Node[] getChild() {
            return child;
        }

        @Override
        protected Node next(Node node) {
            Node next = null;
            switch(node.getCategory()) {
                case ARTIST:
//                    mCurrentTree = new ArtistTree(this, node);
                    mCurrentTree = new ArtistTree(this);
                    next = mCurrentTree.get();
                    break;
                case ALBUM:
                    mCurrentTree = new AlbumTree(this);
                    next = mCurrentTree.get();
                    break;
                case GENRE:
                    mCurrentTree = new GenreTree(this);
                    next = mCurrentTree.get();
                    break;
                case AUDIO_TRACK:
                    mCurrentTree = new AudioTree(this);
                    next = mCurrentTree.get();
                    break;
                case VIDEO_TRACK:
                    break;
                default:
                    next = node;
            }
            return next;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case ARTIST:
//                    mCurrentTree = new ArtistTree(this, node);
                    mCurrentTree = new ArtistTree(this);
                    next = mCurrentTree.get();
                    break;
                case ALBUM:
                    mCurrentTree = new AlbumTree(this);
                    next = mCurrentTree.get();
                    break;
                case GENRE:
                    mCurrentTree = new GenreTree(this);
                    next = mCurrentTree.get();
                    break;
                case AUDIO_TRACK:
                    mCurrentTree = new AudioTree(this);
                    next = mCurrentTree.get();
                    break;
                case VIDEO_TRACK:
                    break;
                default:
                    next = node;
            }
            return next;
        }

        @Override
        protected Node previous() {
            return null;
        }
    }

    class ArtistTree extends TreeBase {
        Node node = new Node(Node.Category.ARTIST, "0", "");
        Node[] child = {
                new Node(Node.Category.ALBUM, "0", ""),
                new Node(Node.Category.AUDIO_TRACK, "1", "")
        };

        public ArtistTree(TreeBase last) {
            setParent(node, last.get());
            lastTree = last;
        }

        @Override
        protected Node get() {
            return node;
        }

        @Override
        protected Node[] getChild() {
            return child;
        }

        @Override
        protected Node next(Node node) {
            Node next = null;
            switch(node.getCategory()) {
                case ARTIST:
                    mCurrentTree = new AlbumTree(this);
                    next = mCurrentTree.get();
                    break;
            }

            return next;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case ARTIST:
                    if(level == 0) {
                        mCurrentTree = new ArtistTree(this);
                        next = mCurrentTree.get();
                    } else {
                        mCurrentTree = new AlbumTree(this);
                        next = mCurrentTree.get();
                    }
                    break;
            }
            return next;
        }

        @Override
        protected Node previous() {
            mCurrentTree = lastTree;
            return lastTree.get();
        }
    }

    class AlbumTree extends TreeBase {
        Node node = new Node(Node.Category.ALBUM, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_albums_tc));
        Node[] child = {
                new Node(Node.Category.AUDIO_TRACK, "0", ContextUtil.getApplicationContext().getString(R.string.songs_tc))
        };

        public AlbumTree(TreeBase last) {
            setParent(node, last.get());
            lastTree = last;
        }

        @Override
        protected Node get() {
            return node;
        }

        @Override
        protected Node[] getChild() {
            return child;
        }

        @Override
        protected Node next(Node node) {
            Node next = null;
            switch(node.getCategory()) {
                case ALBUM:
                    mCurrentTree = new AlbumTree(this);
                    next = mCurrentTree.get();
                    break;
            }
            return next;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case ALBUM:
                    if(level == 0) {
                        mCurrentTree = new AlbumTree(this);
                        next = mCurrentTree.get();
                    } else {
                        mCurrentTree = new AudioTree(this);
                        next = mCurrentTree.get();
                    }
                    break;
            }
            return next;
        }

        @Override
        protected Node previous() {
            mCurrentTree = lastTree;
            return lastTree.get();
        }
    }

    class GenreTree extends TreeBase {
        Node node = new Node(Node.Category.GENRE, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_genre_tc));
        Node[] child = {
                new Node(Node.Category.ARTIST, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_artists_tc)),
                new Node(Node.Category.ALBUM, "1", ContextUtil.getApplicationContext().getString(R.string.med_app_albums_tc))
        };

        public GenreTree(TreeBase last) {
            setParent(node, last.get());
            lastTree = last;
        }

        @Override
        protected Node get() {
            return node;
        }

        @Override
        protected Node[] getChild() {
            return child;
        }

        @Override
        protected Node next(Node node) {
            Node next = null;
            switch(node.getCategory()) {
                case GENRE:
                    mCurrentTree = new AlbumTree(this);
                    next = mCurrentTree.get();
                    break;
                case ARTIST:
                    mCurrentTree = new ArtistTree(this);
                    next = mCurrentTree.get();
                    break;
                case ALBUM:
                    mCurrentTree = new AlbumTree(this);
                    next = mCurrentTree.get();
                    break;
            }
            return next;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case GENRE:
                    if(level == 0) {
                        mCurrentTree = new GenreTree(this);
                        next = mCurrentTree.get();
                    } else {
                        mCurrentTree = new ArtistTree(this);
                        next = mCurrentTree.get();
                    }
            }
            return next;
        }

        @Override
        protected Node previous() {
            mCurrentTree = lastTree;
            return lastTree.get();
        }
    }

    class AudioTree extends TreeBase {
        Node node = new Node(Node.Category.AUDIO_TRACK, "", ContextUtil.getApplicationContext().getString(R.string.songs_tc));

        public AudioTree(TreeBase last) {
            setParent(node, last.get());
            lastTree = last;
        }

        @Override
        protected Node get() {
            return node;
        }

        @Override
        protected Node[] getChild() {
            return new Node[0];
        }

        @Override
        protected Node next(Node node) {
            mCurrentTree = new AudioTree(this);
            Node next = mCurrentTree.get();
            return next;
        }

        @Override
        protected Node next(Node node, int level) {
            mCurrentTree = new AudioTree(this);
            Node next = mCurrentTree.get();
            return next;
        }

        @Override
        protected Node previous() {
            mCurrentTree = lastTree;
            return lastTree.get();
        }
    }

}
