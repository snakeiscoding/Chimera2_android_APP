package com.jht.chimera.lib.download;


/*
 * These are events the library needs to send to the code that is using it.
 */
public interface IDownloadStatus {
	public enum STATUS_CODE {
		SUCCESS,
		TOO_MANY_RETRIES,
		TOO_MANY_REDIRECTS,
		HTTP_ERROR,
		INTERRUPTED,
		URL_NOT_FOUND,
		INVALID_URL,
		MD5_ERROR;
	}
	public void onComplete(DownloadRequest request, STATUS_CODE status, int errorCode);
}
