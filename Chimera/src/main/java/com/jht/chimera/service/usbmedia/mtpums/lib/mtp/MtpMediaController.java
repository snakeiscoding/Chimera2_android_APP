package com.jht.chimera.service.usbmedia.mtpums.lib.mtp;

import android.content.Context;
import android.mtp.MtpDevice;
import android.os.SystemClock;
import android.util.Log;

import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.chimera.service.usbmedia.mtpums.MtpUmsDevice;
import com.jht.chimera.service.usbmedia.mtpums.lib.media.Track;
import com.jht.datamatrixlib.DataMatrix;
import com.mpatric.mp3agic.Mp3FileBuffer;
import com.mpatric.mp3agic.Mp3GenericFile;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;

public class MtpMediaController extends MtpUmsDevice {
	private static final String TAG = "MtpMediaController";
	private static final String ID3_TAG_UNKNOWN = "Unknown"; //NON-NLS

	private final MtpController mtpCtrl;
	private static final ReentrantLock mtpLock = new ReentrantLock();
	private int mTrackId;
	private int mVideoId;
	private int mArtistId;
	private int mAlbumId;
	private int mGenreId;
	private final Map<String, String> mArtists = new HashMap<>();
	private final Map<String, String> mAlbums = new HashMap<>();
	private final Map<String, String> mGenres = new HashMap<>();
	private final Map<String, String> mFileNames = new HashMap<>();
	private MediaScanner mMediaScanner;

	interface OnScanCompletedListener {
	    void onScanCompleted(DeviceContent.ContentInfo info, boolean isAudio);
		void onScanAllCompleted();
    }

	public MtpMediaController(Context context, MtpDevice mtpDevice, DataMatrix dataMatrix) {
        super(context, mtpDevice.getDeviceInfo() == null ? "USB" : mtpDevice.getDeviceInfo().getModel(), dataMatrix); //NON-NLS

        mDeviceType = JHTTypes.MediaDeviceType.MTP;
		mtpCtrl = new MtpController(mtpDevice);
		mtpCtrl.setOnScanCompletedListener(new OnScanCompletedListener() {
			@Override
			public void onScanCompleted(DeviceContent.ContentInfo info, boolean isAudio) {
				if (isAudio) {
					addMusic(info);
				} else {
					addVideo(info);
				}
			}

			@Override
			public void onScanAllCompleted() {

				try {
					Track track = new Track();
					track.setTitle("Exit"); //NON-NLS
					mAudioQueue.put(track);
					mVideoQueue.put(track);
				} catch (InterruptedException e) {
					Log.w(TAG, e);
				}

			}
		});
		scan();
	}
		
	static public void lockMTP() {
		mtpLock.lock();
	}
	
	static public void unlockMTP() {
		mtpLock.unlock();
		
	}

    @Override
    public void scan() {
        mMediaScanner = new MediaScanner();
        mMediaScanner.start();
    }

    @Override
	public boolean select(MediaItem item) {
		boolean rtn = super.select(item);
		if(!rtn) {
			mtpCtrl.setFileName(mFileNames.get(item.title));
			mtpCtrl.setHandle(Integer.valueOf(item.id));
		}

		return rtn;
	}

	@Override
    public void back() {
	    super.back();
    }

    @Override
    public void next() {
	    mIsPaused = false;
	    MediaItem nextItem = getNextItem(true);
	    if (nextItem.equals(mPlayingItem)) {
	    	return;
		}
	    mPlayingItem = nextItem;
	    setContentServerFileName(mPlayingItem);
	    play2();
    }

    @Override
    public void previous() {
        mIsPaused = false;
        MediaItem previousItem = getNextItem(false);
        if (previousItem.equals(mPlayingItem)) {
        	return;
		}
        mPlayingItem = previousItem;
        setContentServerFileName(mPlayingItem);
        play2();
    }

	@Override
	protected byte[] getAlbumArt(Track track) {
		String filename = track.getUri();
		byte[] first128Bytes = new byte[128];
		byte[] id3v2Bytes = null;

		if (mtpCtrl.getPartialBufferFromStart(Integer.parseInt(track.getId()), first128Bytes) > 0) {
			int id3v2Offset = Mp3FileBuffer.getID3v2Offset(first128Bytes);
			boolean readId3v2Success = false;
			if (id3v2Offset > 0) {
				id3v2Bytes = new byte[id3v2Offset];
				readId3v2Success = mtpCtrl.getPartialBufferFromStart(Integer.parseInt(track.getId()), id3v2Bytes) > 0;
			}

			if(readId3v2Success) {
				try {
					Mp3FileBuffer mp3FileBuffer = new Mp3FileBuffer(id3v2Bytes, null);

					byte[] albumArt = mp3FileBuffer.getAlbumImage();
					if(null != albumArt) {
						Log.d(TAG, "got album art");
					}
					return albumArt;
				} catch(Exception e) {
					Log.w(TAG, e);
				}
			}
		}

		return null;
	}

	@Override
	public String getUri(MediaItem item) {
		String uri = super.getUri(item);
		if(uri == null) {
			return null;
		}

		return MTP_LOCAL_ADDR + uri;
	}

	@Override
	public void disconnect() {
		super.disconnect();

		if (mMediaScanner != null) {
			mMediaScanner.stop();
		}

		mtpCtrl.stopContentServer();
	}

	private void setContentServerFileName(MediaItem item) {
		if(item != null) {
			mtpCtrl.setFileName(mFileNames.get(item.title));
			mtpCtrl.setHandle(Integer.valueOf(item.id));
		}
	}

    class MtpFileAccess implements Mp3GenericFile.IGenericFileAccess {
		long mOffset = 0;
		MtpController localMtpCtrl;
		String mFilename;
		long mSize;
		int mHandle;

		MtpFileAccess(MtpController mtpCtrl, String filename, long size) throws FileNotFoundException {
			localMtpCtrl = mtpCtrl;
			mFilename = filename;
			mSize = size;
		}

		MtpFileAccess(MtpController mtpCtrl, String filename, long size, int handle) throws FileNotFoundException {
			this(mtpCtrl, filename, size);
			mHandle = handle;
		}
		
		@Override
		public void seek(long offset) {
			mOffset = offset;
		}
		
		@Override
		public int read(byte[] buffer, int byteOffset, int byteCount) {
			byte[] temp = new byte[byteCount];
//			int bytesRead = localMtpCtrl.getPartialBuffer(mFilename, (int) mOffset, temp);
			int bytesRead = localMtpCtrl.getPartialBuffer(mHandle, (int) mOffset, temp);
			System.arraycopy(temp, 0, buffer, byteOffset, bytesRead);
			return bytesRead;
		}
		
		@Override
		public long length() {
			return mSize;
		}
		
		@Override
		public void close() {}
	}

	class MP3TrackInfo {
		public MP3TrackInfo(MtpFileAccess file, Track track, int id) {
			this.file = file;
			this.track = track;
			this.id = id;
		}
		public MtpFileAccess file;
		public Track track;
		public int id;
	}
	
	class GetMP3Length implements Runnable {
		LinkedBlockingDeque<MP3TrackInfo> mp3Files = new LinkedBlockingDeque<>();
		boolean _isRunning = false;
		
		public void addTrack(MP3TrackInfo info) {
			try {
				mp3Files.put(info);
			} catch(InterruptedException ignored) {
			} 
		}
		
		protected synchronized void setIsRunning(boolean isRunning)
		{
			_isRunning = isRunning;
		}
		public synchronized boolean isRunning()
		{
			return _isRunning;
		}
		
		@Override
		public void run() {
			setIsRunning(true);
			try {
				MP3TrackInfo info = mp3Files.take();
				while(info != null && info.file != null && info.track != null) {
					parseMP3(info);
					info = mp3Files.take();
				}
			} catch(InterruptedException ignored) {
			} finally {
				setIsRunning(false);
			}
		}
		
		private void parseMP3(MP3TrackInfo info) {
			try {
				Mp3GenericFile mp3File = new Mp3GenericFile(info.file, true);
				info.track.setDuration((int)mp3File.getLengthInMilliseconds());
			} catch (Exception e) {
				// Eat everything.
			}
			
		}
		
	}
	//GetMP3Length getMP3LengthTask = new GetMP3Length();
    private String getPos(String pos) {
		try {
			if(pos != null) {
				String[] posValues = pos.split("/");
				if(posValues.length > 0) {
					int position = Integer.parseInt(posValues[0]);
					return String.format(Locale.getDefault(), "%04d", position);
				}
				
			}
			return "0";
		} catch(Exception ex) {
			return "0";
		}
	}
	
	
	private String checkID3Tag(String tagValue) {
		String result;
		if(null != tagValue) {
			result = tagValue.trim();
			if(result.length() == 0) {
				result = ID3_TAG_UNKNOWN;
			}
		} else {
			result = ID3_TAG_UNKNOWN;
		}
		return result;
	}

    private class MediaScanner implements Runnable {
        private boolean mIsRunning;

		public void start() {
            if (mIsRunning) {
                return;
            }

            mIsRunning = true;
			Thread mThread = new Thread(this);
            mThread.start();
        }

        public void stop() {
            mIsRunning = false;
        }

        private boolean updateContent() {
            boolean unlocked = mtpCtrl.isDeviceUnLocked();
            if(!unlocked) {
                Log.d(TAG, "Device may be locked.");
            } else {
                Log.d(TAG, "Device may be unlocked.");
                mtpCtrl.createContentList();
            }

            return unlocked;
        }

        @Override
        public void run() {
            while (mIsRunning && !updateContent()) {
				SystemClock.sleep(2000);
            }

            mIsRunning = false;
        }
    }
	
	private void addMusic(DeviceContent.ContentInfo info) {
		// read the id3 tags
		String filename = info.getName();
		int fileSize = info.getSize();
		Log.d(TAG, "addMusic: " + filename + " " + fileSize);

		boolean addedSong;
		
		try {
//			MtpFileAccess file = new MtpFileAccess(mtpCtrl, filename, fileSize);
			MtpFileAccess file = new MtpFileAccess(mtpCtrl, filename, fileSize, info.getHandle());
			Mp3GenericFile mp3File = new Mp3GenericFile(file);
			
			String artistName = ID3_TAG_UNKNOWN;
			String albumName = ID3_TAG_UNKNOWN;
			String genreName = ID3_TAG_UNKNOWN;
			String titleName = filename;
			String imageMimeType = ID3_TAG_UNKNOWN;
			String trackPos = ID3_TAG_UNKNOWN;
			long length = 0;
			if(mp3File.hasId3v2Tag()) {
				artistName = checkID3Tag(mp3File.getId3v2Tag().getArtist());
				albumName = checkID3Tag(mp3File.getId3v2Tag().getAlbum());
				genreName = checkID3Tag(mp3File.getId3v2Tag().getGenreDescription());
				titleName = checkID3Tag(mp3File.getId3v2Tag().getTitle());
				imageMimeType = mp3File.getId3v2Tag().getAlbumImageMimeType();
				length = mp3File.getId3v2Tag().getSongLength();
				trackPos = mp3File.getId3v2Tag().getTrack();
			} else if(mp3File.hasId3v1Tag()) {
				artistName = checkID3Tag(mp3File.getId3v1Tag().getArtist());
				albumName = checkID3Tag(mp3File.getId3v1Tag().getAlbum());
				genreName = checkID3Tag(mp3File.getId3v1Tag().getGenreDescription());
				titleName = checkID3Tag(mp3File.getId3v1Tag().getTitle());
				trackPos = mp3File.getId3v1Tag().getTrack();
			}

			trackPos = getPos(trackPos);
			mFileNames.put(titleName, filename);
			Log.d(TAG, "addMusic: id3: " + genreName + ", " + artistName  + ", " + albumName + ", " + titleName  + ", " + length + ", " + imageMimeType + ", " + trackPos);
			if(!mArtists.containsKey(artistName)) {
				mArtistId++;
				mArtists.put(artistName, String.valueOf(mArtistId));
			} else {
				mArtistId = Integer.parseInt(mArtists.get(artistName));
			}

			if(!mAlbums.containsKey(albumName)) {
				mAlbumId++;
				mAlbums.put(albumName, String.valueOf(mAlbumId));
			} else {
				mAlbumId = Integer.parseInt(mAlbums.get(albumName));
			}

			if(!mGenres.containsKey(genreName)) {
				mGenreId++;
				mGenres.put(genreName, String.valueOf(mGenreId));
			} else {
				mGenreId = Integer.parseInt(mGenres.get(genreName));
			}

//			Track track = new Track(String.valueOf(mTrackId), titleName, artistName, albumName, genreName, (int) length, Track.TrackType.AUDIO_TRACK);
			Track track = new Track(String.valueOf(info.getHandle()), titleName, artistName, albumName, genreName, (int) length, Track.TrackType.AUDIO_TRACK);
			track.setArtistId(String.valueOf(mArtistId));
			track.setAlbumId(String.valueOf(mAlbumId));
			track.setGenreId(String.valueOf(mGenreId));
			track.setUri(info.getName());
			mAudioQueue.put(track);
			mTrackId++;
			addedSong = true;
		} catch (Exception e1) {
			Log.e(TAG, "addMusic: mtp read exception", e1);
			return;
		}

		if (!addedSong) {
			// Unable to decode id3 tag
			Log.d(TAG, "id3: unable to parse");
//			Track track = new Track(String.valueOf(mTrackId), info.getName(), ID3_TAG_UNKNOWN, ID3_TAG_UNKNOWN, ID3_TAG_UNKNOWN, 0, Track.TrackType.AUDIO_TRACK);
			Track track = new Track(String.valueOf(info.getHandle()), info.getName(), ID3_TAG_UNKNOWN, ID3_TAG_UNKNOWN, ID3_TAG_UNKNOWN, 0, Track.TrackType.AUDIO_TRACK);
			track.setUri(info.getName());
			try {
                mAudioQueue.put(track);
			} catch (InterruptedException e) {
				Log.w(TAG, e);
			}
			mTrackId++;
		}
	}
	
	private void addVideo(DeviceContent.ContentInfo info) {
	    // Get video
		mFileNames.put(info.getName(), info.getName());
//		Track track = new Track(String.valueOf(mVideoId), info.getName(), null, null, null, 0, Track.TrackType.VIDEO_TRACK);
		Track track = new Track(String.valueOf(info.getHandle()), info.getName(), null, null, null, 0, Track.TrackType.VIDEO_TRACK);
		track.setUri(info.getName());
		try {
			mVideoQueue.put(track);
		} catch (InterruptedException e) {
			Log.w(TAG, e);
		}
		mVideoId++;
    }

}

