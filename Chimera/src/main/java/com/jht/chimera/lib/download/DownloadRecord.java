package com.jht.chimera.lib.download;

public class DownloadRecord {
	private DownloadThread thread;
	public enum DownloadState
	{
		PENDING,
		IN_PROGRESS,
		COMPLETE;
	}
	private DownloadState state;
	
	public DownloadRecord(DownloadThread thread)
	{
		state = DownloadState.PENDING;
		this.thread = thread;
	}
	
	public void stopDownload()
	{
		thread.stop();
	}
	
	public int getProgress()
	{
		return thread.getProgress();
	}
	
	public boolean isDownloading()
	{
		return thread.isDownloading();
	}

	public int getDownloadTotalSize()
	{
		return thread.getDownloadTotalSize();
	}

	public int getDownloadedSize()
	{
		return thread.getDownloadedSize();
	}
	

	public void setDownloadState(DownloadState state)
	{
		this.state = state;
	}

	public DownloadState getDownloadState()
	{
		return state;
	}
	
	public void start()
	{
		state = DownloadState.IN_PROGRESS;
		thread.start();
	}
}
