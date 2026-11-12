package com.jht.chimera.service.dapi.handlers;

import androidx.annotation.NonNull;

import com.jacob.CborDecoder;
import com.jacob.CborEncoder;
import com.jht.chimera.test.exceptions.BadHTTPRequest;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okio.Buffer;

public abstract class RequestHandler {

    public abstract @NotNull MockResponse parse(CborDecoder decoder) throws IOException, BadHTTPRequest;


    public abstract int getCommand();
    public abstract int getVersion();

    protected @NonNull MockResponse createGenericResponse(int result, String message) throws IOException {
        MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(encodeResponse(result, message));
        return response;
    }

    private Buffer encodeResponse(int result, String message) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CborEncoder encoder = new CborEncoder(outputStream);
        encoder.writeArrayStart(4);
        encoder.writeInt(getCommand());
        encoder.writeInt(getVersion());
        encoder.writeInt(result);
        encoder.writeTextString(message);
        return new Buffer().write(outputStream.toByteArray());
    }

}
