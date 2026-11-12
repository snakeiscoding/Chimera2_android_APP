package com.jht.chimera.test.runner;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.runner.notification.Failure;

import java.util.Locale;

/**
 * Creates a test case object for the test report.
 */
public class TestCase {
    private final String name;       // Test name.  This i generally the test function.
    private final String className;  // Class name for the test.
    private Failure failure = null;  // Failure object if the test fails.
    private long executionTime;      // Time it took to execute the test.
    private final long startTime;    // Start time for the test.

    /**
     * Create a test case.
     * @param name          The test function name.
     * @param className     The test function class.
     */
    TestCase(String name, String className) {
        this.name = name;
        this.className = className;
        startTime = System.currentTimeMillis();
    }

    /**
     * Test has finished.  Compute the execution time.
     * @return          Execution time for the test.
     */
    long finish() {
        executionTime = System.currentTimeMillis() - startTime;
        return executionTime;
    }

    /**
     * Test has failed.  Grab the failure.
     * @param failure   The failure data.
     * @return          Execution time for the test.
     */
    long failed(Failure failure) {
        this.failure = failure;
        return finish();
    }

    /**
     * The test was skipped.
     */
    long skip() {
        return finish();
    }

    JSONObject resultsJSON() throws JSONException {
        JSONObject testCase = new JSONObject();
        testCase.put("name", name);
        testCase.put("classname", className);
        testCase.put("time", executionTime);
        if(failure != null) {
            JSONObject failureObj = new JSONObject();
            failureObj.put("message", failure.getMessage());
            failureObj.put("trace", failure.getTrace());
            failureObj.put("exception", failure.getException().getClass().getCanonicalName());
            testCase.put("failure", failureObj);
        }
        return testCase;
    }

    /**
     * @return The test case results in XML format.
     */
    String results() {
        StringBuilder testCaseResults = new StringBuilder();
        testCaseResults.append("\t<testcase name=\"").append(name).
                append("\" classname=\"").append(className).
                append("\" time=\"").append(String.format(Locale.ENGLISH, "%f", executionTime / 1000.0)).append("\"");
        if(failure != null) {
            testCaseResults.append(">\n").append(writeFailure()).append("\t</testcase>\n");
        }
        else {
            testCaseResults.append(" />\n");
        }
        return testCaseResults.toString();
    }

    /**
     * @return The failure in XML format.
     */
    private String writeFailure() {
        StringBuilder failureMessage = new StringBuilder();
        String message = failure.getMessage();
        if(message == null) { message = ""; }
        failureMessage.append("\t\t<failure message=\"").append(message.replace("<", "&lt;").replace(">", "&gt;")).
            append("\" type=\"").append(failure.getException().getClass().getCanonicalName()).append("\">\n");

        String failureText = failure.getException().getClass().getCanonicalName() + ": " + failure.getMessage() + "\n" + failure.getTrace() + "\n";
        failureMessage.append(
            failureText.
                replace("<", "&lt;").
                replace(">", "&gt;")
        );
        failureMessage.append("\t\t</failure>\n");
        return failureMessage.toString();

    }

}
