package com.jht.chimera.service.dapi.testfixture;

import com.jacob.CborDecoder;
import com.jht.chimera.service.dapi.handlers.RequestHandler;
import com.jht.chimera.service.dapi.handlers.am.cardioinfo.CardioInfoHandler;
import com.jht.chimera.service.dapi.handlers.am.consolefeatureset.ConsoleFeatureSetHandler;
import com.jht.chimera.service.dapi.handlers.am.consolefeatureset.ConsoleFeatureSetHandlerV2;
import com.jht.chimera.service.dapi.handlers.am.getmachineid.GetMachineUUIDHandler;
import com.jht.chimera.service.dapi.handlers.am.getmachinestats.GetMachineStatsHandler;
import com.jht.chimera.test.exceptions.AuthorizationFailure;
import com.jht.chimera.test.exceptions.BadHTTPRequest;
import com.jht.chimera.test.exceptions.RequestNotFound;
import com.jht.chimera.test.mockweb.BaseResponseHandler;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

class DAPIServiceResponseHandler extends BaseResponseHandler {

    private final HashMap<Integer, List<RequestHandler>> requestHandlers = new HashMap<>();
    private final DAPIServiceTestFixture testFixture;

    DAPIServiceResponseHandler(DAPIServiceTestFixture testFixture) {
        this.testFixture = testFixture;
        addHandler(new CardioInfoHandler(testFixture));
        addHandler(new ConsoleFeatureSetHandler(testFixture));
        addHandler(new ConsoleFeatureSetHandlerV2(testFixture));
        addHandler(new GetMachineUUIDHandler(testFixture));
        addHandler(new GetMachineStatsHandler(testFixture));
    }


    @Override
    public @NotNull MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
        if(recordedRequest.getRequestUrl() == null) { return new MockResponse().setResponseCode(404); }
        return parseRequest(recordedRequest);
    }

    private @NotNull MockResponse parseRequest(RecordedRequest request) {
        try {
            validateHeader(request);
            return getResponse(request);

        }
        catch(RequestNotFound requestNotFound) {
            testFixture.receivedUnknownRequest(requestNotFound);
            return new MockResponse().setResponseCode(404);
        }
        catch(BadHTTPRequest httpRequest) {
            testFixture.receivedInvalidRequest(httpRequest);
            return new MockResponse().setResponseCode(400);
        }
        catch(IOException ioException) {
            testFixture.receivedInvalidRequest(new BadHTTPRequest("IO exception: " + ioException.getMessage()));
            return new MockResponse().setResponseCode(400);
        }
        catch(AuthorizationFailure authorizationFailure) {
            return new MockResponse().setResponseCode(403);
        }
    }

    private @NotNull MockResponse getResponse(RecordedRequest request) throws IOException, RequestNotFound, BadHTTPRequest {
        CborDecoder decoder = new CborDecoder(request.getBody().inputStream());
        decoder.readArrayLength();
        int command = (int)decoder.readInt();
        int version = (int)decoder.readInt();
        List<RequestHandler> handlers = requestHandlers.get(command);
        if(handlers != null) {
            for(RequestHandler handler : handlers) {
                if(handler.getVersion() == version) {
                    return handler.parse(decoder);
                }
            }
        }
        throw new RequestNotFound(command, version);
    }


    private void validateHeader(RecordedRequest request) throws BadHTTPRequest, AuthorizationFailure {
        if(!"application/octet-stream".equals(request.getHeaders().get("Content-Type"))) {
            throw new BadHTTPRequest("HTTP request is not an octet-stream.");
        }
        checkHeaderExists(request, "Content-Length", false);
    }


    private void addHandler(RequestHandler handler) {
        List<RequestHandler> handlers = requestHandlers.get(handler.getCommand());
        if(handlers == null) {
            handlers = new ArrayList<>();
            handlers.add(handler);
            requestHandlers.put(handler.getCommand(), handlers);
        }
        else {
            handlers.add(handler);
        }
    }
}
