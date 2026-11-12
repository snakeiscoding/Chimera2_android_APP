package com.jht.chimera.test.mockweb;

import com.jht.chimera.test.exceptions.AuthorizationFailure;
import com.jht.chimera.test.exceptions.BadHTTPRequest;

import org.jetbrains.annotations.NotNull;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.RecordedRequest;

abstract public class BaseResponseHandler extends Dispatcher {

    protected HttpUrl getHttpUrl(@NotNull RecordedRequest recordedRequest) throws BadHTTPRequest {
        HttpUrl httpUrl = recordedRequest.getRequestUrl();
        if(httpUrl== null) {
            throw new BadHTTPRequest("URL is null");
        }
        return httpUrl;
    }

    protected void checkHeaderExists(@NotNull RecordedRequest recordedRequest, String header, boolean auth) throws AuthorizationFailure, BadHTTPRequest {
        if(recordedRequest.getHeader(header) == null) {
            if(auth) {
                throw new AuthorizationFailure("Missing header " + header);
            }
            else {
                throw new BadHTTPRequest("Missing header " + header);
            }
        }
    }

    protected void checkQueryParameterExists(@NotNull HttpUrl httpUrl, String parameter) throws BadHTTPRequest {
        if(httpUrl.queryParameter(parameter) == null) {
            throw new BadHTTPRequest("Missing query parameter " + parameter);
        }
    }
}
