package com.jht.chimera.lib.download;

import java.util.ArrayList;

public class DownloadRequest {
	
	public static class AdditionalHeader
	{
		public String header;
		public String value;
		
		public AdditionalHeader(String header, String value)
		{
			this.header = header;
			this.value = value;
		}
	}
	
	public int numRetries = 4;
	public int numRedirects = 4;
	public int bufferSize = 1024 * 32;
	public int readTimeout = 60000;
	public int connectionTimeout = 65000;
	public boolean keepAlive = true;
	public int retryTimeout = 60000;
	
	public String file;
	public String address;
	public String description;
	
	public ArrayList<AdditionalHeader> headers;
	
	public DownloadRequest(String file, String address, String description)
	{
		this.file = file;
		this.address = address;
		this.description = description;
	}

	public DownloadRequest(String file, String address, String description, ArrayList<AdditionalHeader> headers)
	{
		this.file = file;
		this.address = address;
		this.description = description;
		this.headers = headers;
	}
	
	@Override
	public String toString()
	{
		return
				"Download request\n" +
				"\taddress: " + address + "\n" +
				"\tdescription: " + description + "\n" +
				"\tfile: " + file;
	}
}
