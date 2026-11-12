package com.jht.chimera.test.util;

import android.util.Log;

import androidx.annotation.NonNull;

import org.junit.Assert;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class LogMonitor implements Runnable {
    private static final String TAG = "LogMonitor";
    private final List<String> expectedEvents = new ArrayList<>();
    private final List<CrashData> crashes = new ArrayList<>();
    private volatile boolean stopMonitor = false;
    private Calendar monitorStartTime;
    public LogMonitor() {
        new Thread(this).start();
    }

    public LogMonitor(String[] expectedEvents) {
        this.expectedEvents.addAll(Arrays.asList(expectedEvents));
        new Thread(this).start();
    }

    public void stop() {
        stopMonitor = true;
        Log.d(TAG, "Stop monitor");
    }

    @Override
    public void run() {
        Process suProcess;
        try {
            suProcess = Runtime.getRuntime().exec("su 548");
            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            BufferedReader osRes = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));

            os.writeBytes("logcat -v time\n");
            os.flush();

            monitorStartTime = Calendar.getInstance();
            String line;
            while((line = osRes.readLine()) != null ) {
                if(stopMonitor) {
                    return;
                }
                parseLine(osRes, line);
            }
        } catch (Exception ignored) {
        }
    }


    private void parseLine(BufferedReader osRes, String line) throws IOException {

        if(parseTime(line).after(monitorStartTime)) {
            if (line.contains("ANR in")) {
                crashes.add(new CrashData(CrashData.CRASH_TYPE.ANR));
            } else if (line.contains("FATAL EXCEPTION")) {
                crashes.add(new CrashData(CrashData.CRASH_TYPE.FATAL_EXCEPTION, osRes.readLine()));
            } else {
                List<String> eventsFound = new ArrayList<>();
                for (String event : expectedEvents) {
                    if (line.contains(event)) {
                        eventsFound.add(event);
                    }
                }
                for (String event : eventsFound) {
                    expectedEvents.remove(event);
                }
            }
        }
    }

    private @NonNull
    Calendar parseTime(String line) {
        try {
            Calendar time = Calendar.getInstance();
            String[] words = line.split(" ");
            String[] dateParts = words[0].split("-");
            String[] timeParts = words[1].split("\\.");
            String[] timeParts2 = timeParts[0].split(":");
            time.set(Calendar.MONTH, Integer.parseInt(dateParts[0]) - 1);
            time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[1]));
            time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts2[0]));
            time.set(Calendar.MINUTE, Integer.parseInt(timeParts2[1]));
            time.set(Calendar.SECOND, Integer.parseInt(timeParts2[2]));
            time.set(Calendar.MILLISECOND, Integer.parseInt(timeParts[1]));
            return time;
        }
        catch(Exception ignored) {
            return Calendar.getInstance();
        }


    }

    public void assertLogsAreClean() {
        assertNoCrashesFound();
        assertNoMissingEvents();
    }

    private void assertNoCrashesFound() {
        if(crashes.size() > 0) {
            Assert.fail(getCrashFailureMessage());
        }
    }

    @NonNull
    private String getCrashFailureMessage() {
        StringBuilder failureMessage = new StringBuilder();
        failureMessage.append("Found crashes in test");
        for(CrashData crashData : crashes) {
            failureMessage.append("\r\n\t").append(getCrashMessage(crashData));
        }
        return failureMessage.toString();
    }

    private String getCrashMessage(CrashData crashData) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        switch (crashData.crashType()) {
            case FATAL_EXCEPTION:
                return crashData.app() + " crashes at " + dateFormat.format(crashData.crashTime());
            case ANR:
                return "ANR found at " + dateFormat.format(crashData.crashTime());
        }
        return "";
    }

    private void assertNoMissingEvents() {
        if(expectedEvents.size() != 0) {
            Assert.fail(getMissingEventsFailureMessage());
        }
    }

    @NonNull
    private String getMissingEventsFailureMessage() {
        StringBuilder failureMessage = new StringBuilder();
        failureMessage.append("Did not find the following events\n");
        for (String event : expectedEvents) {
            failureMessage.append("\t").append(event).append("\n");
        }
        return failureMessage.toString();
    }
}
