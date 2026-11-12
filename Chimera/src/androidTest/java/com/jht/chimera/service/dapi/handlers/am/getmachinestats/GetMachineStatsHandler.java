package com.jht.chimera.service.dapi.handlers.am.getmachinestats;

import androidx.annotation.NonNull;

import com.jacob.CborDecoder;
import com.jacob.CborEncoder;
import com.jht.chimera.service.dapi.handlers.RequestHandler;
import com.jht.chimera.service.dapi.testfixture.DAPIServiceTestFixture;
import com.jhtp.dapiservice.comm.CborCommands;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okio.Buffer;

public class GetMachineStatsHandler extends RequestHandler {



    private final DAPIServiceTestFixture testFixture;
    public GetMachineStatsHandler(DAPIServiceTestFixture testFixture) {
        this.testFixture = testFixture;
    }

    public @NotNull MockResponse parse(CborDecoder decoder) throws IOException {
        testFixture.receivedRequest(new GetMachineStatsRequest(getVersion(), decoder.readTextString()));
        return getResponse(testFixture.getMachineStats());
    }

    @Override
    public int getCommand() {
        return CborCommands.GetMachineStats.Value;
    }

    @Override
    public int getVersion() {
        return 1;
    }


    protected @NonNull
    MockResponse getResponse(MachineStatsData stats) throws IOException {
        MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(encodeResponse(stats));
        return response;
    }

    private Buffer encodeResponse(MachineStatsData stats) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CborEncoder encoder = new CborEncoder(outputStream);
        encoder.writeArrayStart(9);
        encoder.writeInt(getCommand());
        encoder.writeInt(getVersion());
        encoder.writeInt(0);
        encoder.writeTextString("success");
        encoder.writeDouble(stats.distance());
        encoder.writeInt(stats.time());
        encoder.writeInt(0);
        encoder.writeInt(0);
        encoder.writeInt(0);
        return new Buffer().write(outputStream.toByteArray());
    }

}
