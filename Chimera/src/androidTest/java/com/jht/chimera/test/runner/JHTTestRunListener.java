package com.jht.chimera.test.runner;

import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Create a test run listener.  This listens for test events and creates a test report.  This should
 * be overridden to set the test suite name.
 */
@SuppressWarnings("unused")
public class JHTTestRunListener extends RunListener {

    private final com.jht.chimera.test.runner.TestSuite testSuite = new com.jht.chimera.test.runner.TestSuite();

    /**
     * Called before any tests have been run. This may be called on an
     * arbitrary thread.
     *
     * @param description describes the tests to be run
     */
    public void testRunStarted(Description description) {
        testSuite.startTestSuite();
    }

    /**
     * Called when all tests have finished. This may be called on an
     * arbitrary thread.
     *
     * @param result the summary of the test run, including all the tests that failed
     */
    @Override
    public void testRunFinished(Result result) {
        File testResults = new File(
            InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(), resultFileName("xml"));

        try(FileOutputStream fout = new FileOutputStream(testResults)) {
            fout.write(testSuite.results().getBytes(StandardCharsets.UTF_8));
            fout.flush();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        testResults = new File(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(), resultFileName("json"));

        try(FileOutputStream fout = new FileOutputStream(testResults)) {
            fout.write(testSuite.resultsJSON().getBytes(StandardCharsets.UTF_8));
            fout.flush();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        Log.d("bdc_test", "Wrote test results to file !!!!!!!!!!!!!!!!!!!!!!!");
    }

    /**
     * Called when an atomic test is about to be started.
     *
     * @param description the description of the test that is about to be run
     * (generally a class and method name)
     */
    @Override
    public void testStarted(Description description) {
        testSuite.startTest(description);
    }

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     *
     * @param description the description of the test that just ran
     */
    @Override
    public void testFinished(Description description) {
        testSuite.finishTest(description);
    }

    /**
     * Called when an atomic test fails, or when a listener throws an exception.
     *
     * <p>In the case of a failure of an atomic test, this method will be called
     * with the same {@code Description} passed to
     * {@link #testStarted(Description)}, from the same thread that called
     * {@link #testStarted(Description)}.
     *
     * <p>In the case of a listener throwing an exception, this will be called with
     * a {@code Description} of {@link Description#TEST_MECHANISM}, and may be called
     * on an arbitrary thread.
     *
     * @param failure describes the test that failed and the exception that was thrown
     */
    @Override
    public void testFailure(Failure failure) {
        testSuite.testFailed(failure);
    }

    /**
     * Called when an atomic test flags that it assumes a condition that is
     * false
     *
     * @param failure describes the test that failed and the
     * {@link org.junit.AssumptionViolatedException} that was thrown
     */
    @Override
    public void testAssumptionFailure(Failure failure) {
        testSuite.testError(failure);
    }

    /**
     * Called when a test will not be run, generally because a test method is annotated
     * with {@link org.junit.Ignore}.
     *
     * @param description describes the test that will not be run
     */
    @Override
    public void testIgnored(Description description) {
        testSuite.testSkipped(description);
    }

    private String getFailureMessage(Failure failure, String indent) {
        return indent + "Header: " + failure.getTestHeader() +
                indent + "Message: " + failure.getMessage() +
                indent + "Trace: " + failure.getTrace() +
                indent + "Description: " + failure.getDescription();
    }

    /**
     * @return  The file for the test results.
     */
    protected String resultFileName(String extension) {
        return testSuite.testSuiteName().replace(" ", "_") + "_Results." + extension;
    }
}
