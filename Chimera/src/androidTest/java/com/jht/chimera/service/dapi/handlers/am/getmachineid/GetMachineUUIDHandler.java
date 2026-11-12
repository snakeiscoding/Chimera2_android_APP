package com.jht.chimera.service.dapi.handlers.am.getmachineid;

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

public class GetMachineUUIDHandler extends RequestHandler {

    public static class MachineUUIDResponse {
        private final String uuid;
        public MachineUUIDResponse(String uuid) {
            this.uuid = uuid;
        }

        public String uuid() { return uuid; }
    }

    private final DAPIServiceTestFixture testFixture;
    public GetMachineUUIDHandler(DAPIServiceTestFixture testFixture) {
        this.testFixture = testFixture;
    }

    public @NotNull MockResponse parse(CborDecoder decoder) throws IOException {
        MachineUUIDData machineUUIDData = new MachineUUIDData(decoder);
        testFixture.receivedRequest(new GetMachineUUIDRequest(getVersion(), machineUUIDData));
        return getResponse(testFixture.getMachineUUIDResponse() == null ? machineUUIDData.machineId() : testFixture.getMachineUUIDResponse().uuid());
    }

    @Override
    public int getCommand() {
        return CborCommands.GetMachineId.Value;
    }

    @Override
    public int getVersion() {
        return 1;
    }


    protected @NonNull
    MockResponse getResponse(String uuid) throws IOException {
        MockResponse response = new MockResponse();
        response.setResponseCode(200);
        response.setBody(encodeResponse(uuid));
        return response;
    }

    private Buffer encodeResponse(String uuid) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CborEncoder encoder = new CborEncoder(outputStream);
        encoder.writeArrayStart(3);
        encoder.writeInt(getCommand());
        encoder.writeInt(getVersion());
        //encoder.writeInt(0);
        encoder.writeTextString(uuid);
        //encoder.writeTextString("Success");
        return new Buffer().write(outputStream.toByteArray());
    }

}
