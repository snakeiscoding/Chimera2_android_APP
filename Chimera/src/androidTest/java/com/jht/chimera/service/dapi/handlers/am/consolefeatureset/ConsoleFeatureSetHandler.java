package com.jht.chimera.service.dapi.handlers.am.consolefeatureset;

import com.jacob.CborDecoder;
import com.jht.chimera.service.dapi.handlers.RequestHandler;
import com.jht.chimera.service.dapi.testfixture.DAPIServiceTestFixture;
import com.jhtp.dapiservice.comm.CborCommands;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;

public class ConsoleFeatureSetHandler extends RequestHandler {


    private final DAPIServiceTestFixture testFixture;
    public ConsoleFeatureSetHandler(DAPIServiceTestFixture testFixture) {
        this.testFixture = testFixture;
    }
    public @NotNull MockResponse parse(CborDecoder decoder) throws IOException {
        testFixture.receivedRequest(
                new ConsoleFeaturesetRequest(getVersion(), new ConsoleFeatureSetDataV1(decoder)));
        return createGenericResponse(0, "");
    }

    @Override
    public int getCommand() {
        return CborCommands.ConsoleFeatureSet.Value;
    }

    @Override
    public int getVersion() {
        return 1;
    }

}
