package com.jht.chimera.service.usbmedia.ios.lib.media;


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
        Node[] child = {
                new Node(Node.Category.LIBRARY, "0", ContextUtil.getApplicationContext().getString(R.string.music_tc)),
                new Node(Node.Category.PLAYLIST, "1", ContextUtil.getApplicationContext().getString(R.string.playlists_tc)),
//                new Node(Node.Category.RADIO, "2", "Radio"),
                new Node(Node.Category.PODCAST, "3", ContextUtil.getApplicationContext().getString(R.string.podcasts_tc))
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
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case LIBRARY:
                    mCurrentTree = new MyMusicTree(this);
                    next = mCurrentTree.get();
                    break;
                case PLAYLIST:
                    mCurrentTree = new PlaylistTree(this);
                    next = mCurrentTree.get();
                    break;
                case RADIO:
                    mCurrentTree = new RadioTree(this);
                    next = mCurrentTree.get();
                    break;
                case PODCAST:
                    mCurrentTree = new PodcastTree(this);
                    next = mCurrentTree.get();
                    break;
            }
            return next;
        }

        @Override
        protected Node previous() {
            return null;
        }
    }

    class MyMusicTree extends TreeBase {
        Node node = new Node(Node.Category.LIBRARY, "0", ContextUtil.getApplicationContext().getString(R.string.music_tc));
        Node[] child = {
                new Node(Node.Category.ARTIST, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_artists_tc)),
                new Node(Node.Category.ALBUM, "1", ContextUtil.getApplicationContext().getString(R.string.med_app_albums_tc)),
                new Node(Node.Category.AUDIO_TRACK, "2", ContextUtil.getApplicationContext().getString(R.string.med_app_audio_tc)),
                new Node(Node.Category.GENRE, "3", ContextUtil.getApplicationContext().getString(R.string.med_app_genre_tc)),
                new Node(Node.Category.COMPOSER, "4", ContextUtil.getApplicationContext().getString(R.string.composer_tc)),
                new Node(Node.Category.COMPILATION, "5", ContextUtil.getApplicationContext().getString(R.string.compilation_tc))
        };

        public MyMusicTree(TreeBase last) {
            for(Node n : child) {
                node.add(n);
            }
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
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case ARTIST:
                    mCurrentTree = new ArtistTree(this);
                    next = mCurrentTree.get();
                    break;
                case ALBUM:
                    mCurrentTree = new AlbumTree(this);
                    next = mCurrentTree.get();
                    break;
                case AUDIO_TRACK:
                    mCurrentTree = new SongTree(this);
                    next = mCurrentTree.get();
                    break;
                case GENRE:
                    mCurrentTree = new GenreTree(this);
                    next = mCurrentTree.get();
                    break;
                case COMPOSER:
                    mCurrentTree = new ComposerTree(this);
                    next = mCurrentTree.get();
                    break;
                case COMPILATION:
                    mCurrentTree = new CompilationTree(this);
                    next = mCurrentTree.get();
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

    class PlaylistTree extends TreeBase {
        Node node = new Node(Node.Category.PLAYLIST, "0", ContextUtil.getApplicationContext().getString(R.string.playlists_tc));
        Node[] child = {
                new Node(Node.Category.AUDIO_TRACK, "0", ContextUtil.getApplicationContext().getString(R.string.songs_tc))
        };

        public PlaylistTree(TreeBase last) {
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
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            mCurrentTree = new SongTree(this);
            Node next = mCurrentTree.get();
            return next;
        }

        @Override
        protected Node previous() {
            mCurrentTree = lastTree;
            return lastTree.get();
        }
    }

    class RadioTree extends TreeBase {
        Node node = new Node(Node.Category.RADIO, "0", ContextUtil.getApplicationContext().getString(R.string.radio_tc));
//        Node[] child = {
//                new Node(Node.Category.PLAYLIST, "0", "")
//        };

        public RadioTree(TreeBase last) {
            setParent(node, last.get());
            lastTree = last;
        }

        @Override
        protected Node get() {
            return node;
        }

        @Override
        protected Node[] getChild() {
            return null;
        }

        @Override
        protected Node next(Node node) {
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            mCurrentTree = new RadioTree(this);
            Node next = mCurrentTree.get();
            return next;

        }

        @Override
        protected Node previous() {
            mCurrentTree = lastTree;
            return lastTree.get();
        }
    }

    class PodcastTree extends TreeBase {
        Node node = new Node(Node.Category.PODCAST, "0", ContextUtil.getApplicationContext().getString(R.string.podcasts_tc));
        Node[] child = {
                new Node(Node.Category.AUDIO_TRACK, "0", ContextUtil.getApplicationContext().getString(R.string.songs_tc))
        };

        public PodcastTree(TreeBase last) {
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
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case PODCAST:
                    if(level == 0) {
                        mCurrentTree = new PodcastTree(this);
                    } else {
                        mCurrentTree = new SongTree(this);
                    }
                    next = mCurrentTree.get();
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

    class ArtistTree extends TreeBase {
        Node node = new Node(Node.Category.ARTIST, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_artists_tc));
        Node[] child = {
                new Node(Node.Category.ALBUM, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_albums_tc)),
                new Node(Node.Category.AUDIO_TRACK, "1", ContextUtil.getApplicationContext().getString(R.string.songs_tc))
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
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case ARTIST:
                    if(level == 1) {
                        mCurrentTree = new ArtistTree(this);
                    } else {
                        mCurrentTree = new AlbumTree(this);
                    }
                    next = mCurrentTree.get();
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
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case ALBUM:
                    if(level == 1) {
                        mCurrentTree = new AlbumTree(this);
                    } else {
                        mCurrentTree = new SongTree(this);
                    }
                    next = mCurrentTree.get();
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

    class SongTree extends TreeBase {
        Node node = new Node(Node.Category.AUDIO_TRACK, "0", ContextUtil.getApplicationContext().getString(R.string.songs_tc));

        public SongTree(TreeBase last) {
            setParent(node, last.get());
            lastTree = last;
        }

        @Override
        protected Node get() {
            return node;
        }

        @Override
        protected Node[] getChild() {
            return null;
        }

        @Override
        protected Node next(Node node) {
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            mCurrentTree = new SongTree(this);
            Node next = mCurrentTree.get();
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
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case GENRE:
                    if(level == 1) {
                        mCurrentTree = new GenreTree(this);
                    } else {
                        mCurrentTree = new ArtistTree(this);
                    }
                    next = mCurrentTree.get();
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

    class ComposerTree extends TreeBase {
        Node node = new Node(Node.Category.COMPOSER, "0", ContextUtil.getApplicationContext().getString(R.string.composer_tc));
        Node[] child = {
                new Node(Node.Category.ALBUM, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_albums_tc)),
                new Node(Node.Category.AUDIO_TRACK, "1", ContextUtil.getApplicationContext().getString(R.string.songs_tc))
        };

        public ComposerTree(TreeBase last) {
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
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
            Node next = null;
            switch(node.getCategory()) {
                case COMPOSER:
                    if(level == 1) {
                        mCurrentTree = new ComposerTree(this);
                    } else {
                        mCurrentTree = new AlbumTree(this);
                    }
                    next = mCurrentTree.get();
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

    class CompilationTree extends TreeBase {
        Node node = new Node(Node.Category.COMPILATION, "0", ContextUtil.getApplicationContext().getString(R.string.compilation_tc));
        Node[] child = {
                new Node(Node.Category.ALBUM, "0", ContextUtil.getApplicationContext().getString(R.string.med_app_albums_tc)),
                new Node(Node.Category.AUDIO_TRACK, "1", ContextUtil.getApplicationContext().getString(R.string.songs_tc))
        };

        public CompilationTree(TreeBase last) {
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
            return null;
        }

        @Override
        protected Node next(Node node, int level) {
//            mCurrentTree = new AlbumTree(this);
            mCurrentTree = new SongTree(this);
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
