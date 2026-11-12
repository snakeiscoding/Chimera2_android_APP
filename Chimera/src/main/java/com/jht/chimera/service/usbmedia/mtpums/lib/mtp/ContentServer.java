/**
 * Johnson Health Tech
 * 1600 Landmark Drive
 * Cottage Grove, WI  53527
 * 
 * Copyright (C) 2014
 * Proprietary and confidential
 * 
 * @author Blong.Thor
 */
package com.jht.chimera.service.usbmedia.mtpums.lib.mtp;

import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * This class creates a local server to transmit media content.  A file descriptor
 * interface is exposed for content readers to write data to this module.  The 
 * received data is buffered to allow smooth transmission to the client.  The 
 * client makes HTTP requests to the designated port with the file name of the
 * media desired for playback.   
 * 
 * Example: http://localhost:8050/myvideo.mp4
 * 
 * @author blong.thor
 *
 */
public class ContentServer {
/**************************************
 * Design 
 * 
 * Use:
 * LocalSocket server;
 * 
 * Create a socket.  Wait for client to connect to it.  When client
 * is connected then send buffered video content to it.  Listen for
 * seek messages.  Handle stop, timeouts, dropped connections.  
 * 
 * 
 */
    @NonNls private static final String TAG = "ContentServer";
	@NonNls private static final String HTTP200 = "200 OK";
	@NonNls private static final String HTTP206 = "206 OK";
	@NonNls private static final String HTTP404 = "404 Not Found";
	@NonNls private static final String HTTP406 = "406 Not Acceptable";
	@NonNls private static final String HTTP416 = "416 Requested Range Not Satisfiable";
	@NonNls	private static final String HTTP500 = "500 Error";
	@NonNls private static final String CONTENT_TYPE_MP3 = "audio/mpeg";
	@NonNls private static final String CONTENT_TYPE_WAV = "audio/vnd.wav";
	@NonNls private static final String CONTENT_TYPE_MP4 = "video/mp4";
	@NonNls private static final String CONTENT_TYPE_WMV = "video/x-ms-wmv";
	@NonNls private static final String CONTENT_TYPE_MOV = "video/quicktime";
	@NonNls private static final String CONTENT_TYPE_AVI = "video/x-msvideo";
	@NonNls private static final String CONTENT_TYPE_FLV = "video/x-flv";
	@NonNls private static final String CONTENT_TYPE_MKV = "video/x-matroska";

    private int localPort = 8050;  // Default port
    private ContentServerListener mListener;
    private ServerSocket server;
	private Socket senderSocket;
	private ConnectThread connectThread;
	private ServerThread serverThread;
    private String fileName;
    private int handle;
    
    /**
     * Convert the content type to MIME.
     * @return
     */
    String decodeContentType(String mediaExtension) {
        String type = CONTENT_TYPE_MP3;		// default
        if(mediaExtension.compareTo("mp3") == 0) {
            type = CONTENT_TYPE_MP3;
        } else if(mediaExtension.compareTo("wav") == 0) {
            type = CONTENT_TYPE_WAV;
        } else if(mediaExtension.compareTo("mp4") == 0) {
            type = CONTENT_TYPE_MP4;
        } else if(mediaExtension.compareTo("wmv") == 0) {
            type = CONTENT_TYPE_WMV;
        } else if(mediaExtension.compareTo("mov") == 0) {
            type = CONTENT_TYPE_MOV;
        } else if(mediaExtension.compareTo("flv") == 0) {
            type = CONTENT_TYPE_FLV;
        } else if(mediaExtension.compareTo("mkv") == 0) {
        	type = CONTENT_TYPE_MKV;
		}
        
        return type;
    }
    
    enum HttpRequestType {
    	GET_MEDIA,
    	SEEK_MEDIA,
    	UNKNOWN
    }
    
    class HttpRequestInfo {
    	HttpRequestType mType;
    	String mMediaName;
    	int mOffset;
    	String mContentType;
    	int mHandle;
    	
    	HttpRequestInfo(HttpRequestType type, String name, int offset, String contentType) {
    		mType = type;
    		mMediaName = name;
    		mOffset = offset;
    		mContentType = contentType;
    	}

		HttpRequestInfo(HttpRequestType type, String name, int offset, String contentType, int handle) {
			this(type, name, offset, contentType);
			mHandle = handle;
		}
    	
    	public HttpRequestType getType() {
    		return mType;
    	}
    	
    	public String getMediaName() {
    		return mMediaName;
    	}

    	public int getHandle() {
    		return mHandle;
		}
    	
    	public int getOffset()
    	{
    		return mOffset;
    	}
    	
    	public String getContentType() {
    		return mContentType;
    	}
    };
    
    /**
     * Constructor
     */
    public ContentServer() { }
    
    /**
     * Get the current port the server is listening to.
     * 
     * @return int port number
     */
    public int getPort()
    {
        return localPort;
    }
    
    /**
     * Start up the local server with the specified port.
     */
    public void init(int port) {
        if(port > 0) {
            localPort = port;
        }
        
        try {
            server = new ServerSocket(localPort);
            connectThread = new ConnectThread();
            new Thread(connectThread).start();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }
    
    /**
     * Set the external event handler for the content server.
     * 
     * @param listener
     */
    public void setListener(ContentServerListener listener) {
        mListener = listener;
    }

    public void setFileName(String name) {
		Log.d(TAG, "setFileName: " + name);
    	fileName = name;
	}

	public void setHandle(int handle) {
		Log.d(TAG, "set handle:" + handle);
    	this.handle = handle;
	}
    
    /**
     * Stop the server and the buffering thread.
     */
    public void stop() {
		Log.d(TAG, "stop: stop the content server");
        try {
            if(server != null) {
                server.close();
                server = null;
                senderSocket = null;
            }
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    /**
     * This thread handles listening for client connections to the
     * content server.
     * 
     * @author Blong.Thor
     *
     */
    class ConnectThread implements Runnable {
		@Override
		public void run() {
			while(true) {
	            try {
	            	Log.d(TAG, "CS: Waiting for client connection");
	                senderSocket = server.accept();
	                Log.d(TAG, "CS: Client connected: " + senderSocket.getRemoteSocketAddress().toString());

	                // Exit old server thread
	                if(serverThread != null) {
	                	serverThread.stopRunning();
	                }
	                
	                // Start new server thread
	                serverThread = new ServerThread(senderSocket);
	                new Thread(serverThread).start();
	            } catch (Exception e) {
	            	if(serverThread != null) {
	                	serverThread.stopRunning();
	                }
	                Log.d(TAG, "CS: socket closed, exit connect thread");
	                return;
	            }
			}
		}
    }
    
    /**
     * Local media content server using HTTP transport.
     * 
     * @author blong.thor
     *
     */
    class ServerThread implements Runnable {
    	boolean mIsRunning = false;
    	Socket socket;
    	InputStream mInputForServer = null;
        OutputStream mOutputForServer = null;
        int mMediaSize = 0;
        
        private final Semaphore mServerThreadComplete = new Semaphore(1, true);
        
        ServerThread(Socket socket) {
        	this.socket = socket;
        	try {
				mInputForServer = socket.getInputStream();
				mOutputForServer = socket.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.w(TAG, e);
			}
        }
    	
        /**
         * Start the local server thread.
         */
        @Override
        public void run() {
        	try {
	        	try {
					mServerThreadComplete.acquire();
				} catch (InterruptedException e1) {
					Log.w(TAG, e1);
				}
	        	
	        	mIsRunning = true;
	        	
	            byte[] clientMsgBuffer = new byte[512];
	            
	            boolean receivedClientReq = false;
	            HttpRequestInfo clientRequestType = null;
	            
	            do {
	                try {
	                	if(mInputForServer.available() > 0) {
	                    	int clientMsgSize = mInputForServer.read(clientMsgBuffer, 0, clientMsgBuffer.length);
							Log.d(TAG, "file name: " + fileName);
	                        if(clientMsgSize > 0) {
	                            String clientMsg = new String(clientMsgBuffer, 0, clientMsgSize);
	                            Log.d(TAG, "clientMsg: " + clientMsg);
	                            
	                            clientRequestType = parseHttpRequest(clientMsg);
	                        }
	                    }
	                    
	                    if(clientRequestType != null) {
	                    	boolean foundContent = false;
	                		if(mListener != null) {
//	                            foundContent = mListener.mediaRequest(clientRequestType.getMediaName(), clientRequestType.getOffset());
								foundContent = mListener.mediaRequest(clientRequestType.getHandle(), clientRequestType.getOffset());
	                            mMediaSize = mListener.getMediaSize();
	                        }

	                    	// Process request
	                    	if(clientRequestType.getOffset() > mMediaSize) {
	                    		mOutputForServer.write(rangeNotSatisfiable().getBytes());
	                    		receivedClientReq = false;
	                    	} else if(foundContent) {
		                    	// Write header for media http stream
		                    	String response = generateHttpResponse(clientRequestType, mMediaSize);
		                    	if(0 < response.length()) {
		                    		mOutputForServer.write(response.getBytes());
		                    		receivedClientReq = true;
		                    	}
	                    	} else {
	                			mOutputForServer.write(notFound().getBytes());
	                			throw(new IOException()); // to enter catch block and wait for new connection
	                		}
	                    	
	                    	// Clear request
	                    	clientRequestType = null;
	                    } else {
	                    	// Wait for a short delay before checking for the next message.
	                    	Thread.sleep(5);
	                    }
	
	                    if(receivedClientReq) {
	                    	// Write data for media http stream
	                    	byte tempBuffer[] = null;
	                    	if(null != mListener) {
	                    		//Log.d("", "cs: before getmediadata()");
	                    		tempBuffer = mListener.getMediaData();
	                    	}
	                    	
	                    	if(null != tempBuffer && tempBuffer.length > 0) {
	                    		//Log.d(TAG, "** cs: buffer: " + tempBuffer.length + " **");
		                        mOutputForServer.write(tempBuffer, 0, tempBuffer.length);
	                    	}
	                    	
	                    	if(mListener.isFinish()) {
	                    		Log.d(TAG, "CS: isFinish() == true");
	                    		mIsRunning = false;
	                    	}
	                    }
	                } catch (Exception e) {
						Log.e(TAG, "" + e);
	                	mIsRunning = false;
	                }
	            } while(mIsRunning);
	            
	            mListener.mediaStopped();
	
	            Log.d(TAG, "CS: Client closed");
	            mServerThreadComplete.release();
        	} finally {
        		try {
        			if(socket != null) {
						socket.close();
					}
        		} catch(Exception ex)
        		{}
        	}
        }
        
        public void stopRunning() {
			Log.d(TAG, "stopRunning: ");
        	try {
        		mIsRunning = true;
        		mInputForServer.close();
				mOutputForServer.close();
				// Wait for thread to exit
	        	mServerThreadComplete.acquire();
	            mServerThreadComplete.release();
        	} catch (Exception e) {
				Log.w(TAG, e);
			}
        }
    }
    
    
    /**
     * Extract information from the HTTP request.  Then
     * determine the request type.
     * 
     * @param request
     */
    private HttpRequestInfo parseHttpRequest(String request) {
        String requestToken[] = request.split("\\s");
        String fileName;
        
        HttpRequestInfo requestType = null;
        
        if(0 == requestToken[0].compareTo("GET")) {

//            int startOfFileName = request.indexOf("/") + 1;
//            int endOfFileName = request.indexOf("HTTP") - 1;
//            fileName = request.substring(startOfFileName, endOfFileName);
            fileName = this.fileName;
            String mediaContentType = "";
            
            // Set media type
            int extensionIndex = fileName.lastIndexOf('.') + 1;
            if(extensionIndex > 0) {
            	mediaContentType = decodeContentType(fileName.substring(extensionIndex));
            }
            
            Log.d(TAG, "parseHttpRequest: File name: [" + fileName + "], type: [" + mediaContentType + "]");

            if(request.contains("Range:")) {
            	// Seek media
            	for(int i = 0; i < requestToken.length; i++) {
            		if(requestToken[i].contains("bytes")) {
            			int startByteRange = requestToken[i].indexOf("=") + 1;
            			int endByteRange = requestToken[i].indexOf("-");
            			String byteRange = requestToken[i].substring(startByteRange, endByteRange);
            			int byteRangeValue = Integer.valueOf(byteRange);
            			Log.d(TAG, "parseHttpRequest: Seek, byte range: " + byteRangeValue);
            			
//            			requestType = new HttpRequestInfo(HttpRequestType.SEEK_MEDIA, fileName, byteRangeValue, mediaContentType);
						requestType = new HttpRequestInfo(HttpRequestType.SEEK_MEDIA, fileName, byteRangeValue, mediaContentType, this.handle);
            		}
            	}
            } else {
            	// Get media from beginning
//            	requestType = new HttpRequestInfo(HttpRequestType.GET_MEDIA, fileName, 0, mediaContentType);
				requestType = new HttpRequestInfo(HttpRequestType.GET_MEDIA, fileName, 0, mediaContentType, this.handle);
            }
        }
        
        return requestType;
    }
    
    /**
     * Generate the http response depending on the request type.
     * 
     * @param clientRequestType
     * @return
     */
    private String generateHttpResponse(HttpRequestInfo clientRequestType, int mediaSize) {
    	@NonNls String response = "";
    	if(clientRequestType != null) {
            if(HttpRequestType.GET_MEDIA == clientRequestType.getType()) {
                response = "HTTP/1.1 " + HTTP200 + "\r\n";
                response += "Content-Type: " + clientRequestType.getContentType() + "\r\n";
                response += "Accept-Ranges: bytes" + "\r\n";
                response +=  "Content-Length: " + mediaSize + "\r\n\r\n";
				Log.d(TAG, "generateHttpResponse: " + response);
            } else if(HttpRequestType.SEEK_MEDIA == clientRequestType.getType()) {
            	response = "HTTP/1.1 " + HTTP206 + "\r\n";
                response += "Content-Type: " + clientRequestType.getContentType() + "\r\n";
                response += "Accept-Ranges: bytes\r\n";
                int partialLength = mediaSize - clientRequestType.getOffset();
                response +=  "Content-Length: " + partialLength + "\r\n";
                response += "Content-Range: bytes " + clientRequestType.getOffset() + "-" + (mediaSize-1) + "/" + mediaSize + "\r\n\r\n";
                Log.d(TAG, "generateHttpResponse: " + response);
            }
        }
    	
    	return response;
    }
    
    /**
     * Generate the http responses.
     * 
     * @return
     */
    private String keepAliveResponse() {
		@NonNls String response = "HTTP/1.1 " + HTTP200 + "\r\n";
        response +=  "Connection: Keep-Alive\r\n\r\n";
        Log.d(TAG, response);
    	
    	return response;
    }
    
    private String rangeNotSatisfiable() {
		String response = "HTTP/1.1 " + HTTP416 + "\r\n\r\n"; // NON-NLS
    	Log.d(TAG, response);
    	return response;
    }
    
    private String notFound() {
		String response = "HTTP/1.1 " + HTTP404 + "\r\n\r\n"; // NON-NLS
    	Log.d(TAG, response);
    	return response;
    }
}



