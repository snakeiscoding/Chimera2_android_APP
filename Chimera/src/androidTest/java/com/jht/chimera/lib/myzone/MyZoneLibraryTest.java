package com.jht.chimera.lib.myzone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jht.androidcommonalgorithms.events.JHTDataNotification;
import com.jht.chimera.lib.integrations.myzone.MyZoneLib;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.MainApp;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MyZoneLibraryTest {

    static class RequestData{
        final String id;
        final String auth;
        final int beltNumber;
        final long intervalStart;
        final long intervalEnd;

        public RequestData(String id, String authentication, int beltNum, long intervalStart, long intervalEnd){
            this.id = id;
            this.auth = authentication;
            this.beltNumber = beltNum;
            this.intervalStart = intervalStart;
            this.intervalEnd = intervalEnd;
        }
    }

    @Test
    @Emulator
    @Chimera
    @MainApp
    public void testRequestDisplayName(){
        String[] scenarios = {"success","error"};
        for(String scenario : scenarios) {
            try (MyZoneTestFixture testFixture = new MyZoneTestFixture()) {

                JHTDataNotification<RequestData> notification = new JHTDataNotification<>();
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent != null && intent.getAction().equalsIgnoreCase(MyZoneLib.REQUEST_DISPLAY_NAME_TEST)) {
                            notification.setData(new RequestData(
                                    intent.getStringExtra("id"),
                                    intent.getStringExtra("auth"),
                                    intent.getIntExtra("belt_number", 0),
                                    0, //no need to verify in this scenario
                                    0 //no need to verify in this scenario
                            ));
                        }
                    }
                };

                testFixture.registerReceiver(receiver, new IntentFilter(MyZoneLib.REQUEST_DISPLAY_NAME_TEST));
                new Thread(() -> {
                    MyZoneLib myZoneLib = new MyZoneLib(testFixture.getTargetContext(), null, null);
                    myZoneLib.requestDisplayName("jht123", "fake_auth_example", 123456, new MyZoneLib.IRequestDisplayNameResultListener() {
                        @Override
                        public void onResult(String id, int beltNumber, String displayName, String profileImage, String rank) {
                            Assert.assertNotNull(displayName);
                            Assert.assertEquals("jht123",id);
                            Assert.assertEquals(123456,beltNumber);
                            Assert.assertEquals("test_display_name", displayName);
                            Assert.assertEquals("https://cdn.shopify.com/s/files/1/1288/9651/t/3/assets/logo.png?v=12354323415741380892", profileImage);
                            Assert.assertEquals("hall_of_fame", rank);

                        }

                        @Override
                        public void onError(String id, String action, String reason) {
                            Assert.assertEquals("jht123", id);
                            Assert.assertEquals(MyZoneLib.REQUEST_DISPLAY_NAME_TEST, action);
                            Assert.assertEquals("testing display name error", reason);
                        }
                    });
                }).start();
                RequestData data = notification.waitForData(2000);
                Assert.assertNotNull(data);
                Assert.assertEquals("jht123", data.id);
                Assert.assertEquals("fake_auth_example", data.auth);
                Assert.assertEquals(123456, data.beltNumber);

                Intent result = new Intent();
                if(scenario.equalsIgnoreCase("success")) {
                    result.setAction(MyZoneLib.RESULT_DISPLAY_NAME_TEST);
                    result.putExtra("id", data.id);
                    result.putExtra("belt_number", data.beltNumber);
                    result.putExtra("display_name", "test_display_name");
                    result.putExtra("profile_image", "https://cdn.shopify.com/s/files/1/1288/9651/t/3/assets/logo.png?v=12354323415741380892");
                    result.putExtra("rank", "hall_of_fame");
                }else {
                    result.setAction(MyZoneLib.ERROR_TEST);
                    result.putExtra("id", data.id);
                    result.putExtra("action", MyZoneLib.REQUEST_DISPLAY_NAME_TEST);
                    result.putExtra("reason", "testing display name error");
                }
                testFixture.getTestContext().sendBroadcast(result);
            }
        }

    }

    @Test
    @Emulator
    @Chimera
    @MainApp
    public void testRequestConnection(){
        String[] scenarios = {"success","error"};
        for(String scenario : scenarios) {
            try (MyZoneTestFixture testFixture = new MyZoneTestFixture()) {
                JHTDataNotification<RequestData> notification = new JHTDataNotification<>();
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent != null && intent.getAction().equalsIgnoreCase(MyZoneLib.REQUEST_CONNECTION_TEST)) {
                            notification.setData(new RequestData(
                                    intent.getStringExtra("id"),
                                    "irrelevant", //no need to verify in this scenario
                                    intent.getIntExtra("belt_number", 0),
                                    0, //no need to verify in this scenario
                                    0 //no need to verify in this scenario
                            ));
                        }
                    }
                };
                testFixture.registerReceiver(receiver, new IntentFilter(MyZoneLib.REQUEST_CONNECTION_TEST));
                new Thread(() -> {
                    MyZoneLib myZoneLib = new MyZoneLib(testFixture.getTargetContext(), null, null);
                    myZoneLib.requestConnection("jht123", 123456, new MyZoneLib.IRequestConnectionResultListener() {
                        @Override
                        public void onResult(String id, int beltNumber) {
                            Assert.assertNotNull(id);
                            Assert.assertEquals("jht123", id);
                            Assert.assertEquals(123456, beltNumber);
                        }

                        @Override
                        public void onError(String id, String action, String reason) {
                            Assert.assertEquals("jht123", id);
                            Assert.assertEquals(MyZoneLib.REQUEST_CONNECTION_TEST, action);
                            Assert.assertEquals("testing request connection error", reason);
                        }
                    });
                }).start();


                RequestData data = notification.waitForData(2000);
                Assert.assertNotNull(data);
                Assert.assertEquals("jht123", data.id);
                Assert.assertEquals(123456, data.beltNumber);
                Intent result = new Intent();
                if(scenario.equalsIgnoreCase("success")) {
                    result.setAction(MyZoneLib.CONNECTED_TEST);
                    result.putExtra("id", data.id);
                    result.putExtra("belt_number", data.beltNumber);
                }else{
                    result.setAction(MyZoneLib.ERROR_TEST);
                    result.putExtra("id", data.id);
                    result.putExtra("action", MyZoneLib.REQUEST_CONNECTION_TEST);
                    result.putExtra("reason", "testing request connection error");
                }
                testFixture.getTestContext().sendBroadcast(result);
            }
        }
    }

    @Test
    @Emulator
    @Chimera
    @MainApp
    public void testFinishSession(){
        String[] scenarios = {"success","error"};
        for(String scenario : scenarios) {
            try (MyZoneTestFixture testFixture = new MyZoneTestFixture()) {
                JHTDataNotification<RequestData> notification = new JHTDataNotification<>();

                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent != null && intent.getAction().equalsIgnoreCase(MyZoneLib.REQUEST_FINISH_SESSION_TEST)) {
                            notification.setData(new RequestData(
                                    intent.getStringExtra("id"),
                                    "ignore ", //no need to verify in this scenario
                                    0, //no need to verify in this scenario
                                    0, //no need to verify in this scenario
                                    0 //no need to verify in this scenario
                            ));
                        }
                    }
                };
                testFixture.registerReceiver(receiver, new IntentFilter(MyZoneLib.REQUEST_FINISH_SESSION_TEST));
                new Thread(() -> {
                    MyZoneLib myZoneLib = new MyZoneLib(testFixture.getTargetContext(), null, null);
                    myZoneLib.finishSession("jht123", new MyZoneLib.IFinishSessionResultListener() {
                        @Override
                        public void onResult(String id) {
                            Assert.assertEquals("jht123", id);
                        }

                        @Override
                        public void onError(String id, String action, String reason) {
                            Assert.assertEquals("jht123", id);
                            Assert.assertEquals(MyZoneLib.REQUEST_FINISH_SESSION_TEST, action);
                            Assert.assertEquals("testing session finished", reason);
                        }
                    });
                }).start();
                RequestData data = notification.waitForData(2000);
                Assert.assertNotNull(data);
                Assert.assertEquals("jht123", data.id);
                Intent result = new Intent();
                if(scenario.equalsIgnoreCase("success")) {
                    result.setAction(MyZoneLib.RESULT_SESSION_FINSIHED_TEST);
                    result.putExtra("id", data.id);
                }else {
                    result.setAction(MyZoneLib.ERROR_TEST);
                    result.putExtra("id", data.id);
                    result.putExtra("action", MyZoneLib.REQUEST_FINISH_SESSION_TEST);
                    result.putExtra("reason", "testing session finished");
                }

                testFixture.getTestContext().sendBroadcast(result);
            }
        }
    }

    @Test
    @Emulator
    @Chimera
    @MainApp
    public void testRequestTimeInZones(){
        String[] scenarios = {"success","error"};
        for(String scenario : scenarios) {
            try (MyZoneTestFixture testFixture = new MyZoneTestFixture()) {
                JHTDataNotification<RequestData> notification = new JHTDataNotification<>();

                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent != null && intent.getAction().equalsIgnoreCase(MyZoneLib.REQUEST_TIME_IN_ZONES_TEST)) {
                            notification.setData(new RequestData(
                                    intent.getStringExtra("id"),
                                    "irrelevant", //no need to verify in this scenario
                                    intent.getIntExtra("belt_number", 0),
                                    intent.getLongExtra("interval_start", -1),
                                    intent.getLongExtra("interval_end", -1)

                            ));
                        }
                    }
                };

                testFixture.registerReceiver(receiver, new IntentFilter(MyZoneLib.REQUEST_TIME_IN_ZONES_TEST));
                new Thread(() -> {
                    MyZoneLib myZoneLib = new MyZoneLib(testFixture.getTargetContext(), null, null);
                    long intervalStart = 1621478538;
                    long intervalEnd = 1621482138;
                    myZoneLib.requestTimeInZones("jht123", 1234567, intervalStart, intervalEnd, new MyZoneLib.IRequestTimeInZonesResultListener() {
                        @Override
                        public void onResult(String id, int beltNumber, int[] timeInZones, int[] mepsInZones) {
                            Assert.assertEquals("jht123", id);
                            Assert.assertEquals(1234567, beltNumber);
                            Assert.assertArrayEquals(new int[]{11, 22, 33, 44, 55, 66}, timeInZones);
                            Assert.assertArrayEquals(new int[]{12, 34, 56, 78, 90, 5}, mepsInZones);
                        }

                        @Override
                        public void onError(String id, String action, String reason) {
                            Assert.assertEquals("jht123", id);
                            Assert.assertEquals(MyZoneLib.REQUEST_TIME_IN_ZONES_TEST, action);
                            Assert.assertEquals("testing request time zone error", reason);

                        }
                    });

                }).start();

                RequestData data = notification.waitForData(2000);
                Assert.assertNotNull(data);
                Assert.assertEquals("jht123", data.id);
                Assert.assertEquals(1234567, data.beltNumber);
                Assert.assertEquals(1621478538, data.intervalStart);
                Assert.assertEquals(1621482138, data.intervalEnd);

                Intent result = new Intent();
                result.putExtra("id", data.id);
                if(scenario.equalsIgnoreCase("success")) {
                    result.setAction(MyZoneLib.RESULT_TIME_IN_ZONES_TEST);
                    result.putExtra("belt_number", data.beltNumber);

                    int[] timeInZonesArray = new int[]{11, 22, 33, 44, 55, 66};
                    result.putExtra("time_in_zones", timeInZonesArray);

                    int[] mepsInZonesArray = new int[]{12, 34, 56, 78, 90, 5};
                    result.putExtra("meps_in_zones", mepsInZonesArray);
                } else {
                    result.setAction(MyZoneLib.ERROR_TEST);
                    result.putExtra("action", MyZoneLib.REQUEST_TIME_IN_ZONES_TEST);
                    result.putExtra("reason", "testing request time zone error");
                }
                testFixture.getTestContext().sendBroadcast(result);
            }
        }
    }

    @Test
    @Emulator
    @Chimera
    @MainApp
    public void testRequestUpload(){
        String[] scenarios = {"success","error"};
        for(String scenario : scenarios) {
            try (MyZoneTestFixture testFixture = new MyZoneTestFixture()) {
                JHTDataNotification<RequestData> notification = new JHTDataNotification<>();

                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent != null && intent.getAction().equalsIgnoreCase(MyZoneLib.REQUEST_UPLOAD_TEST)) {
                            notification.setData(new RequestData(
                                    intent.getStringExtra("id"),
                                    "ignore ", //no need to verify in this scenario
                                    0, //no need to verify in this scenario
                                    0, //no need to verify in this scenario
                                    0 //no need to verify in this scenario
                            ));
                        }
                    }
                };
                testFixture.registerReceiver(receiver, new IntentFilter(MyZoneLib.REQUEST_UPLOAD_TEST));
                new Thread(() -> {
                    MyZoneLib myZoneLib = new MyZoneLib(testFixture.getTargetContext(), null, null);
                    myZoneLib.requestUpload("jht123", new MyZoneLib.IRequestUploadResultListener() {
                        @Override
                        public void onResult(String id) {
                            Assert.assertEquals("jht123", id);
                        }

                        @Override
                        public void onError(String id, String action, String reason) {
                            Assert.assertEquals("jht123", id);
                            Assert.assertEquals(MyZoneLib.REQUEST_UPLOAD_TEST, action);
                            Assert.assertEquals("testing request upload error", reason);
                        }
                    });
                }).start();
                RequestData data = notification.waitForData(2000);
                Assert.assertNotNull(data);
                Assert.assertEquals("jht123", data.id);

                Intent result = new Intent();
                result.putExtra("id", data.id);

                if(scenario.equalsIgnoreCase("success")) {
                    result.setAction(MyZoneLib.RESULT_UPLOAD_COMPLETE_TEST);
                }else {
                    result.setAction(MyZoneLib.ERROR_TEST);
                    result.putExtra("action", MyZoneLib.REQUEST_UPLOAD_TEST);
                    result.putExtra("reason", "testing request upload error");
                }

                testFixture.getTestContext().sendBroadcast(result);
            }
        }
    }

    @Test
    @Emulator
    @Chimera
    @MainApp
    public void testWorkoutDataUpdate(){
        try(MyZoneTestFixture testFixture = new MyZoneTestFixture()){
            new MyZoneLib(testFixture.getTargetContext(), (kCal, meps, heartRate, percentEffort) -> {
                Assert.assertEquals(22, kCal);
                Assert.assertEquals(6, meps);
                Assert.assertEquals(97, heartRate);
                Assert.assertEquals(67, percentEffort);
            }, null);

            new Thread(()->{
                Intent intent = new Intent(MyZoneLib.WORKOUT_UPDATE_TEST);
                intent.putExtra("kcal",22);
                intent.putExtra("meps",6);
                intent.putExtra("heart_rate",97);
                intent.putExtra("percent_effort", 67);
                testFixture.getTargetContext().sendBroadcast(intent);
            }).start();
        }
    }

    @Test
    @Emulator
    @Chimera
    @MainApp
    public void testDisconnection(){
        try(MyZoneTestFixture testFixture = new MyZoneTestFixture()){
            new MyZoneLib(testFixture.getTargetContext(), null, beltNumber -> Assert.assertEquals(1234567, beltNumber));

            new Thread(()->{
                Intent intent = new Intent(MyZoneLib.DISCONNECTED_TEST);
                intent.putExtra("belt_number",1234567);
                testFixture.getTargetContext().sendBroadcast(intent);
            }).start();
        }

    }


}
