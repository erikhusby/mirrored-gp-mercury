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
    public void onTestStart(ITestResult result) {
        super.onTestStart(result);
        logger.info("Starting: " + result.getTestClass().getName() + "#" + result.getName());
    }
}
