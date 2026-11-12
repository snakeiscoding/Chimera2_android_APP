package com.jht.chimera.service.consoleconfig;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.test.exceptions.AuthorizationFailure;
import com.jht.chimera.test.exceptions.BadHTTPRequest;
import com.jht.chimera.test.exceptions.ContentNotModified;
import com.jht.chimera.test.mockweb.BaseResponseHandler;

import org.jetbrains.annotations.NotNull;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

class ConsoleConfigServiceResponseHandler extends BaseResponseHandler {
    private String responseXML;

    void setResponseXML(String responseXML) {
        this.responseXML = responseXML;
    }

    @Override
    public @NotNull MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
        try {
            validateRequestHeaders(recordedRequest);
            checkETag(recordedRequest);
            return getResponse();
        } catch (BadHTTPRequest ignored) {
            return new MockResponse().setResponseCode(400);
        } catch (AuthorizationFailure ignored) {
            return new MockResponse().setResponseCode(403);
        } catch(ContentNotModified ignored) {
            return new MockResponse().setResponseCode(304);
        }
    }

    @NotNull
    private MockResponse getResponse() {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(200);
        mockResponse.addHeader("Content-Type", "text/xml");
        mockResponse.addHeader("ETag", FileUtils.computeMd5(responseXML));
        mockResponse.setBody(responseXML);
        return mockResponse;
    }

    private void checkETag(@NotNull RecordedRequest recordedRequest) throws ContentNotModified {
        String etag = recordedRequest.getHeader("If-None-Match");
        if (etag != null && etag.equals(FileUtils.computeMd5(responseXML))) {
            throw new ContentNotModified("MD5 matches");
        }
    }

    private void validateRequestHeaders(@NotNull RecordedRequest recordedRequest) throws BadHTTPRequest, AuthorizationFailure {
        HttpUrl httpUrl = getHttpUrl(recordedRequest);
        checkHeaderExists(recordedRequest, "x-jfit-authorization", true);
        checkQueryParameterExists(httpUrl, "club_id");

    }

}
