package com.jht.chimera.test.runner;

import com.jht.chimera.lib.common.SystemPropertyUtil;
import com.jht.chimera.lib.common.config.ProductConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The test suite for the test report.  Used to generate and manage the overall test report.
 */
public class TestSuite {
    private final HashMap<String, TestCase> testMap = new HashMap<>();  // Map to all the tests.
    private int failures = 0;   // Number of tests that failed.
    private int errors = 0;     // Number of tests that had errors.
    private int skipped = 0;    // Number of tests that were skipped.
    private long executionTime; // Total execution time.
    private Date startTime;     // Start time of the test.

    /**
     * Create the test suite
     */
    TestSuite() {
    }

    /**
     * Start the test suite.
     */
    void startTestSuite() {
        startTime = new Date();
    }

    /**
     * Start an individual test.
     * @param description   Test info.
     */
    void startTest(Description description) {
        testMap.put(description.getDisplayName(), new TestCase(description.getMethodName(), description.getClassName()));
    }

    /**
     * Finish the test.
     * @param description   Test info used to lookup the test.
     */
    void finishTest(Description description) {
        TestCase test = testMap.get(description.getDisplayName());
        if(test != null) {
            executionTime += test.finish();
        }
    }

    /**
     * Test failed
     * @param failure   Failure info.
     */
    void testFailed(Failure failure) {
        TestCase test = testMap.get(failure.getTestHeader());
        if(test != null) {
            executionTime += test.failed(failure);
            failures++;
        }
    }

    /**
     * Test skipped
     * @param description   Test info used to lookup the test.
     */
    void testSkipped(Description description) {
        TestCase test = testMap.get(description.getDisplayName());
        if(test != null) {
            executionTime += test.skip();
            skipped++;
        }

    }

    /**
     * Test skipped
     * @param failure   Failure info.
     */
    void testError(Failure failure) {
        TestCase test = testMap.get(failure.getTestHeader());
        if(test != null) {
            executionTime += test.failed(failure);
            errors++;
        }
    }

    String resultsJSON() throws JSONException {
        JSONObject testSuite = new JSONObject();
        testSuite.put("name", testSuiteName());
        testSuite.put("errors", errors);
        testSuite.put("failures", failures);
        testSuite.put("skipped", skipped);
        testSuite.put("time", executionTime);
        testSuite.put("timestamp", getTimeStamp());
        JSONArray testCases = new JSONArray();
        for(TestCase testCase : testMap.values()) {
            testCases.put(testCase.resultsJSON());
        }
        testSuite.put("testCases", testCases);
        return testSuite.toString(4);
    }

    String results() {
        StringBuilder testSuiteXml = new StringBuilder();
        testSuiteXml.append("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>");
        testSuiteXml.append("<testsuite").append(addTestSuiteAttributes()).append(">\n").append(addProperties());
        for(TestCase testCase : testMap.values()) {
            testSuiteXml.append(testCase.results());
        }
        testSuiteXml.append("</testsuite>");
        return testSuiteXml.toString();
    }

    String testSuiteName() {
        return SystemPropertyUtil.get("jht_test_suite_name");
    }

    private String addTestSuiteAttributes() {

        return " name=\"" + testSuiteName() + "\"" +
                " failures=\"" + failures + "\"" +
                " errors=\"" + errors + "\"" +
                " skipped=\"" + skipped + "\"" +
                " time=\"" + String.format(Locale.ENGLISH, "%f", executionTime / 1000.0) + "\"" +
                " timestamp=\"" + getTimeStamp() + "\"" +
                " hostname=\"localhost\"";
    }

    private String getTimeStamp() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(startTime);
    }

    private String addProperties() {
        return "\t<properties>\n" +
                addProperty("device", SystemPropertyUtil.get("ro.product.model")) +
                addProperty("flavor", ProductConfig.flavor.name()) +
                addProperty("project", ProductConfig.product.name()) +
                "\t</properties>\n";
    }

    private String addProperty(String name, String value) {
        return "\t\t<property name=\"" + name + "\" value=\"" + value + "\" />\n";
    }



}
