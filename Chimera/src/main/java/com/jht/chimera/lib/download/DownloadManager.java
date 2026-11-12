package com.jht.chimera.lib.download;

import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;

/*
 * class DownloadManager
 * 
 * This class provides the public interface for our custom download manager.
 * 
 */
public class DownloadManager {
	
	@NonNls public final static String TAG = "com.jht.downloadlib";

	private static final int DEFAULT_MAX_DOWNLOADS = 3;
	private static final int DEFAULT_CHUNK_SIZE = 10 * 1048576;
	private int maxDownloads = 3;
	private int numDownloads = 0;
	private int maxChunkSize = -1;
	
	private ArrayList<DownloadRecord> downloads = new ArrayList<DownloadRecord>();
	
	public class DownloadStatusCallback implements IDownloadStatus
	{
		IDownloadStatus orignialCallback;
		DownloadRecord record;
		
		public DownloadStatusCallback(DownloadRecord record, IDownloadStatus original)
		{
			this.record = record;
			orignialCallback = original;
		}

		@Override
		public void onComplete(DownloadRequest request, STATUS_CODE status, int errorCode) {
			record.setDownloadState(DownloadRecord.DownloadState.COMPLETE);
			orignialCallback.onComplete(request, status, errorCode);
			
			synchronized(this)
			{
				numDownloads--;
				for(DownloadRecord r : downloads)
				{
					if(r.getDownloadState() == DownloadRecord.DownloadState.PENDING)
					{
						r.start();
						break;
					}
				}
			}
			
		}
	}
	public DownloadManager()
	{
		this(DEFAULT_MAX_DOWNLOADS, DEFAULT_CHUNK_SIZE);
	}
	
	public DownloadManager(int maxDownloads)
	{
		this(maxDownloads, DEFAULT_CHUNK_SIZE);
	}

	public DownloadManager(int maxDownloads, int maxChunkSize)
	{
		this.maxDownloads = maxDownloads;
		this.maxChunkSize = maxChunkSize;
	}
	
	public void setMaxDownloads(int maxDownloads)
	{
		this.maxDownloads = maxDownloads;
	}

	public void setChunkSize(int maxChunkSize)
	{
		this.maxChunkSize = maxChunkSize;
	}
	
	public IDownloadStatus createCallback(DownloadRecord record, IDownloadStatus original)
	{
		return new DownloadStatusCallback(record, original);
	}

	/**
	 * Starts downloading the location specified in the DownloadRequest.
	 * @param request The download parameters
	 * @param callback The callback for the download
     * @return The record to track the download
     */
	public DownloadRecord download(DownloadRequest request, IDownloadStatus callback)
	{
		Log.d(TAG, "Received download request " + request );
		DownloadThread thread = new DownloadThread(this, request, maxChunkSize, callback);
		DownloadRecord record = thread.getDownloadRecord();
		downloads.add(record);
		synchronized(this)
		{
			if(numDownloads < maxDownloads)
			{
				numDownloads++;
				record.start();
			}
		}
		return record;
	}
	
	public void clearDownloads()
	{
		Log.e(TAG, "Clearing all downloads!");
		for(DownloadRecord d :downloads)
		{
			d.stopDownload();
		}
		downloads.clear();
	}
	

	public boolean isDownloading()
	{

		for(DownloadRecord d :downloads)
		{
			if(d.isDownloading())
				return true;
		}
		return false;
	}

	/**
	 * Gets the progress as an integer from 0 to 100.
	 *
	 * @return The current progress from 0 to 100
     */
	public int getProgress()
	{
		long downloadSize = 0;
		long downloaded = 0;
		for(DownloadRecord d :downloads)
		{
			downloadSize += d.getDownloadTotalSize();
			downloaded += d.getDownloadedSize();
		}
		return (int)(downloaded  * 100.0 / downloadSize + 0.5);
	}

	/**
	 * The number of bytes that have been downloaded to this point.
	 *
	 * @return The number of bytes that have been downloaded
     */
	public long getSizeDownloaded()
	{
		long downloaded = 0;
		for(DownloadRecord d :downloads)
		{
			downloaded += d.getDownloadedSize();
		}
		return downloaded;
	}
}
