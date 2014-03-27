package org.broadinstitute.gpinformatics.infrastructure.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

/**
 * Log status of TestNG tests.
 */
public class TestListener extends TestListenerAdapter {
    private static final Log logger = LogFactory.getLog(TestListener.class);

    @Override
    public void onTestStart(ITestResult testResult) {
        log("[TEST STARTED] => " + name(testResult));
        super.onTestStart(testResult);
    }

    @Override
    public void onTestFailure(ITestResult testResult) {
        log("[TEST FAILED] => " + name(testResult));
    }

    @Override
    public void onTestSkipped(ITestResult testResult) {
        log("[TEST SKIPPED] => " + name(testResult));
    }

    @Override
    public void onTestSuccess(ITestResult testResult) {
        long timeInMillis = testResult.getEndMillis() - testResult.getStartMillis();
        log(String.format("[TEST SUCCEEDED] (%s) => %s", readableTime(timeInMillis), name(testResult)));
    }

    /**
     * Use the Apache Log4J logger, but might want to replace with TestNG Reporter later.
     *
     * @param logMessage the log message
     */
    protected void log(String logMessage) {
        logger.info(logMessage);
    }

    /**
     * Get the name of the test.
     *
     * @param testResult the test result
     *
     * @return the name of the test
     */
    private static String name(ITestResult testResult) {
        String className = testResult.getTestClass().getName();
        return className.substring(className.lastIndexOf(".") + 1) + "."
               + testResult.getMethod().getMethodName();
    }

    /**
     * Take a long date time and change it into a human readable and appropriate time.
     * This is good for displaying milliseconds or seconds or minutes without doing the
     * math by hand.
     *
     * @param time the long time
     *
     * @return String representing the time in human readable form
     */
    private String readableTime(long time) {
        StringBuilder result = new StringBuilder();
        if (time > 90000) {
            long t = time / 60000;
            result.append(t);
            result.append(" minutes");
            t = (time / 1000) - (t * 60);
            result.append(" and ").append(t).append(" seconds");
            return result.toString();
        }

        if (time > 3000) {
            result.append(time / 1000);
            result.append(" seconds");
            return result.toString();
        }
        result.append(time);
        result.append(" ms");

        return result.toString();
    }
}