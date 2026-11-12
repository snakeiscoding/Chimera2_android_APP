package com.jht.chimera.service.dapi.testfixture;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.service.dapi.handlers.DAPIRequest;
import com.jht.chimera.service.dapi.handlers.am.getmachineid.GetMachineUUIDHandler;
import com.jht.chimera.service.dapi.handlers.am.getmachinestats.MachineStatsData;
import com.jht.chimera.test.exceptions.BadHTTPRequest;
import com.jht.chimera.test.exceptions.RequestNotFound;
import com.jht.chimera.test.fixtures.BaseTestFixture;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.mockwebserver.MockWebServer;

public class DAPIServiceTestFixture extends BaseTestFixture {
    private static final String TAG = "DAPIServiceTestFixture";
    private final MockWebServer server = new MockWebServer();
    private final List<DAPIRequest> expectedRequests = new ArrayList<>();
    private final List<DAPIRequest> requestsToIgnore = new ArrayList<>();
    private DAPIRequest testFailedRequestExpected = null;
    private final JHTNotification testFinishedNotification = new JHTNotification();
    private BadHTTPRequest badHTTPRequest = null;
    private RequestNotFound requestNotFound = null;
    private MachineStatsData machineStatsData = null;
    private final StringBuilder testResults = new StringBuilder();

    private GetMachineUUIDHandler.MachineUUIDResponse machineUUIDResponse = null;

    public DAPIServiceTestFixture() {
        server.setDispatcher(new DAPIServiceResponseHandler(this));
    }

    public void addExepectedRequest(DAPIRequest request) {
        expectedRequests.add(request);
    }

    public void addRequestToIgnore(DAPIRequest request) {
        requestsToIgnore.add(request);
    }

    public void receivedRequest(DAPIRequest request) {
        if(!requestsToIgnore.contains(request)) {
            if(expectedRequests.size() > 0 && expectedRequests.get(0).equals(request)) {
                testResults.append("Received ").append(request).append(" as expected\n");
                expectedRequests.remove(0);
                if(expectedRequests.size() == 0) {
                    testFinishedNotification.setNotification();
                }
            }
            else {
                if(expectedRequests.size() > 0) {
                    testResults.append("Received ").append(request).append(" but expected ").append(expectedRequests.get(0)).append("\n");
                    testFailedRequestExpected = expectedRequests.get(0);
                }
                else {
                    testResults.append("Received ").append(request).append(", but we were done.\n");
                    testFailedRequestExpected = null;
                }
                testFinishedNotification.setNotification();
            }
        }
        else {
            testResults.append("Ignoring ").append(request).append("\n");
        }
    }

    void receivedInvalidRequest(BadHTTPRequest badRequest) {
        this.badHTTPRequest = badRequest;
        testFinishedNotification.setNotification();

    }

    void receivedUnknownRequest(RequestNotFound requestNotFound) {
        this.requestNotFound = requestNotFound;
        testFinishedNotification.setNotification();
    }

    @Override
    public void startTest() {
        super.startTest();
        DataMatrix dm = new DataMatrix();
        if(!servicesRunning(dm)) {
            DataMatrix.putBoolean(DAPIKeys.DATA_DAPI_SERVICE_READY_BOOL, false);
        }
        dm.finish();
        super.startServices(dm);
        try {
            server.start();
            waitForSystemToBeReady(dm);
        } catch (IOException | InterruptedException ignored) {
        }
    }

    private void waitForSystemToBeReady(DataMatrix dm) throws InterruptedException {
        JHTNotification notification = new JHTNotification();
        dm.createBoolAdapter(DAPIKeys.DATA_DAPI_SERVICE_READY_BOOL, ready -> {
            if(ready) {
                notification.setNotification();
            }
        });
        Assert.assertTrue(notification.waitForNotification(30000));
        dm.finish();
        Thread.sleep(2000);
    }

    public void switchToMockServer() {
        DataMatrix.putString(DAPIKeys.ACTION_SET_DAPI_TEST_URL_STRING, server.url("/").toString());
    }

    @Override
    public void reset() {
        super.reset();
        expectedRequests.clear();
        requestsToIgnore.clear();

    }
    public void waitForTestComplete(int timeoutMs) {
        testFinishedNotification.waitForNotification(timeoutMs);
    }

    @Override
    public void runFinalCheck() {
        super.runFinalCheck();
        if(badHTTPRequest != null) {
            Assert.fail(badHTTPRequest.message());
        }
        if(requestNotFound != null) {
            Assert.fail(testResults.toString() + "\nUnknown request received. " + requestNotFound.command() + " " + requestNotFound.version());
        }
        Assert.assertNull(testResults.toString(), testFailedRequestExpected);
        if(!expectedRequests.isEmpty()) {
            testResults.append("Expected the following messages " );
            for(DAPIRequest request : expectedRequests) {
                testResults.append(request).append(", ");
            }
            Assert.fail(testResults.toString());
        }
    }

    @Override
    public void close() {
        super.close();
        try {
            server.close();
        } catch (IOException ignored) {}
        DataMatrix.putString(DAPIKeys.ACTION_SET_DAPI_TEST_URL_STRING, "");
    }

    public void setMachineUUIDResponse(GetMachineUUIDHandler.MachineUUIDResponse machineUUIDResponse) {
        this.machineUUIDResponse = machineUUIDResponse;
    }

    public GetMachineUUIDHandler.MachineUUIDResponse getMachineUUIDResponse() {
        return machineUUIDResponse;
    }

    public void setMachineStats(MachineStatsData machineStatsData) {
        this.machineStatsData = machineStatsData;
    }

    public MachineStatsData getMachineStats() {
        return machineStatsData;
    }



}
