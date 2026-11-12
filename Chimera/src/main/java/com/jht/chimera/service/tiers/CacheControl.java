package com.jht.chimera.service.tiers;

import java.net.HttpURLConnection;

public class CacheControl {
    private String etag = null;


    public void addHeaders(HttpURLConnection connection)
    {
        if( etag != null)
        {
            connection.addRequestProperty("If-None-Match", etag);
        }
    }

    public void updateFields(HttpURLConnection connection)
    {
        etag = connection.getHeaderField("ETag");
    }
}
