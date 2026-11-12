package com.jht.chimera.lib.download;

import android.util.Log;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.download.IDownloadStatus.STATUS_CODE;
import com.jht.datamatrixlib.DataMatrix;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class DownloadThread implements Runnable
{

    private boolean _forceStop = false;
    private URL url;
    private Thread thread = null;
    private DownloadRequest request;
    private DownloadRecord record;
    private IDownloadStatus callback;
    private int progress = 0;
    private int downloadSize = 0;
    private int downloaded = 0;
    private boolean isDownloading = true;
    private int maxChunkSize = 0;
    private String md5 = "";

    private Object fileSizeLock = new Object();
    private int fileSizeResult = 0;

    public DownloadThread(DownloadManager dm, final DownloadRequest request, int maxChunkSize, IDownloadStatus callback)
    {
        try
        {
            this.url = new URL(request.address);
            this.request = request;
            this.maxChunkSize = maxChunkSize;
            this.record = new DownloadRecord(this);
            this.callback = dm.createCallback(record, callback);
            thread = new Thread(this);
        }
        catch (MalformedURLException e)
        {
            Log.d(DownloadManager.TAG, "MalformedURLException", e);
            setProgress(100);
            // use callback since this.callback will not be set unless we move logic around above
            if (callback != null)
                callback.onComplete(request, STATUS_CODE.INVALID_URL, 0);
        }
    }

    public DownloadRecord getDownloadRecord()
    {
        return record;
    }

    private int getFileSize()
    {
        synchronized (this)
        {
            try
            {
                int redirects = 0;
                while (redirects < request.numRedirects)
                {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("HEAD");

                    int responseCode = connection.getResponseCode();
                    md5 = connection.getHeaderField("ETag");
                    if (md5 != null && !md5.equals(""))
                    {
                        md5 = md5.substring(1, md5.length() - 1);
                    }
                    if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                        Log.d(DownloadManager.TAG, "Sent get request to " + url.toExternalForm() + " with response " + responseCode);
                    }
                    switch (responseCode)
                    {
                        case HttpURLConnection.HTTP_OK:
                            downloadSize = connection.getContentLength();
                            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                                Log.d(DownloadManager.TAG, "Content length of " + url.toExternalForm() + " is " + downloadSize);
                            }
                            return 0;
                        case HttpURLConnection.HTTP_MOVED_PERM:
                        case HttpURLConnection.HTTP_MOVED_TEMP:
                            String location = connection.getHeaderField("Location");
                            url = new URL(url, location);

                            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                                Log.d(DownloadManager.TAG, "Redirect to " + url.toExternalForm() + ".");
                            }
                            redirects++;
                            break;
                        default:
                            return responseCode;
                    }
                }
            }
            catch (IOException e)
            {
                Log.d(DownloadManager.TAG, "IOException", e);
                return -1;
            }
            catch (Exception ex)
            {
                Log.d(DownloadManager.TAG, "2 Exception ", ex);
                return -2;
            }
        }
        return -3;
    }

    public void start()
    {
        thread.start();
    }

    private synchronized void setProgress(int progress)
    {
        if (progress > 100)
            this.progress = 100;
        else if (progress < 0)
            this.progress = 0;
        else
            this.progress = progress;
        //Log.d(DownloadManager.TAG, "Progress of " + request.description + " is " + this.progress);
        isDownloading = progress < 100;
    }

    public synchronized boolean isDownloading()
    {
        return isDownloading;
    }

    public synchronized int getDownloadTotalSize()
    {
        return downloadSize;
    }

    public synchronized int getDownloadedSize()
    {
        return downloaded;
    }

    public synchronized int getProgress()
    {
        return progress;
    }

    // FIXME - Unclosed streams - The attempt to close is not sufficiently safe.
    private boolean download(InputStream is, final String file, final boolean append)
    {

        BufferedInputStream br = new BufferedInputStream(is);
        File newFile = new File(file + ".tmp");
        BufferedOutputStream outputWriter = null;
        boolean success = true;
        int downloadCounter = 0;
        if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
            Log.d(DownloadManager.TAG, "Downloading " + request.address + " to " + file);
        }
        try
        {
            outputWriter = new BufferedOutputStream(new FileOutputStream(newFile.getAbsolutePath(), false));

            byte[] buffer = new byte[request.bufferSize];
            int readSize = 0;
            synchronized (this)
            {
                if (!append)
                {
                    downloaded = 0;
                }
            }
            while ((readSize = br.read(buffer, 0, request.bufferSize)) >= 0)
            {
                // stop if interrupted
                synchronized (this)
                {
                    if (_forceStop)
                    {
                        setProgress(100);
                        if (callback != null)
                            callback.onComplete(request, STATUS_CODE.INTERRUPTED, 0);
                        success = false;
                        return success;
                    }
                }

                downloadCounter += readSize;
                synchronized (this)
                {
                    downloaded += readSize;
                }
                if (downloadSize > 0)
                {
                    int p = (int) ((downloaded) * 100.0 / downloadSize + 0.5);
                    if (p > 99)
                        p = 99;
                    setProgress(p);
                }
                outputWriter.write(buffer, 0, readSize);

            }


        }
        catch (IOException e)
        {
            Log.e(DownloadManager.TAG, "IO Exception when downloading " + request.file + ".", e);
            success = false;
            return success;
        }
        finally
        {
            // FIXME - This is not safely ensuring all of the streams are closed.
            try
            {
                if (is != null)
                    is.close();
                if (outputWriter != null)
                {
                    outputWriter.flush();
                    outputWriter.close();
                }
                if (success)
                {
                    ExecuteAsRoot catFiles = new ExecuteAsRoot();
                    if (!append)
                    {
                        catFiles.addCommand("cat " + file + ".tmp > " + file);
                    }
                    else
                    {
                        catFiles.addCommand("cat " + file + ".tmp >> " + file);
                    }
                    catFiles.addCommand("rm " + file + ".tmp");
                    catFiles.addCommand("chmod 777 " + file);
                    catFiles.execute();
                }
                else
                {
                    downloaded = downloaded - downloadCounter;
                }
            }
            catch (IOException e)
            {
                success = false;
                return success;
            }
        }
        success = true;
        return success;


    }

    // FIXME - Generally, the catch block only goes around a small chunk of code. This implementation should be checked.
    @Override
    public void run()
    {

        Log.d(DownloadManager.TAG, url.getProtocol() + " " + url.toString());
        if(url.getProtocol().toLowerCase(Locale.ENGLISH).startsWith("ftp")) {
            downloadFTP();
            return;
        }
        try
        {
            synchronized (fileSizeLock)
            {
                fileSizeResult = getFileSize();
            }
            if (fileSizeResult != 0)
            {
                if (DownloadThread.this.callback != null)
                    DownloadThread.this.callback.onComplete(request, STATUS_CODE.HTTP_ERROR, fileSizeResult);
                return;
            }

            synchronized (fileSizeLock)
            {
                if (fileSizeResult != 0)
                {
                    return;
                }
            }
            int numChunks = 1;
            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                Log.d(DownloadManager.TAG, "Downloading " + url.toExternalForm() + " with size " + downloadSize + " maxChunk " + maxChunkSize);
            }

            synchronized (this)
            {
                if (maxChunkSize > 0)
                {
                    numChunks = downloadSize / maxChunkSize;

                    if (downloadSize % maxChunkSize != 0)
                    {
                        numChunks++;
                    }
                }
            }
            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                Log.d(DownloadManager.TAG, "Downloading " + url.toExternalForm() + " in " + numChunks + " chunks.");
            }

            int currentChunk = findStartChunk();
            if (currentChunk == -1)
            {
                setProgress(100);
                if (callback != null)
                    callback.onComplete(request, STATUS_CODE.SUCCESS, 0);
                downloaded = downloadSize;
                return;
            }

            downloaded += currentChunk * maxChunkSize;

            for (int i = currentChunk; i < numChunks; i++)
            {
                int redirects = 0;
                int retries = 0;
                boolean success = false;
                while (!success && redirects < request.numRedirects && retries < request.numRetries)
                {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    if (request.keepAlive)
                    {
                        connection.setRequestProperty("Connection", "keep-alive");
                    }
                    if (numChunks > 1)
                    {
                        long startByte = i * maxChunkSize;
                        long endByte = (i + 1) * maxChunkSize - 1;
                        if (endByte > downloadSize - 1)
                            endByte = downloadSize - 1;
                        connection.addRequestProperty("RANGE", "bytes=" + startByte + "-" + endByte);
                    }
                    connection.setReadTimeout(request.readTimeout);
                    connection.setConnectTimeout(request.connectionTimeout);
                    connection.setDoInput(true);
                    connection.setDoOutput(false);
                    connection.setRequestMethod("GET");

                    if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                        Log.d(DownloadManager.TAG, "Setup request for " + url.toExternalForm());
                    }

                    int responseCode = connection.getResponseCode();
                    if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                        Log.d(DownloadManager.TAG, "Sent get request to " + url.toExternalForm() + " with response " + responseCode);
                    }
                    switch (responseCode)
                    {
                        case HttpURLConnection.HTTP_OK:
                        case HttpURLConnection.HTTP_PARTIAL:
                            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                                Log.d(DownloadManager.TAG, "Downloading " + request.file + " part " + i + " retries " + retries + " numChunks " + numChunks);
                            }
                            if (!download(connection.getInputStream(), request.file, i > 0))
                            {
                                if (_forceStop)
                                {
                                    Log.e(DownloadManager.TAG, "Force stopped " + request.file);
                                    return;
                                }
                                if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                                    Log.d(DownloadManager.TAG, "Download of " + url.toExternalForm() + " failed.");
                                }
                                retries++;
                                if (retries < request.numRetries)
                                    try { Thread.sleep(request.retryTimeout); } catch (InterruptedException ignore) {}
                            }
                            else
                            {
                                success = true;
                                if (i == numChunks - 1)
                                {
                                    String computedMd5 = FileUtils.computeMd5(new File(request.file));
                                    Log.d(DownloadManager.TAG, "md5 = " + md5 + " compute = " + computedMd5);
                                    if (md5 != null && !md5.equals("") && !computedMd5.equals(md5))
                                    {
                                        setProgress(100);
                                        if (callback != null)
                                            callback.onComplete(request, STATUS_CODE.MD5_ERROR, 0);
                                    }
                                    else
                                    {
                                        setProgress(100);
                                        if (callback != null)
                                            callback.onComplete(request, STATUS_CODE.SUCCESS, 0);
                                    }
                                    return;
                                }
                            }
                            break;
                        case HttpURLConnection.HTTP_NOT_FOUND:
                            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                                Log.d(DownloadManager.TAG, url.toExternalForm() + " not found.");
                            }
                            setProgress(100);
                            if (callback != null)
                                callback.onComplete(request, STATUS_CODE.URL_NOT_FOUND, responseCode);

                        case HttpURLConnection.HTTP_MOVED_PERM:
                        case HttpURLConnection.HTTP_MOVED_TEMP:
                            String location = connection.getHeaderField("Location");
                            url = new URL(url, location);

                            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                                Log.d(DownloadManager.TAG, "Redirect to " + url.toExternalForm() + ".");
                            }
                            redirects++;
                            break;
                        default:
                            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                                Log.d(DownloadManager.TAG, "Download of " + url.toExternalForm() + " failed with code " + responseCode);
                            }
                            setProgress(100);
                            if (callback != null)
                                callback.onComplete(request, STATUS_CODE.HTTP_ERROR, responseCode);
                            return;

                    }
                }

                if (redirects >= request.numRedirects)
                {
                    if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                        Log.d(DownloadManager.TAG, "Too many redirects of " + request.address);
                    }
                    setProgress(100);
                    if (callback != null)
                        callback.onComplete(request, STATUS_CODE.TOO_MANY_REDIRECTS, 0);
                }
                else if (retries >= request.numRetries)
                {
                    if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL)) {
                        Log.d(DownloadManager.TAG, "Too many retries for " + request.address);
                    }
                    setProgress(100);
                    if (callback != null)
                        callback.onComplete(request, STATUS_CODE.TOO_MANY_RETRIES, 0);
                }
            }
        }
        catch (IOException e)
        {
            Log.d(DownloadManager.TAG, "IOException", e);
        }
    }

    private void downloadFTP() {
        try {
            URLConnection connect = url.openConnection();
            byte[] buffer = new byte[4096];
            int fileSize = connect.getContentLength();
            try (InputStream stream = connect.getInputStream()) {
                try(OutputStream out = new FileOutputStream(request.file)) {
                    int readSize = stream.read(buffer);
                    int totalReadSize = 0;
                    while (readSize > 0) {
                        totalReadSize += readSize;
                        setProgress(totalReadSize * 100 / fileSize);
                        out.write(buffer, 0, readSize);
                        readSize = connect.getInputStream().read(buffer);
                    }
                    callback.onComplete(request, STATUS_CODE.SUCCESS, 0);
                }
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
            Log.d("DownloadThread", "error downloading from FTP");
            callback.onComplete(request, STATUS_CODE.HTTP_ERROR, 0);
        }

    }

    /**
     * Returns the start chunk that should be downloaded based on what is currently downloaded.
     *
     * @return The start chunk, or -1 if the file is already downloaded.
     */
    private int findStartChunk()
    {
        File outputFile = new File(request.file);
        if (outputFile.exists())
        {
            long currentSize = outputFile.length();
            if (currentSize == downloadSize)
            {
                String computedMd5 = FileUtils.computeMd5(outputFile.getPath());
                if (md5 != null && !md5.isEmpty() && computedMd5.equals(md5))
                {
                    Log.d(DownloadManager.TAG, "File already downloaded");
                    return -1;
                }
                else
                {
                    Log.d(DownloadManager.TAG, "Incorrect md5 for same size file: " + computedMd5 + " vs " + md5);
                    return 0;
                }
            }
            else if (currentSize < downloadSize)
            {
                if (currentSize % maxChunkSize == 0)
                {
                    Log.d(DownloadManager.TAG, "Starting from intermediate chunk");
                    return (int) currentSize / maxChunkSize;
                }
                else
                {
                    Log.d(DownloadManager.TAG, "File at wrong size, starting from beginning");
                    return 0;
                }
            }
            else
            {
                Log.d(DownloadManager.TAG, "File too large, restarting");
                return 0;
            }
        }
        else
        {
            Log.d(DownloadManager.TAG, "No file found, starting from beginning");
            return 0;
        }
    }

    public void stop()
    {
        synchronized (this)
        {
            _forceStop = true;
        }
    }

}
